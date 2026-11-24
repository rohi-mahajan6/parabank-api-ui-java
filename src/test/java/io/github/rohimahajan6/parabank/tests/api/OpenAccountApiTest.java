package io.github.rohimahajan6.parabank.tests.api;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Account;
import io.github.rohimahajan6.parabank.api.model.AccountType;
import io.github.rohimahajan6.parabank.api.model.Customer;
import io.github.rohimahajan6.parabank.support.Evidence;
import io.github.rohimahajan6.parabank.support.KnownDefect;
import io.github.rohimahajan6.parabank.support.LiveData;
import io.github.rohimahajan6.parabank.support.Money;
import io.restassured.response.Response;
import java.math.BigDecimal;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Docker only (group "write"). DockerGuard blocks these calls with TEST_ENV=public.
 *
 * <p>The createAccount response and the account as fetched afterwards are both recorded, so the
 * report shows whether the opening deposit is missing or only missing from the response body.
 */
public class OpenAccountApiTest {

    private final ApiClient api = new ApiClient();
    private final LiveData data = new LiveData(api);

    @Test(groups = "write", description = "createAccount moves the opening deposit: source drops by exactly the new account's balance (checked via GET)")
    public void createSavingsAccountMovesOpeningDeposit() {
        Customer customer = data.demoCustomer();
        Account source = data.accountsByBalanceDesc(customer.id()).get(0);
        BigDecimal sourceBefore = Money.balance(api, source.id());

        Response response = api.createAccount(customer.id(), AccountType.SAVINGS, source.id());

        assertThat(response.statusCode()).as("createAccount HTTP status").isEqualTo(200);
        Account created = response.as(Account.class);
        Account fetched = api.account(created.id()).as(Account.class);
        BigDecimal sourceAfter = Money.balance(api, source.id());
        BigDecimal sourceDrop = sourceBefore.subtract(sourceAfter);
        Evidence.attach("createAccount evidence", String.join("\n",
                "request: POST /createAccount?customerId=" + customer.id() + "&newAccountType="
                        + AccountType.SAVINGS.code() + "&fromAccountId=" + source.id(),
                "status:  " + response.statusCode(),
                "body:    " + response.asString().replaceAll("\\s+", " "),
                "new account " + created.id() + ": balance in response " + created.balance()
                        + ", balance from GET " + fetched.balance(),
                "source " + source.id() + ": before " + sourceBefore + "  after " + sourceAfter
                        + "  drop " + sourceDrop));

        // What works (observed in CI run 45376749272): must keep passing.
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(created.type()).as("type in response").isEqualTo("SAVINGS");
        softly.assertThat(created.customerId()).as("customerId in response").isEqualTo(customer.id());
        softly.assertThat(fetched.type()).as("type from GET").isEqualTo("SAVINGS");
        softly.assertThat(fetched.balance()).as("opening deposit from GET").isPositive();
        softly.assertThat(sourceDrop).as("source drop equals new account balance from GET (" + fetched.balance() + ")")
                .isEqualByComparingTo(fetched.balance());
        softly.assertThat(data.accounts(customer.id())).extracting(Account::id).as("customer's account list")
                .contains(created.id());
        softly.assertAll();
    }

    @Test(groups = {"write", "known-defect"},
            description = "createAccount response should report the real opening balance (known defect: reports 0)")
    public void createAccountResponseReportsOpeningBalance() {
        Customer customer = data.demoCustomer();
        Account source = data.accountsByBalanceDesc(customer.id()).get(0);

        Response response = api.createAccount(customer.id(), AccountType.SAVINGS, source.id());

        assertThat(response.statusCode()).as("createAccount HTTP status").isEqualTo(200);
        Account created = response.as(Account.class);
        Account fetched = api.account(created.id()).as(Account.class);
        Evidence.attach("createAccount response vs GET", String.join("\n",
                "body:        " + response.asString().replaceAll("\\s+", " "),
                "GET account: " + api.account(created.id()).asString().replaceAll("\\s+", " ")));

        KnownDefect.expect("PB-006", "createAccount response reports balance 0 although the account holds the opening deposit",
                () -> assertThat(created.balance()).as("balance in createAccount response vs GET " + fetched.balance())
                        .isEqualByComparingTo(fetched.balance()));
    }
}
