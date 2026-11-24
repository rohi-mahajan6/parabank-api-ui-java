package io.github.rohimahajan6.parabank.tests.hybrid;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Account;
import io.github.rohimahajan6.parabank.api.model.AccountType;
import io.github.rohimahajan6.parabank.api.model.Customer;
import io.github.rohimahajan6.parabank.support.BaseUiTest;
import io.github.rohimahajan6.parabank.support.LiveData;
import io.github.rohimahajan6.parabank.support.Money;
import io.github.rohimahajan6.parabank.ui.pages.LoginPage;
import io.github.rohimahajan6.parabank.ui.pages.OpenAccountPage;
import java.math.BigDecimal;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Docker only (group "write"): act in the UI, verify through the API. */
public class OpenAccountHybridTest extends BaseUiTest {

    private final ApiClient api = new ApiClient();
    private final LiveData data = new LiveData(api);

    @Test(groups = "write", description = "Open a SAVINGS account in the UI; the API shows it with the right type and owner")
    public void openSavingsAccountInUiIsVisibleInApi() {
        Customer customer = data.demoCustomer();
        Account source = data.accountsByBalanceDesc(customer.id()).get(0);
        BigDecimal sourceBefore = Money.balance(api, source.id());
        new LoginPage(driver, config.baseUrl()).open().logIn(config.username(), config.password());

        long newAccountId = new OpenAccountPage(driver, config.baseUrl()).open()
                .openAccount(AccountType.SAVINGS, source.id());

        Account created = api.account(newAccountId).as(Account.class);
        assertThat(created.type()).isEqualTo("SAVINGS");
        assertThat(created.customerId()).isEqualTo(customer.id());
        assertThat(Money.balance(api, source.id()))
                .as("source drops by the opening deposit")
                .isEqualByComparingTo(sourceBefore.subtract(created.balance()));
    }
}
