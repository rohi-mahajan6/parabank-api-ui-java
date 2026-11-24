package io.github.rohimahajan6.parabank.tests.api;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Account;
import io.github.rohimahajan6.parabank.support.Evidence;
import io.github.rohimahajan6.parabank.support.KnownDefect;
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
 * <p>Rejection tests assert what a bank should do: reject the request (4xx), move no money and
 * record no transactions. Cases confirmed as ParaBank defects on 2026-11-24 (CI run 45376749272)
 * are wrapped in {@link KnownDefect} and carry the "known-defect" group; see README section 7.
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

    @Test(groups = "write", description = "Non-numeric amount is rejected and no money moves")
    public void nonNumericAmountIsRejected() {
        Outcome outcome = attemptTransfer(from.id(), to.id(), "abc");
        outcome.assertRejectedWithoutEffect();
    }

    @Test(groups = "write", description = "Transfer to a non-existent account is rejected and no money moves")
    public void transferToNonExistentAccountIsRejected() {
        BigDecimal fromBefore = Money.balance(api, from.id());

        Response response = api.transfer(from.id(), NON_EXISTENT_ACCOUNT, "1.00");

        assertThat(response.statusCode()).as("HTTP status").isBetween(400, 499);
        assertThat(Money.balance(api, from.id())).isEqualByComparingTo(fromBefore);
    }

    // ---- Known defects (README section 7)

    @DataProvider(name = "zeroAmounts")
    public Object[][] zeroAmounts() {
        return new Object[][] {{"0", "PB-001"}, {"0.00", "PB-002"}};
    }

    @Test(groups = {"write", "known-defect"}, dataProvider = "zeroAmounts",
            description = "Zero amount should be rejected (known defect: accepted with 200)")
    public void zeroAmountIsRejected(String amount, String defectId) {
        Outcome outcome = attemptTransfer(from.id(), to.id(), amount);
        KnownDefect.expect(defectId, "transfer of " + amount + " is accepted with 200 'Successfully transferred'",
                outcome::assertRejectedWithoutEffect);
    }

    @Test(groups = {"write", "known-defect"},
            description = "Negative amount should be rejected (known defect: accepted and money moves backwards)")
    public void negativeAmountIsRejected() {
        Outcome outcome = attemptTransfer(from.id(), to.id(), "-5.00");
        KnownDefect.expect("PB-003", "transfer of -5.00 is accepted and moves 5.00 from the destination to the source",
                outcome::assertRejectedWithoutEffect);
    }

    @Test(groups = {"write", "known-defect"},
            description = "Overdraft should be rejected (known defect: accepted, source goes negative)")
    public void overdraftIsRejected() {
        BigDecimal fromBefore = Money.balance(api, from.id());
        String tooMuch = fromBefore.max(BigDecimal.ZERO).add(Money.of("1000.00")).toPlainString();

        Outcome outcome = attemptTransfer(from.id(), to.id(), tooMuch);
        KnownDefect.expect("PB-004", "transfer larger than the balance is accepted and the source goes negative",
                outcome::assertRejectedWithoutEffect);
    }

    @Test(groups = {"write", "known-defect"},
            description = "Transfer to the same account should be rejected (known defect: accepted with 200)")
    public void transferToSameAccountIsRejected() {
        Outcome outcome = attemptTransfer(from.id(), from.id(), "1.00");
        KnownDefect.expect("PB-005", "transfer from an account to itself is accepted with 200 'Successfully transferred'",
                outcome::assertRejectedWithoutEffect);
    }

    // ---- Evidence

    /**
     * Sends the transfer and records the full picture: status, body, both balances before and after,
     * and (for numeric amounts) how many Debit/Credit transactions of that amount exist before and after.
     */
    private Outcome attemptTransfer(long fromId, long toId, String amount) {
        BigDecimal numeric = parse(amount);
        BigDecimal fromBefore = Money.balance(api, fromId);
        BigDecimal toBefore = Money.balance(api, toId);
        long debitsBefore = numeric == null ? -1 : data.countTransactions(fromId, "Debit", numeric.abs());
        long creditsBefore = numeric == null ? -1 : data.countTransactions(toId, "Credit", numeric.abs());

        Response response = api.transfer(fromId, toId, amount);

        BigDecimal fromAfter = Money.balance(api, fromId);
        BigDecimal toAfter = Money.balance(api, toId);
        long debitsAfter = numeric == null ? -1 : data.countTransactions(fromId, "Debit", numeric.abs());
        long creditsAfter = numeric == null ? -1 : data.countTransactions(toId, "Credit", numeric.abs());

        Outcome outcome = new Outcome(fromId, toId, amount, response.statusCode(), response.asString(),
                fromBefore, fromAfter, toBefore, toAfter, debitsBefore, debitsAfter, creditsBefore, creditsAfter);
        Evidence.attach("Transfer evidence", outcome.describe());
        return outcome;
    }

    private static BigDecimal parse(String amount) {
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    record Outcome(long fromId, long toId, String amount, int status, String body,
                   BigDecimal fromBefore, BigDecimal fromAfter, BigDecimal toBefore, BigDecimal toAfter,
                   long debitsBefore, long debitsAfter, long creditsBefore, long creditsAfter) {

        String describe() {
            String text = String.join("\n",
                    "request: POST /transfer?fromAccountId=" + fromId + "&toAccountId=" + toId + "&amount=" + amount,
                    "status:  " + status,
                    "body:    " + body,
                    "source " + fromId + ":      before " + fromBefore + "  after " + fromAfter
                            + "  change " + fromAfter.subtract(fromBefore),
                    "destination " + toId + ": before " + toBefore + "  after " + toAfter
                            + "  change " + toAfter.subtract(toBefore));
            if (debitsBefore >= 0) {
                text += "\nDebit transactions of " + amount + " on " + fromId + ":  before " + debitsBefore
                        + "  after " + debitsAfter
                        + "\nCredit transactions of " + amount + " on " + toId + ": before " + creditsBefore
                        + "  after " + creditsAfter
                        + "\n(transaction amounts are matched by absolute value)";
            }
            return text;
        }

        /** What a bank should do: reject, move no money, record no transactions. */
        void assertRejectedWithoutEffect() {
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(status).as("HTTP status for amount " + amount).isBetween(400, 499);
            softly.assertThat(fromAfter).as("source balance (before " + fromBefore + ")").isEqualByComparingTo(fromBefore);
            softly.assertThat(toAfter).as("destination balance (before " + toBefore + ")").isEqualByComparingTo(toBefore);
            if (debitsBefore >= 0) {
                softly.assertThat(debitsAfter).as("new Debit transactions on " + fromId).isEqualTo(debitsBefore);
                softly.assertThat(creditsAfter).as("new Credit transactions on " + toId).isEqualTo(creditsBefore);
            }
            softly.assertAll();
        }
    }
}
