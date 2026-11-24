package io.github.rohimahajan6.parabank.tests.hybrid;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Account;
import io.github.rohimahajan6.parabank.api.model.Payee;
import io.github.rohimahajan6.parabank.support.BaseUiTest;
import io.github.rohimahajan6.parabank.support.LiveData;
import io.github.rohimahajan6.parabank.support.Money;
import io.github.rohimahajan6.parabank.ui.pages.BillPayPage;
import io.github.rohimahajan6.parabank.ui.pages.LoginPage;
import java.math.BigDecimal;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Docker only (group "write"): pay a bill in the UI, verify through the API. */
public class BillPayHybridTest extends BaseUiTest {

    private final ApiClient api = new ApiClient();
    private final LiveData data = new LiveData(api);

    @Test(groups = "write", description = "UI bill pay: source drops by exactly the amount and a matching debit appears")
    public void uiBillPayIsReflectedInApi() {
        Account source = data.accountsByBalanceDesc(data.demoCustomer().id()).get(0);
        BigDecimal amount = Money.of("23.45");
        Payee payee = Payee.fake("QA Test Utility Co", "55555");
        BigDecimal before = Money.balance(api, source.id());
        long debitsBefore = data.countTransactions(source.id(), "Debit", amount);
        new LoginPage(driver, config.baseUrl()).open().logIn(config.username(), config.password());

        BillPayPage.Result result = new BillPayPage(driver, config.baseUrl()).open()
                .fill(payee, payee.accountNumber(), payee.accountNumber(), amount.toPlainString(), source.id())
                .send()
                .waitForResult();

        assertThat(result.payeeName()).isEqualTo(payee.name());
        assertThat(result.amount()).isEqualTo(Money.display(amount));
        assertThat(result.fromAccountId()).isEqualTo(String.valueOf(source.id()));

        assertThat(Money.balance(api, source.id())).isEqualByComparingTo(before.subtract(amount));
        assertThat(data.countTransactions(source.id(), "Debit", amount)).isEqualTo(debitsBefore + 1);
    }
}
