package io.github.rohimahajan6.parabank.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Small wait helpers shared by page objects. */
final class PageWaits {

    private PageWaits() {
    }

    /** Account selects are filled by an API call after page load; wait for the option, then select it. */
    static void selectOptionByValue(WebDriverWait wait, WebDriver driver, By select, String value) {
        wait.until(d -> new Select(d.findElement(select)).getOptions().stream()
                .anyMatch(o -> value.equals(o.getAttribute("value"))));
        new Select(driver.findElement(select)).selectByValue(value);
    }
}
