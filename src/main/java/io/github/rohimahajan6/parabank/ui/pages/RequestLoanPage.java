package io.github.rohimahajan6.parabank.ui.pages;

import io.github.rohimahajan6.parabank.guard.DockerGuard;
import io.qameta.allure.Step;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/** requestloan.htm. The result panel shows provider, date, status, and either the new account or a reason. */
public class RequestLoanPage {

    private static final By AMOUNT = By.id("amount");
    private static final By DOWN_PAYMENT = By.id("downPayment");
    private static final By FROM_ACCOUNT = By.id("fromAccountId");
    private static final By APPLY = By.cssSelector("input[type='button'][value='Apply Now']");
    private static final By RESULT = By.id("requestLoanResult");
    private static final By STATUS = By.id("loanStatus");
    private static final By APPROVED = By.id("loanRequestApproved");
    private static final By NEW_ACCOUNT_ID = By.id("newAccountId");
    private static final By DENIED_REASON = By.cssSelector("#loanRequestDenied p.error");
    private static final By ERROR = By.id("requestLoanError");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    public RequestLoanPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @Step("Open the Request Loan page")
    public RequestLoanPage open() {
        driver.get(baseUrl + "/requestloan.htm");
        wait.until(d -> !new Select(d.findElement(FROM_ACCOUNT)).getOptions().isEmpty());
        return this;
    }

    @Step("Apply for a loan of {amount} with down payment {downPayment} from {fromAccountId}")
    public Result apply(String amount, String downPayment, long fromAccountId) {
        DockerGuard.requireWritable("UI request loan");
        driver.findElement(AMOUNT).clear();
        driver.findElement(AMOUNT).sendKeys(amount);
        driver.findElement(DOWN_PAYMENT).clear();
        driver.findElement(DOWN_PAYMENT).sendKeys(downPayment);
        PageWaits.selectOptionByValue(wait, driver, FROM_ACCOUNT, String.valueOf(fromAccountId));
        driver.findElement(APPLY).click();
        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(RESULT),
                ExpectedConditions.visibilityOfElementLocated(ERROR)));
        if (driver.findElement(ERROR).isDisplayed()) {
            throw new IllegalStateException("Loan request failed: " + driver.findElement(ERROR).getText());
        }
        String status = wait.until(d -> {
            String s = d.findElement(STATUS).getText().trim();
            return s.isEmpty() ? null : s;
        });
        boolean approved = driver.findElement(APPROVED).isDisplayed();
        String newAccount = approved ? driver.findElement(NEW_ACCOUNT_ID).getText().trim() : null;
        String reason = approved ? null : driver.findElement(DENIED_REASON).getText().trim();
        return new Result(status, newAccount == null || newAccount.isEmpty() ? null : Long.parseLong(newAccount), reason);
    }

    /** status is "Approved" or "Denied" as shown by the page. */
    public record Result(String status, Long newAccountId, String deniedReason) {
    }
}
