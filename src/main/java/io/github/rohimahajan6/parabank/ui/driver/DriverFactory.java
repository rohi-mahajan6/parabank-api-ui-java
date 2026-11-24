package io.github.rohimahajan6.parabank.ui.driver;

import io.github.rohimahajan6.parabank.config.Config;
import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Creates Chrome sessions. Selenium Manager (built into Selenium 4) finds or downloads a matching
 * driver, so no driver binary is kept in the repo.
 */
public final class DriverFactory {

    private DriverFactory() {
    }

    public static WebDriver create() {
        return create(Config.get().headless());
    }

    public static WebDriver create(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1366,900", "--disable-search-engine-choice-screen");
        WebDriver driver = new ChromeDriver(options);
        // Explicit waits only; an implicit wait would mix badly with them.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        return driver;
    }
}
