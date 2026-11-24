package io.github.rohimahajan6.parabank.config;

import java.util.Locale;

/** Where the tests run. Anything unknown or missing falls back to PUBLIC, the read-only mode. */
public enum TestEnv {
    /** parabank.parasoft.com: shared public data, read-only. */
    PUBLIC,
    /** Throwaway ParaBank container: full suite, including writes. */
    DOCKER;

    public static TestEnv from(String value) {
        if (value == null || value.isBlank()) {
            return PUBLIC;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "docker" -> DOCKER;
            case "public" -> PUBLIC;
            default -> throw new IllegalArgumentException(
                    "Unknown TEST_ENV '" + value + "'. Use 'public' or 'docker'.");
        };
    }
}
