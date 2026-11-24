package io.github.rohimahajan6.parabank.ui.pages;

import io.github.rohimahajan6.parabank.guard.DockerGuard;
import io.qameta.allure.Step;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/** transfer.htm. Submits through JavaScript; the result appears in #showResult or #showError. */
public class TransferPage {

    private static final By AMOUNT = By.id("amount");
    private static final By FROM_ACCOUNT = By.id("fromAccountId");
    private static final By TO_ACCOUNT = By.id("toAccountId");
    private static final By TRANSFER = By.cssSelector("#transferForm input[type='submit'][value='Transfer']");
    private static final By RESULT = By.id("showResult");
    private static final By AMOUNT_RESULT = By.id("amountResult");
    private static final By FROM_RESULT = By.id("fromAccountIdResult");
    private static final By TO_RESULT = By.id("toAccountIdResult");
    private static final By ERROR = By.id("showError");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    public TransferPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @Step("Open the Transfer Funds page")
    public TransferPage open() {
        driver.get(baseUrl + "/transfer.htm");
        wait.until(d -> !new Select(d.findElement(FROM_ACCOUNT)).getOptions().isEmpty());
        return this;
    }

    @Step("Transfer {amount} from {fromAccountId} to {toAccountId}")
    public Result transfer(String amount, long fromAccountId, long toAccountId) {
        DockerGuard.requireWritable("UI transfer");
        driver.findElement(AMOUNT).clear();
        driver.findElement(AMOUNT).sendKeys(amount);
        PageWaits.selectOptionByValue(wait, driver, FROM_ACCOUNT, String.valueOf(fromAccountId));
        PageWaits.selectOptionByValue(wait, driver, TO_ACCOUNT, String.valueOf(toAccountId));
        driver.findElement(TRANSFER).click();
        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(RESULT),
                ExpectedConditions.visibilityOfElementLocated(ERROR)));
        if (driver.findElement(ERROR).isDisplayed()) {
            return new Result(false, null, null, null);
        }
        return new Result(true, driver.findElement(AMOUNT_RESULT).getText().trim(),
                driver.findElement(FROM_RESULT).getText().trim(), driver.findElement(TO_RESULT).getText().trim());
    }

    /** What the page showed after submitting. Amount is formatted by the page, e.g. "$17.29". */
    public record Result(boolean completed, String amount, String fromAccountId, String toAccountId) {
    }
}
