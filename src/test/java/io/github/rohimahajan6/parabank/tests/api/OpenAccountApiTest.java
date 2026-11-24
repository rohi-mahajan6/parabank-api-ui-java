package io.github.rohimahajan6.parabank.tests.api;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Account;
import io.github.rohimahajan6.parabank.api.model.AccountType;
import io.github.rohimahajan6.parabank.api.model.Customer;
import io.github.rohimahajan6.parabank.support.LiveData;
import io.github.rohimahajan6.parabank.support.Money;
import io.restassured.response.Response;
import java.math.BigDecimal;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Docker only (group "write"). DockerGuard blocks these calls with TEST_ENV=public. */
public class OpenAccountApiTest {

    private final ApiClient api = new ApiClient();
    private final LiveData data = new LiveData(api);

    @Test(groups = "write", description = "createAccount moves the opening deposit: source drops by exactly the new account's balance")
    public void createSavingsAccountMovesOpeningDeposit() {
        Customer customer = data.demoCustomer();
        Account source = data.accountsByBalanceDesc(customer.id()).get(0);
        BigDecimal sourceBefore = Money.balance(api, source.id());

        Response response = api.createAccount(customer.id(), AccountType.SAVINGS, source.id());

        assertThat(response.statusCode()).isEqualTo(200);
        Account created = response.as(Account.class);
        assertThat(created.type()).isEqualTo("SAVINGS");
        assertThat(created.customerId()).isEqualTo(customer.id());
        assertThat(created.balance()).as("opening deposit").isPositive();

        BigDecimal sourceAfter = Money.balance(api, source.id());
        assertThat(sourceAfter).isEqualByComparingTo(sourceBefore.subtract(created.balance()));

        Account fetched = api.account(created.id()).as(Account.class);
        assertThat(fetched.type()).isEqualTo("SAVINGS");
        assertThat(fetched.balance()).isEqualByComparingTo(created.balance());
        assertThat(data.accounts(customer.id())).extracting(Account::id).contains(created.id());
    }
}
