package io.github.rohimahajan6.parabank.tests.ui;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Payee;
import io.github.rohimahajan6.parabank.support.BaseUiTest;
import io.github.rohimahajan6.parabank.support.LiveData;
import io.github.rohimahajan6.parabank.ui.pages.BillPayPage;
import io.github.rohimahajan6.parabank.ui.pages.LoginPage;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bill pay validation. Invalid input should never reach the server, but the Send Payment button
 * is still a write action, so these run on Docker only (group "docker").
 * Expected texts are the ones present in billpay.htm.
 */
public class BillPayUiTest extends BaseUiTest {

    @Test(groups = "docker", description = "Empty bill pay submit shows every required-field message")
    public void emptySubmitShowsAllValidationMessages() {
        new LoginPage(driver, config.baseUrl()).open().logIn(config.username(), config.password());

        BillPayPage page = new BillPayPage(driver, config.baseUrl()).open().send();

        assertThat(page.visibleValidationMessages()).containsExactlyInAnyOrder(
                "Payee name is required.",
                "Address is required.",
                "City is required.",
                "State is required.",
                "Zip Code is required.",
                "Phone number is required.",
                "Account number is required.",
                "Account number is required.",
                "The amount cannot be empty.");
        assertThat(page.resultShown()).isFalse();
    }

    @Test(groups = "docker", description = "Mismatched account and verify-account shows the mismatch message")
    public void mismatchedAccountNumbersShowError() {
        LiveData data = new LiveData(new ApiClient());
        long fromAccount = data.accountsByBalanceDesc(data.demoCustomer().id()).get(0).id();
        new LoginPage(driver, config.baseUrl()).open().logIn(config.username(), config.password());

        BillPayPage page = new BillPayPage(driver, config.baseUrl()).open()
                .fill(Payee.fake("QA Mismatch Payee", "12345"), "12345", "54321", "1.00", fromAccount)
                .send();

        assertThat(page.visibleValidationMessages()).containsExactly("The account numbers do not match.");
        assertThat(page.resultShown()).isFalse();
    }
}
