package io.github.rohimahajan6.parabank.support;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.config.Config;
import io.github.rohimahajan6.parabank.config.TestEnv;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * Resets Docker ParaBank to its default data with POST /initializeDB once, before the suite starts,
 * so every Docker run begins from the same known data.
 *
 * <p>Registered only in testng-regression.xml. With TEST_ENV=public it does nothing, and the
 * request itself goes through DockerGuard, so the public site can never be reset.
 */
public class DockerResetListener implements ISuiteListener {

    private static final Logger LOG = LoggerFactory.getLogger(DockerResetListener.class);

    @Override
    public void onStart(ISuite suite) {
        if (Config.get().env() != TestEnv.DOCKER) {
            LOG.info("TEST_ENV={} : skipping database reset", Config.get().env());
            return;
        }
        Response response = new ApiClient().initializeDb();
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("POST /initializeDB failed with HTTP " + response.statusCode()
                    + ": " + response.asString());
        }
        LOG.info("Docker ParaBank reset with POST /initializeDB (HTTP {})", response.statusCode());
    }
}
