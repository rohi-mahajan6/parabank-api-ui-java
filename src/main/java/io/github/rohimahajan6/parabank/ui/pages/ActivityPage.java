package io.github.rohimahajan6.parabank.ui.pages;

import io.qameta.allure.Step;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Account Activity (activity.htm?id=...). The transaction table is rebuilt by JavaScript each time
 * the filter is applied, so after clicking Go the page waits for the old rows to go stale.
 */
public class ActivityPage {

    private static final By ACCOUNT_DETAILS = By.id("accountDetails");
    private static final By MONTH = By.id("month");
    private static final By TYPE = By.id("transactionType");
    private static final By GO = By.cssSelector("#activityForm input[type='submit'][value='Go']");
    private static final By ROWS = By.cssSelector("#transactionTable tbody tr");
    private static final By NO_TRANSACTIONS = By.id("noTransactions");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    public ActivityPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @Step("Open activity for account {accountId}")
    public ActivityPage open(long accountId) {
        driver.get(baseUrl + "/activity.htm?id=" + accountId);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(ACCOUNT_DETAILS, String.valueOf(accountId)));
        waitForAjaxIdle();
        waitForResult();
        return this;
    }

    /**
     * Applies the filter. The Go handler starts a jQuery request synchronously, so waiting for
     * jQuery to have no active requests means the table has been rebuilt from the response,
     * even when the table was empty before (no old row to go stale).
     */
    @Step("Filter by month {month} and type {type}")
    public ActivityPage filter(String month, String type) {
        new Select(driver.findElement(MONTH)).selectByVisibleText(month);
        new Select(driver.findElement(TYPE)).selectByVisibleText(type);
        driver.findElement(GO).click();
        waitForAjaxIdle();
        waitForResult();
        return this;
    }

    private void waitForAjaxIdle() {
        wait.until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                "return !window.jQuery || window.jQuery.active === 0;")));
    }

    public List<Row> rows() {
        return driver.findElements(ROWS).stream().map(ActivityPage::toRow).toList();
    }

    public boolean noTransactionsShown() {
        return driver.findElements(NO_TRANSACTIONS).stream().anyMatch(WebElement::isDisplayed);
    }

    private void waitForResult() {
        wait.until(d -> !d.findElements(ROWS).isEmpty() || noTransactionsShown());
    }

    private static Row toRow(WebElement tr) {
        List<WebElement> cells = tr.findElements(By.tagName("td"));
        return new Row(cells.get(0).getText().trim(), cells.get(1).getText().trim(),
                cells.get(2).getText().trim(), cells.get(3).getText().trim());
    }

    /** One row: Date, Transaction, Debit (-), Credit (+). Exactly one of debit/credit is filled. */
    public record Row(String date, String description, String debit, String credit) {
    }
}
