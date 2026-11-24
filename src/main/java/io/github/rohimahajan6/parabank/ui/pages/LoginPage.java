package io.github.rohimahajan6.parabank.ui.pages;

import io.qameta.allure.Step;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The login box in the left panel. It appears on every page while logged out.
 * Logging in and out is read-only: it creates no data.
 */
public class LoginPage {

    private static final By USERNAME = By.name("username");
    private static final By PASSWORD = By.name("password");
    private static final By LOG_IN = By.cssSelector("input[type='submit'][value='Log In']");
    private static final By ERROR = By.cssSelector("#rightPanel .error");
    private static final By WELCOME = By.cssSelector("#leftPanel .smallText");
    private static final By LOG_OUT = By.cssSelector("#leftPanel a[href*='logout.htm']");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    public LoginPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @Step("Open the home page")
    public LoginPage open() {
        driver.get(baseUrl + "/index.htm");
        wait.until(ExpectedConditions.visibilityOfElementLocated(USERNAME));
        return this;
    }

    /**
     * Submits the login form and waits until the browser has left the submitted page.
     * Without this wait, a test that navigates right after logging in can be overtaken by the
     * login redirect to overview.htm (seen on 2026-11-24 in ActivityUiTest).
     */
    @Step("Log in as {username}")
    public void logIn(String username, String password) {
        driver.findElement(USERNAME).clear();
        driver.findElement(USERNAME).sendKeys(username);
        driver.findElement(PASSWORD).clear();
        driver.findElement(PASSWORD).sendKeys(password);
        WebElement button = driver.findElement(LOG_IN);
        button.click();
        wait.until(ExpectedConditions.stalenessOf(button));
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    @Step("Log out")
    public void logOut() {
        wait.until(ExpectedConditions.elementToBeClickable(LOG_OUT)).click();
    }

    /** Text such as "Welcome John Smith", shown in the left panel after a successful login. */
    public String welcomeText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(WELCOME)).getText().trim();
    }

    /** Error text shown in the right panel, e.g. "Please enter a username and password." */
    public String errorText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR)).getText().trim();
    }

    /** True once the login form is shown again (for example after logout). */
    public boolean isLoginFormVisible() {
        return !wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(USERNAME)).isEmpty()
                && driver.findElement(LOG_IN).isDisplayed();
    }

    public boolean isWelcomeShown() {
        return !driver.findElements(WELCOME).isEmpty();
    }
}
