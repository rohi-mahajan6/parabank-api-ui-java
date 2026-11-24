package io.github.rohimahajan6.parabank.tests.ui;

import io.github.rohimahajan6.parabank.config.TestEnv;
import io.github.rohimahajan6.parabank.support.BaseUiTest;
import io.github.rohimahajan6.parabank.ui.pages.LoginPage;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginUiTest extends BaseUiTest {

    @Test(groups = "smoke", description = "Valid login shows 'Welcome John Smith' and lands on the overview")
    public void validLoginShowsWelcome() {
        LoginPage login = new LoginPage(driver, config.baseUrl()).open();

        login.logIn(config.username(), config.password());

        assertThat(login.welcomeText()).isEqualTo("Welcome John Smith");
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
        LoginPage login = new LoginPage(driver, config.baseUrl()).open();
        login.logIn(config.username(), config.password());
        assertThat(login.welcomeText()).isEqualTo("Welcome John Smith");

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
