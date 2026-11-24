package io.github.rohimahajan6.parabank.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Test settings. Each key is looked up in this order:
 * JVM system property (-DKEY=...), then environment variable, then the .env file.
 * The .env file is optional, so CI can rely on environment variables alone.
 */
public final class Config {

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();
    private static final Config INSTANCE = new Config();

    private final TestEnv env;
    private final String baseUrl;
    private final String apiUrl;
    private final String username;
    private final String password;
    private final boolean headless;

    private Config() {
        this.env = TestEnv.from(get("TEST_ENV"));
        this.baseUrl = stripTrailingSlash(required("PARABANK_BASE_URL"));
        this.apiUrl = stripTrailingSlash(required("PARABANK_API_URL"));
        this.username = required("PARABANK_USERNAME");
        this.password = required("PARABANK_PASSWORD");
        // Headless in CI, visible locally, unless HEADLESS says otherwise.
        String headlessSetting = get("HEADLESS");
        this.headless = headlessSetting == null ? get("CI") != null : Boolean.parseBoolean(headlessSetting);
    }

    public static Config get() {
        return INSTANCE;
    }

    public TestEnv env() {
        return env;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String apiUrl() {
        return apiUrl;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public boolean headless() {
        return headless;
    }

    private static String get(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        if (value == null || value.isBlank()) {
            value = DOTENV.get(key);
        }
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String required(String key) {
        String value = get(key);
        if (value == null) {
            throw new IllegalStateException(
                    "Missing setting " + key + ". Copy .env.example to .env and fill it in, "
                            + "or set it as an environment variable.");
        }
        return value;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
