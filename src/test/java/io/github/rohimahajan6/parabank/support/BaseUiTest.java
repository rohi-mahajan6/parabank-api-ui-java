package io.github.rohimahajan6.parabank.support;

import io.github.rohimahajan6.parabank.config.Config;
import io.github.rohimahajan6.parabank.ui.driver.DriverFactory;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.ByteArrayInputStream;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/** One fresh browser per test. On failure, a screenshot is attached to the Allure report. */
public abstract class BaseUiTest {

    protected WebDriver driver;
    protected final Config config = Config.get();

    @BeforeMethod(alwaysRun = true)
    public void startBrowser() {
        driver = DriverFactory.create();
    }

    @AfterMethod(alwaysRun = true)
    public void stopBrowser(ITestResult result) {
        if (driver == null) {
            return;
        }
        try {
            if (!result.isSuccess()) {
                byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                Allure.attachment("Screenshot on failure", "image/png", new ByteArrayInputStream(png),
                        AttachmentOptions.withFileExtension("png"));
            }
        } finally {
            driver.quit();
        }
    }
}
