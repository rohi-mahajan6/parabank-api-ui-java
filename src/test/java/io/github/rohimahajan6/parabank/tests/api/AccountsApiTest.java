package io.github.rohimahajan6.parabank.tests.api;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Account;
import io.github.rohimahajan6.parabank.api.model.Customer;
import io.github.rohimahajan6.parabank.support.LiveData;
import io.restassured.response.Response;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

public class AccountsApiTest {

    private final ApiClient api = new ApiClient();
    private final LiveData data = new LiveData(api);
    private Customer customer;

    @BeforeClass(alwaysRun = true)
    public void findCustomer() {
        customer = data.demoCustomer();
    }

    @Test(groups = "smoke", description = "Customer accounts: 200, schema valid, non-empty, all owned by the customer")
    public void customerAccountsAreReturned() {
        Response response = api.accounts(customer.id());

        assertThat(response.statusCode()).isEqualTo(200);
        response.then().body(matchesJsonSchemaInClasspath("schemas/account-list.json"));

        List<Account> accounts = Arrays.asList(response.as(Account[].class));
        assertThat(accounts).isNotEmpty();
        assertThat(accounts).allSatisfy(a -> assertThat(a.customerId()).isEqualTo(customer.id()));
    }

    /*
     * Balance is deliberately not compared: the public data is shared and another user's
     * transfer between the two calls could change it. Exact balances are checked on Docker.
     */
    @Test(groups = "smoke", description = "GET /accounts/{id} matches the entry in the customer's account list")
    public void singleAccountMatchesListEntry() {
        Account fromList = data.accounts(customer.id()).get(0);

        Response response = api.account(fromList.id());

        assertThat(response.statusCode()).isEqualTo(200);
        response.then().body(matchesJsonSchemaInClasspath("schemas/account.json"));
        Account single = response.as(Account.class);
        assertThat(single.id()).isEqualTo(fromList.id());
        assertThat(single.customerId()).isEqualTo(fromList.customerId());
        assertThat(single.type()).isEqualTo(fromList.type());
    }

    @Test(groups = "smoke", description = "Unknown account id returns 400 'Could not find account #<id>'")
    public void missingAccountReturns400() {
        Response response = api.account(99_999_999L);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.asString()).isEqualTo("Could not find account #99999999");
    }

    @Test(groups = "smoke", description = "Non-numeric account id returns 404")
    public void nonNumericAccountIdReturns404() {
        Response response = api.account("abc");

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test(groups = "smoke", description = "Without an Accept header the API answers in XML")
    public void responseIsXmlWithoutAcceptHeader() {
        long accountId = data.accounts(customer.id()).get(0).id();

        Response response = api.accountWithoutAcceptHeader(accountId);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).startsWith("application/xml");
        assertThat(response.asString()).contains("<account>").contains("<id>" + accountId + "</id>");
    }
}
