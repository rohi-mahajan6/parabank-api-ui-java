package io.github.rohimahajan6.parabank.tests.hybrid;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Account;
import io.github.rohimahajan6.parabank.support.BaseUiTest;
import io.github.rohimahajan6.parabank.support.LiveData;
import io.github.rohimahajan6.parabank.support.Money;
import io.github.rohimahajan6.parabank.ui.pages.LoginPage;
import io.github.rohimahajan6.parabank.ui.pages.RequestLoanPage;
import java.math.BigDecimal;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Docker only (group "write"): request a loan in the UI, verify the new account through the API. */
public class LoanHybridTest extends BaseUiTest {

    private final ApiClient api = new ApiClient();
    private final LiveData data = new LiveData(api);

    @Test(groups = "write", description = "UI loan within limits is approved; API shows the LOAN account and the down payment debit")
    public void uiLoanIsApprovedAndVisibleInApi() {
        long customerId = data.demoCustomer().id();
        Account source = data.accountsByBalanceDesc(customerId).get(0);
        BigDecimal downPayment = Money.of("100.00");
        BigDecimal before = Money.balance(api, source.id());
        new LoginPage(driver, config.baseUrl()).open().logIn(config.username(), config.password());

        RequestLoanPage.Result result = new RequestLoanPage(driver, config.baseUrl()).open()
                .apply("500.00", downPayment.toPlainString(), source.id());

        assertThat(result.status()).as("denied reason: " + result.deniedReason()).isEqualTo("Approved");
        assertThat(result.newAccountId()).isNotNull();
        Account loanAccount = api.account(result.newAccountId()).as(Account.class);
        assertThat(loanAccount.type()).isEqualTo("LOAN");
        assertThat(loanAccount.customerId()).isEqualTo(customerId);
        assertThat(Money.balance(api, source.id())).isEqualByComparingTo(before.subtract(downPayment));
    }
}
