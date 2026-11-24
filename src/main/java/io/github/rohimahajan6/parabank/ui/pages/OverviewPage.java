package io.github.rohimahajan6.parabank.ui.pages;

import io.qameta.allure.Step;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Accounts Overview. The table is filled by JavaScript after an API call, so the page waits for
 * the first account link instead of the page load. The last row is a "Total" row without a link.
 */
public class OverviewPage {

    private static final By TABLE = By.id("accountTable");
    private static final By ACCOUNT_ROWS = By.xpath("//table[@id='accountTable']/tbody/tr[td/a]");
    private static final By HEADERS = By.cssSelector("#accountTable thead th");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public OverviewPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @Step("Wait for the accounts table")
    public OverviewPage waitForAccounts() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(TABLE));
        wait.until(ExpectedConditions.presenceOfElementLocated(ACCOUNT_ROWS));
        return this;
    }

    public List<String> headers() {
        return driver.findElements(HEADERS).stream().map(h -> h.getText().trim()).toList();
    }

    public List<Row> accountRows() {
        return driver.findElements(ACCOUNT_ROWS).stream().map(OverviewPage::toRow).toList();
    }

    private static Row toRow(WebElement tr) {
        List<WebElement> cells = tr.findElements(By.tagName("td"));
        return new Row(cells.get(0).getText().trim(), cells.get(1).getText().trim(), cells.get(2).getText().trim());
    }

    public record Row(String account, String balance, String available) {
    }
}
