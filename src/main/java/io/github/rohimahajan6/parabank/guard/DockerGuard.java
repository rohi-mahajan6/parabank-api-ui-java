package io.github.rohimahajan6.parabank.guard;

import io.github.rohimahajan6.parabank.config.Config;
import io.github.rohimahajan6.parabank.config.TestEnv;
import java.net.URI;
import java.util.Locale;

/**
 * Blocks every operation that creates or changes data unless the tests run against Docker ParaBank.
 *
 * <p>Two independent checks, so one misconfiguration is not enough to write to the public site:
 * <ol>
 *   <li>TEST_ENV must be {@code docker}.</li>
 *   <li>The target host must not be the public ParaBank host, whatever TEST_ENV says.</li>
 * </ol>
 * Every write method in the API client and every write action in page objects calls
 * {@link #requireWritable(String)} before sending anything.
 */
public final class DockerGuard {

    static final String PUBLIC_HOST = "parabank.parasoft.com";

    private DockerGuard() {
    }

    /** Checks the current configuration: both the API URL and the UI base URL must be safe. */
    public static void requireWritable(String operation) {
        Config config = Config.get();
        requireWritable(operation, config.env(), config.apiUrl());
        requireWritable(operation, config.env(), config.baseUrl());
    }

    /** Same check with explicit inputs, so it can be unit tested without any configuration. */
    public static void requireWritable(String operation, TestEnv env, String targetUrl) {
        if (env != TestEnv.DOCKER) {
            throw new WriteBlockedException("Blocked write operation '" + operation + "': TEST_ENV=" + env
                    + ". Writes are only allowed with TEST_ENV=docker.");
        }
        if (isPublicHost(targetUrl)) {
            throw new WriteBlockedException("Blocked write operation '" + operation + "': target " + targetUrl
                    + " is the public ParaBank site, even though TEST_ENV=docker.");
        }
    }

    static boolean isPublicHost(String url) {
        if (url == null) {
            return true; // unknown target: treat as unsafe
        }
        String host;
        try {
            host = URI.create(url).getHost();
        } catch (IllegalArgumentException e) {
            return true;
        }
        return host == null || host.toLowerCase(Locale.ROOT).endsWith(PUBLIC_HOST);
    }
}
