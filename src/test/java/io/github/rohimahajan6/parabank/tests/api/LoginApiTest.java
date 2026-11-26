package io.github.rohimahajan6.parabank.tests.api;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Customer;
import io.github.rohimahajan6.parabank.config.Config;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

public class LoginApiTest {

    private static final String INVALID_CREDENTIALS = "Invalid username and/or password";

    private final ApiClient api = new ApiClient();
    private final Config config = Config.get();

    @Test(groups = "smoke", description = "Valid login returns 200 and a customer matching the schema")
    public void loginWithValidCredentialsReturnsCustomer() {
        Response response = api.login(config.username(), config.password());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).startsWith("application/json");
        response.then().body(matchesJsonSchemaInClasspath("schemas/customer.json"));

        // The public demo customer is shared and can be renamed by anyone (CI run 45580958874 saw
        // "John Smith" become "Anitha Madhavan"), so the name is not hard-coded. The login response
        // must name a customer, and it must be the same customer GET /customers/{id} returns.
        Customer customer = response.as(Customer.class);
        assertThat(customer.firstName()).as("first name").isNotBlank();
        assertThat(customer.lastName()).as("last name").isNotBlank();

        Customer byId = api.customer(customer.id()).as(Customer.class);
        assertThat(byId.firstName()).as("first name from GET /customers/" + customer.id()).isEqualTo(customer.firstName());
        assertThat(byId.lastName()).as("last name from GET /customers/" + customer.id()).isEqualTo(customer.lastName());
    }

    @Test(groups = "smoke", description = "Wrong password returns 400 'Invalid username and/or password'")
    public void loginWithWrongPasswordReturns400() {
        Response response = api.login(config.username(), "wrong-password-qa");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.asString()).isEqualTo(INVALID_CREDENTIALS);
    }

    @Test(groups = "smoke", description = "Unknown user returns 400 'Invalid username and/or password'")
    public void loginWithUnknownUserReturns400() {
        Response response = api.login("no_such_user_qa_9f3k", "whatever");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.asString()).isEqualTo(INVALID_CREDENTIALS);
    }
}
