package io.github.rohimahajan6.parabank.support;

import io.qameta.allure.Allure;
import org.testng.SkipException;

/**
 * Expected-failure wrapper for confirmed ParaBank defects (TestNG has no test.fail()).
 *
 * <p>The block passed in asserts the CORRECT behaviour.
 * <ul>
 *   <li>If it fails, the defect still reproduces: the test is reported as skipped with a
 *       "KNOWN DEFECT PB-xxx" message, so the build stays green but the defect stays visible.
 *       Allure groups these under the "Known defects" category (see allure/categories.json).</li>
 *   <li>If it passes, the defect is gone: the test fails, so the README can be updated and the
 *       marker removed. A known defect can never silently disappear.</li>
 * </ul>
 * Evidence (status, body, balances) should be attached before calling this.
 */
public final class KnownDefect {

    static final String README_FINDINGS =
            "https://github.com/rohi-mahajan6/parabank-api-ui-java#7-known-defects--findings";

    private KnownDefect() {
    }

    public static void expect(String id, String summary, Runnable correctBehaviour) {
        Allure.label("tag", "known-defect");
        Allure.label("tag", id);
        Allure.link(id + " in README", README_FINDINGS);
        try {
            correctBehaviour.run();
        } catch (AssertionError e) {
            throw new SkipException("KNOWN DEFECT " + id + " (expected failure, see README): " + summary
                    + " | assertion: " + oneLine(e.getMessage()));
        }
        throw new AssertionError("Known defect " + id + " did not reproduce: " + summary
                + ". The correct behaviour now passes; update the README and remove the known-defect marker.");
    }

    private static String oneLine(String message) {
        return message == null ? "" : message.replaceAll("\\s+", " ").trim();
    }
}
