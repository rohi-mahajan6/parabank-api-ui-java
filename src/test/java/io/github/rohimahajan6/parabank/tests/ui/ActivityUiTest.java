package io.github.rohimahajan6.parabank.tests.ui;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Transaction;
import io.github.rohimahajan6.parabank.support.BaseUiTest;
import io.github.rohimahajan6.parabank.support.LiveData;
import io.github.rohimahajan6.parabank.ui.pages.ActivityPage;
import io.github.rohimahajan6.parabank.ui.pages.LoginPage;
import io.restassured.response.Response;
import java.util.List;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Structure only. The account is picked at run time through the API (read-only). */
public class ActivityUiTest extends BaseUiTest {

    private static final String MONEY = "\\$[0-9,]+\\.\\d{2}";

    @Test(groups = "smoke", description = "Filtering activity by Debit shows only debit rows")
    public void debitFilterShowsOnlyDebits() {
        ApiClient api = new ApiClient();
        long accountId = new LiveData(api)
                .accountWithTransaction(t -> "Debit".equals(t.type()), "a debit transaction")
                .account().id();
        new LoginPage(driver, config.baseUrl()).open().logIn(config.username(), config.password());

        ActivityPage activity = new ActivityPage(driver, config.baseUrl()).open(accountId).filter("All", "Debit");
        List<ActivityPage.Row> rows = activity.rows();

        if (rows.isEmpty()) {
            // Shared public data can change between picking the account and opening the page.
            // Ask the API the same question now: if it also has no debits, the data moved under us.
            Response now = api.transactionsByMonthAndType(accountId, "All", "Debit");
            int apiDebits = now.statusCode() == 200 ? now.as(Transaction[].class).length : 0;
            if (apiDebits == 0) {
                throw new SkipException("Account " + accountId + " had a debit when picked, but neither the page "
                        + "nor the API shows one now: shared public data changed during the test.");
            }
            assertThat(rows).as("page shows no debit rows, but the API returns " + apiDebits
                    + " debits for account " + accountId).isNotEmpty();
        }
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.debit()).matches(MONEY);
            assertThat(row.credit()).isEmpty();
        });
    }
}
