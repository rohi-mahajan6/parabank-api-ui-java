package io.github.rohimahajan6.parabank.tests.hybrid;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Account;
import io.github.rohimahajan6.parabank.support.BaseUiTest;
import io.github.rohimahajan6.parabank.support.LiveData;
import io.github.rohimahajan6.parabank.support.Money;
import io.github.rohimahajan6.parabank.ui.pages.LoginPage;
import io.github.rohimahajan6.parabank.ui.pages.TransferPage;
import java.math.BigDecimal;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Docker only (group "write"): transfer in the UI, then check balances and transactions through the API. */
public class TransferHybridTest extends BaseUiTest {

    private final ApiClient api = new ApiClient();
    private final LiveData data = new LiveData(api);

    @Test(groups = "write", description = "UI transfer: API balances move by exactly X and a matching debit/credit pair appears")
    public void uiTransferIsReflectedInApi() {
        Account[] pair = data.twoAccounts(data.demoCustomer().id());
        long from = pair[0].id();
        long to = pair[1].id();
        BigDecimal amount = Money.of("17.29");
        BigDecimal fromBefore = Money.balance(api, from);
        BigDecimal toBefore = Money.balance(api, to);
        long debitsBefore = data.countTransactions(from, "Debit", amount);
        long creditsBefore = data.countTransactions(to, "Credit", amount);
        new LoginPage(driver, config.baseUrl()).open().logIn(config.username(), config.password());

        TransferPage.Result result = new TransferPage(driver, config.baseUrl()).open()
                .transfer(amount.toPlainString(), from, to);

        assertThat(result.completed()).as("Transfer Complete! panel").isTrue();
        assertThat(result.amount()).isEqualTo(Money.display(amount));
        assertThat(result.fromAccountId()).isEqualTo(String.valueOf(from));
        assertThat(result.toAccountId()).isEqualTo(String.valueOf(to));

        assertThat(Money.balance(api, from)).isEqualByComparingTo(fromBefore.subtract(amount));
        assertThat(Money.balance(api, to)).isEqualByComparingTo(toBefore.add(amount));
        assertThat(data.countTransactions(from, "Debit", amount)).isEqualTo(debitsBefore + 1);
        assertThat(data.countTransactions(to, "Credit", amount)).isEqualTo(creditsBefore + 1);
    }
}
