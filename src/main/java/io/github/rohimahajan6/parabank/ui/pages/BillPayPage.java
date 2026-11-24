package io.github.rohimahajan6.parabank.ui.pages;

import io.github.rohimahajan6.parabank.api.model.Payee;
import io.github.rohimahajan6.parabank.guard.DockerGuard;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * billpay.htm. Fields are located by name: the phone field gets a random id on every page load.
 * Validation messages are spans with ids starting "validationModel-"; the result is #billpayResult.
 */
public class BillPayPage {

    private static final By FROM_ACCOUNT = By.name("fromAccountId");
    private static final By SEND = By.cssSelector("input[type='button'][value='Send Payment']");
    private static final By VALIDATION = By.cssSelector("[id^='validationModel-']");
    private static final By RESULT = By.id("billpayResult");
    private static final By RESULT_PAYEE = By.cssSelector("#billpayResult #payeeName");
    private static final By RESULT_AMOUNT = By.cssSelector("#billpayResult #amount");
    private static final By RESULT_FROM = By.cssSelector("#billpayResult #fromAccountId");
    private static final By ERROR = By.id("billpayError");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    public BillPayPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @Step("Open the Bill Pay page")
    public BillPayPage open() {
        driver.get(baseUrl + "/billpay.htm");
        wait.until(ExpectedConditions.elementToBeClickable(SEND));
        return this;
    }

    @Step("Fill bill pay form: account {accountNumber} / verify {verifyAccount}, amount {amount}")
    public BillPayPage fill(Payee payee, String accountNumber, String verifyAccount, String amount, long fromAccountId) {
        type("payee.name", payee.name());
        type("payee.address.street", payee.address().street());
        type("payee.address.city", payee.address().city());
        type("payee.address.state", payee.address().state());
        type("payee.address.zipCode", payee.address().zipCode());
        type("payee.phoneNumber", payee.phoneNumber());
        type("payee.accountNumber", accountNumber);
        type("verifyAccount", verifyAccount);
        type("amount", amount);
        PageWaits.selectOptionByValue(wait, driver, FROM_ACCOUNT, String.valueOf(fromAccountId));
        return this;
    }

    @Step("Click Send Payment")
    public BillPayPage send() {
        DockerGuard.requireWritable("UI bill pay");
        driver.findElement(SEND).click();
        return this;
    }

    /** Texts of the validation messages currently visible. */
    public List<String> visibleValidationMessages() {
        return driver.findElements(VALIDATION).stream()
                .filter(WebElement::isDisplayed)
                .map(e -> e.getText().trim())
                .toList();
    }

    public boolean resultShown() {
        return driver.findElement(RESULT).isDisplayed();
    }

    /** Waits for the success panel (or fails on the error panel) and returns what it shows. */
    public Result waitForResult() {
        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(RESULT),
                ExpectedConditions.visibilityOfElementLocated(ERROR)));
        if (driver.findElement(ERROR).isDisplayed()) {
            throw new IllegalStateException("Bill pay failed: " + driver.findElement(ERROR).getText());
        }
        return new Result(driver.findElement(RESULT_PAYEE).getText().trim(),
                driver.findElement(RESULT_AMOUNT).getText().trim(),
                driver.findElement(RESULT_FROM).getText().trim());
    }

    private void type(String name, String value) {
        WebElement field = driver.findElement(By.name(name));
        field.clear();
        field.sendKeys(value);
    }

    public record Result(String payeeName, String amount, String fromAccountId) {
    }
}
