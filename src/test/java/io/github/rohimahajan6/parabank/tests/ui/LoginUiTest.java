package io.github.rohimahajan6.parabank.tests.ui;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Customer;
import io.github.rohimahajan6.parabank.config.TestEnv;
import io.github.rohimahajan6.parabank.support.BaseUiTest;
import io.github.rohimahajan6.parabank.support.Evidence;
import io.github.rohimahajan6.parabank.support.LiveData;
import io.github.rohimahajan6.parabank.ui.pages.LoginPage;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginUiTest extends BaseUiTest {

    /**
     * The welcome text the UI should show for the demo customer, built from the API login response.
     * The public demo customer is shared and gets renamed (CI run 45580958874: "John Smith" became
     * "Anitha Madhavan", with a trailing space in the API's first name), so the name is fetched, not
     * hard-coded. Whitespace is collapsed the same way the browser renders it.
     */
    private String expectedWelcome() {
        Customer customer = new LiveData(new ApiClient()).demoCustomer();
        String expected = ("Welcome " + customer.firstName() + " " + customer.lastName()).replaceAll("\\s+", " ").trim();
        Evidence.attach("Expected welcome (from API login)", expected);
        return expected;
    }

    @Test(groups = "smoke", description = "Valid login shows 'Welcome <name from the API>' and lands on the overview")
    public void validLoginShowsWelcome() {
        String expected = expectedWelcome();
        LoginPage login = new LoginPage(driver, config.baseUrl()).open();

        login.logIn(config.username(), config.password());

        assertThat(login.welcomeText()).isEqualTo(expected);
        assertThat(driver.getCurrentUrl()).contains("/overview.htm");
    }

    @Test(groups = "smoke", description = "Empty credentials show 'Please enter a username and password.'")
    public void emptyCredentialsShowError() {
        LoginPage login = new LoginPage(driver, config.baseUrl()).open();

        login.logIn("", "");

        assertThat(login.errorText()).isEqualTo("Please enter a username and password.");
        assertThat(login.isWelcomeShown()).isFalse();
    }

    @Test(groups = "smoke", description = "Logging out returns to the login form")
    public void logoutReturnsToLoginForm() {
        String expected = expectedWelcome();
        LoginPage login = new LoginPage(driver, config.baseUrl()).open();
        login.logIn(config.username(), config.password());
        assertThat(login.welcomeText()).isEqualTo(expected);

        login.logOut();

        assertThat(login.isLoginFormVisible()).isTrue();
        assertThat(login.isWelcomeShown()).isFalse();
    }

    /*
     * Observed on the public site (2026-11-24): a wrong password, and even an unknown username,
     * logged in as John Smith. The API rejects the same credentials with 400. This test confirms
     * the behaviour on Docker (group "docker", run only by the regression suite). If it fails there,
     * Q1 becomes a confirmed finding. See docs/exploration-notes.md, Q1.
     * The public-site skip below is a safety net in case someone runs the group against public.
     */
    @Test(groups = "docker", description = "Wrong password is rejected (Docker: confirms exploration note Q1)")
    public void wrongPasswordIsRejected() {
        if (config.env() == TestEnv.PUBLIC) {
            throw new SkipException("Observed on public site, to be confirmed on Docker: a wrong password "
                    + "logged in as John Smith (exploration note Q1). Assertion runs only with TEST_ENV=docker.");
        }
        LoginPage login = new LoginPage(driver, config.baseUrl()).open();

        login.logIn(config.username(), "wrong-password-qa");

        assertThat(login.isWelcomeShown()).as("welcome panel after a wrong password").isFalse();
        assertThat(driver.getCurrentUrl()).doesNotContain("/overview.htm");
        assertThat(login.errorText()).isNotBlank();
    }
}
