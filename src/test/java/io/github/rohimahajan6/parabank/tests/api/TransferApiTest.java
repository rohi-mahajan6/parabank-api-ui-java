package io.github.rohimahajan6.parabank.tests.api;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Account;
import io.github.rohimahajan6.parabank.support.Evidence;
import io.github.rohimahajan6.parabank.support.LiveData;
import io.github.rohimahajan6.parabank.support.Money;
import io.restassured.response.Response;
import java.math.BigDecimal;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Docker only (group "write").
 *
 * <p>Negative and overdraft tests assert what a bank should do: reject the request and leave both
 * balances unchanged. ParaBank's real behaviour is not known yet; any test that fails on the first
 * Docker run is reviewed and, if it is a real defect, documented as a finding with evidence.
 */
public class TransferApiTest {

    private static final long NON_EXISTENT_ACCOUNT = 99_999_999L;

    private final ApiClient api = new ApiClient();
    private final LiveData data = new LiveData(api);
    private Account from;
    private Account to;

    @BeforeMethod(alwaysRun = true)
    public void pickAccounts() {
        Account[] pair = data.twoAccounts(data.demoCustomer().id());
        from = pair[0];
        to = pair[1];
    }

    @Test(groups = "write", description = "Transfer moves exactly the amount: source -X, destination +X")
    public void transferMovesExactAmount() {
        BigDecimal amount = Money.of("12.34");
        BigDecimal fromBefore = Money.balance(api, from.id());
        BigDecimal toBefore = Money.balance(api, to.id());

        Response response = api.transfer(from.id(), to.id(), amount);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.asString()).contains("Successfully transferred");
        assertThat(Money.balance(api, from.id())).isEqualByComparingTo(fromBefore.subtract(amount));
        assertThat(Money.balance(api, to.id())).isEqualByComparingTo(toBefore.add(amount));
    }

    @DataProvider(name = "invalidAmounts")
    public Object[][] invalidAmounts() {
        return new Object[][] {{"0"}, {"0.00"}, {"-5.00"}, {"abc"}};
    }

    @Test(groups = "write", dataProvider = "invalidAmounts",
            description = "Invalid amount is rejected and no money moves")
    public void invalidAmountIsRejected(String amount) {
        assertRejectedWithoutMovement(from.id(), to.id(), amount);
    }

    @Test(groups = "write", description = "Transfer to the same account is rejected and no money moves")
    public void transferToSameAccountIsRejected() {
        assertRejectedWithoutMovement(from.id(), from.id(), "1.00");
    }

    @Test(groups = "write", description = "Transfer to a non-existent account is rejected and no money moves")
    public void transferToNonExistentAccountIsRejected() {
        BigDecimal fromBefore = Money.balance(api, from.id());

        Response response = api.transfer(from.id(), NON_EXISTENT_ACCOUNT, "1.00");

        assertThat(response.statusCode()).as("HTTP status").isBetween(400, 499);
        assertThat(Money.balance(api, from.id())).isEqualByComparingTo(fromBefore);
    }

    @Test(groups = "write", description = "Overdraft: transfer larger than the balance is rejected (finding candidate)")
    public void overdraftIsRejected() {
        BigDecimal fromBefore = Money.balance(api, from.id());
        BigDecimal tooMuch = fromBefore.max(BigDecimal.ZERO).add(Money.of("1000.00"));

        assertRejectedWithoutMovement(from.id(), to.id(), tooMuch.toPlainString());
    }

    /**
     * Records the full picture before asserting: HTTP status, response body and both balances
     * before and after. The evidence is attached to the Allure report, and the checks are soft
     * assertions, so a failing status check no longer hides whether money actually moved.
     */
    private void assertRejectedWithoutMovement(long fromId, long toId, String amount) {
        BigDecimal fromBefore = Money.balance(api, fromId);
        BigDecimal toBefore = Money.balance(api, toId);

        Response response = api.transfer(fromId, toId, amount);

        BigDecimal fromAfter = Money.balance(api, fromId);
        BigDecimal toAfter = Money.balance(api, toId);
        Evidence.attach("Transfer evidence", String.join("\n",
                "request: POST /transfer?fromAccountId=" + fromId + "&toAccountId=" + toId + "&amount=" + amount,
                "status:  " + response.statusCode(),
                "body:    " + response.asString(),
                "source " + fromId + ":      before " + fromBefore + "  after " + fromAfter
                        + "  change " + fromAfter.subtract(fromBefore),
                "destination " + toId + ": before " + toBefore + "  after " + toAfter
                        + "  change " + toAfter.subtract(toBefore)));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(response.statusCode()).as("HTTP status for amount " + amount).isBetween(400, 499);
        softly.assertThat(fromAfter).as("source balance (before " + fromBefore + ")").isEqualByComparingTo(fromBefore);
        softly.assertThat(toAfter).as("destination balance (before " + toBefore + ")").isEqualByComparingTo(toBefore);
        softly.assertAll();
    }
}
