package io.github.rohimahajan6.parabank.tests.ui;

import io.github.rohimahajan6.parabank.support.BaseUiTest;
import io.github.rohimahajan6.parabank.ui.pages.LoginPage;
import io.github.rohimahajan6.parabank.ui.pages.OverviewPage;
import java.util.List;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Structure only: the shared public data changes too often to assert exact accounts or balances. */
public class OverviewUiTest extends BaseUiTest {

    private static final String MONEY = "-?\\$[0-9,]+\\.\\d{2}";

    @Test(groups = "smoke", description = "Accounts table has the expected columns and at least one well-formed row")
    public void accountsTableIsPresentAndWellFormed() {
        new LoginPage(driver, config.baseUrl()).open().logIn(config.username(), config.password());

        OverviewPage overview = new OverviewPage(driver).waitForAccounts();

        assertThat(overview.headers()).containsExactly("Account", "Balance*", "Available Amount");
        List<OverviewPage.Row> rows = overview.accountRows();
        assertThat(rows).isNotEmpty();
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.account()).matches("\\d+");
            assertThat(row.balance()).matches(MONEY);
            assertThat(row.available()).matches(MONEY);
        });
    }
}
