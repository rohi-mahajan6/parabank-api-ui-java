package io.github.rohimahajan6.parabank.ui.pages;

import io.github.rohimahajan6.parabank.api.model.AccountType;
import io.github.rohimahajan6.parabank.guard.DockerGuard;
import io.qameta.allure.Step;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/** openaccount.htm. The from-account list is filled by JavaScript; the result appears in #openAccountResult. */
public class OpenAccountPage {

    private static final By TYPE = By.id("type");
    private static final By FROM_ACCOUNT = By.id("fromAccountId");
    private static final By OPEN = By.cssSelector("input[type='button'][value='Open New Account']");
    private static final By RESULT = By.id("openAccountResult");
    private static final By NEW_ACCOUNT_ID = By.id("newAccountId");
    private static final By ERROR = By.id("openAccountError");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    public OpenAccountPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @Step("Open the Open New Account page")
    public OpenAccountPage open() {
        driver.get(baseUrl + "/openaccount.htm");
        wait.until(d -> !new Select(d.findElement(FROM_ACCOUNT)).getOptions().isEmpty());
        return this;
    }

    @Step("Open a {type} account funded from {fromAccountId}")
    public long openAccount(AccountType type, long fromAccountId) {
        DockerGuard.requireWritable("UI open account");
        new Select(driver.findElement(TYPE)).selectByValue(String.valueOf(type.code()));
        PageWaits.selectOptionByValue(wait, driver, FROM_ACCOUNT, String.valueOf(fromAccountId));
        driver.findElement(OPEN).click();
        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(RESULT),
                ExpectedConditions.visibilityOfElementLocated(ERROR)));
        if (driver.findElement(ERROR).isDisplayed()) {
            throw new IllegalStateException("Open account failed: " + driver.findElement(ERROR).getText());
        }
        String id = wait.until(d -> {
            String text = d.findElement(NEW_ACCOUNT_ID).getText().trim();
            return text.isEmpty() ? null : text;
        });
        return Long.parseLong(id);
    }
}
