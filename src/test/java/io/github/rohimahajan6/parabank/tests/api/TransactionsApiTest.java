package io.github.rohimahajan6.parabank.tests.api;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Transaction;
import io.github.rohimahajan6.parabank.support.LiveData;
import io.github.rohimahajan6.parabank.support.LiveData.AccountWithTransaction;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each filter test takes a real transaction from the shared data, filters by one of its
 * properties, and checks that every returned item has that property and the original is included.
 */
public class TransactionsApiTest {

    private final ApiClient api = new ApiClient();
    private final LiveData data = new LiveData(api);

    @Test(groups = "smoke", description = "Transactions: 200, schema valid, all belong to the account")
    public void accountTransactionsAreReturned() {
        long accountId = data.accountWithTransaction(t -> true, "at least one transaction").account().id();

        Response response = api.transactions(accountId);

        assertThat(response.statusCode()).isEqualTo(200);
        response.then().body(matchesJsonSchemaInClasspath("schemas/transaction-list.json"));
        List<Transaction> transactions = list(response);
        assertThat(transactions).isNotEmpty();
        assertThat(transactions).allSatisfy(t -> assertThat(t.accountId()).isEqualTo(accountId));
    }

    @Test(groups = "smoke", description = "Filter by amount returns only transactions with that amount")
    public void filterByAmount() {
        AccountWithTransaction pick = data.accountWithTransaction(t -> true, "at least one transaction");
        Transaction sample = pick.transaction();

        Response response = api.transactionsByAmount(pick.account().id(), sample.amount());

        assertThat(response.statusCode()).isEqualTo(200);
        List<Transaction> result = list(response);
        assertThat(result).extracting(Transaction::id).contains(sample.id());
        assertThat(result).allSatisfy(t -> assertThat(t.amount()).isEqualByComparingTo(sample.amount()));
    }

    @Test(groups = "smoke", description = "Filter by date range returns only transactions inside the range")
    public void filterByDateRange() {
        AccountWithTransaction pick = data.accountWithTransaction(t -> true, "at least one transaction");
        Transaction sample = pick.transaction();
        LocalDate day = sample.utcDate();

        Response response = api.transactionsByDateRange(pick.account().id(), day, day);

        assertThat(response.statusCode()).isEqualTo(200);
        List<Transaction> result = list(response);
        assertThat(result).extracting(Transaction::id).contains(sample.id());
        assertThat(result).allSatisfy(t -> assertThat(t.utcDate()).isEqualTo(day));
    }

    @Test(groups = "smoke", description = "Filter by month and type returns only matching transactions")
    public void filterByMonthAndType() {
        AccountWithTransaction pick = data.accountWithTransaction(t -> true, "at least one transaction");
        Transaction sample = pick.transaction();
        String month = sample.utcDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        Response response = api.transactionsByMonthAndType(pick.account().id(), month, sample.type());

        assertThat(response.statusCode()).isEqualTo(200);
        List<Transaction> result = list(response);
        assertThat(result).extracting(Transaction::id).contains(sample.id());
        assertThat(result).allSatisfy(t -> {
            assertThat(t.type()).isEqualTo(sample.type());
            assertThat(t.utcDate().getMonth()).isEqualTo(sample.utcDate().getMonth());
        });
    }

    private static List<Transaction> list(Response response) {
        return Arrays.asList(response.as(Transaction[].class));
    }
}
