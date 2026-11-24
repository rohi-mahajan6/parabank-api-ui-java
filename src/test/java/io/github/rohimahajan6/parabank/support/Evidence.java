package io.github.rohimahajan6.parabank.support;

import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Attaches plain-text evidence to the current Allure test and writes it to the log. */
public final class Evidence {

    private static final Logger LOG = LoggerFactory.getLogger(Evidence.class);

    private Evidence() {
    }

    public static void attach(String name, String text) {
        LOG.info("{}:\n{}", name, text);
        Allure.attachment(name, text);
    }
}
