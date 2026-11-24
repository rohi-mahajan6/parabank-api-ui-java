package io.github.rohimahajan6.parabank.tests.contract;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Customer;
import io.github.rohimahajan6.parabank.support.LiveData;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

/** JSON schema checks on read-only GETs. Schemas live in src/test/resources/schemas/. */
public class ContractTest {

    private final ApiClient api = new ApiClient();
    private final LiveData data = new LiveData(api);
    private Customer customer;

    @BeforeClass(alwaysRun = true)
    public void findCustomer() {
        customer = data.demoCustomer();
    }

    @Test(groups = "smoke", description = "GET /customers/{id} matches the Customer schema")
    public void customerContract() {
        Response response = api.customer(customer.id());

        assertThat(response.statusCode()).isEqualTo(200);
        response.then().body(matchesJsonSchemaInClasspath("schemas/customer.json"));
    }

    @Test(groups = "smoke", description = "GET /accounts/{id} matches the Account schema")
    public void accountContract() {
        long accountId = data.accounts(customer.id()).get(0).id();

        Response response = api.account(accountId);

        assertThat(response.statusCode()).isEqualTo(200);
        response.then().body(matchesJsonSchemaInClasspath("schemas/account.json"));
    }

    @Test(groups = "smoke", description = "GET /customers/{id}/accounts matches the Account[] schema")
    public void accountListContract() {
        Response response = api.accounts(customer.id());

        assertThat(response.statusCode()).isEqualTo(200);
        response.then().body(matchesJsonSchemaInClasspath("schemas/account-list.json"));
    }

    @Test(groups = "smoke", description = "GET /accounts/{id}/transactions matches the Transaction[] schema")
    public void transactionListContract() {
        long accountId = data.accountWithTransaction(t -> true, "at least one transaction").account().id();

        Response response = api.transactions(accountId);

        assertThat(response.statusCode()).isEqualTo(200);
        response.then().body(matchesJsonSchemaInClasspath("schemas/transaction-list.json"));
    }
}
