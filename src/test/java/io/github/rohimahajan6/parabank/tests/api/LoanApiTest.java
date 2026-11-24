package io.github.rohimahajan6.parabank.tests.api;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Account;
import io.github.rohimahajan6.parabank.api.model.Customer;
import io.github.rohimahajan6.parabank.api.model.LoanResponse;
import io.github.rohimahajan6.parabank.support.LiveData;
import io.github.rohimahajan6.parabank.support.Money;
import io.restassured.response.Response;
import java.math.BigDecimal;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Docker only (group "write"). Loan approval rules are configured in ParaBank's admin settings;
 * these tests use the defaults the container starts with after POST /initializeDB.
 */
public class LoanApiTest {

    private final ApiClient api = new ApiClient();
    private final LiveData data = new LiveData(api);

    @Test(groups = "write", description = "Loan within limits is approved: LOAN account created, down payment debited exactly")
    public void loanWithinLimitsIsApproved() {
        Customer customer = data.demoCustomer();
        Account source = data.accountsByBalanceDesc(customer.id()).get(0);
        BigDecimal downPayment = Money.of("100.00");
        BigDecimal amount = Money.of("500.00");
        BigDecimal sourceBefore = Money.balance(api, source.id());

        Response response = api.requestLoan(customer.id(), amount, downPayment, source.id());

        assertThat(response.statusCode()).isEqualTo(200);
        LoanResponse loan = response.as(LoanResponse.class);
        assertThat(loan.approved()).as("approved, message=" + loan.message()).isTrue();
        assertThat(loan.accountId()).isNotNull();

        Account loanAccount = api.account(loan.accountId()).as(Account.class);
        assertThat(loanAccount.type()).isEqualTo("LOAN");
        assertThat(loanAccount.customerId()).isEqualTo(customer.id());
        assertThat(Money.balance(api, source.id())).isEqualByComparingTo(sourceBefore.subtract(downPayment));
    }

    @Test(groups = "write", description = "Down payment larger than the balance is denied with a reason; no money moves")
    public void downPaymentLargerThanBalanceIsDenied() {
        Customer customer = data.demoCustomer();
        Account source = data.accountsByBalanceDesc(customer.id()).get(0);
        BigDecimal sourceBefore = Money.balance(api, source.id());
        BigDecimal downPayment = sourceBefore.max(BigDecimal.ZERO).add(Money.of("1000.00"));

        Response response = api.requestLoan(customer.id(), downPayment.multiply(BigDecimal.TEN), downPayment, source.id());

        assertDenied(response);
        assertThat(Money.balance(api, source.id())).isEqualByComparingTo(sourceBefore);
    }

    @Test(groups = "write", description = "Loan far above what the funds support is denied with a reason; no money moves")
    public void loanAboveLimitsIsDenied() {
        Customer customer = data.demoCustomer();
        Account source = data.accountsByBalanceDesc(customer.id()).get(0);
        BigDecimal sourceBefore = Money.balance(api, source.id());

        Response response = api.requestLoan(customer.id(), Money.of("10000000.00"), Money.of("10.00"), source.id());

        assertDenied(response);
        assertThat(Money.balance(api, source.id())).isEqualByComparingTo(sourceBefore);
    }

    private static void assertDenied(Response response) {
        assertThat(response.statusCode()).isEqualTo(200);
        LoanResponse loan = response.as(LoanResponse.class);
        assertThat(loan.approved()).isFalse();
        assertThat(loan.message()).as("denial reason").isIn(LoanResponse.DENIAL_CODES);
    }
}
