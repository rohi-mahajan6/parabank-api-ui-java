package io.github.rohimahajan6.parabank.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Shape observed from GET /accounts/{id}/transactions.
 * {@code date} is epoch milliseconds; observed values are midnight UTC (date only).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Transaction(long id, long accountId, String type, long date, BigDecimal amount, String description) {

    public LocalDate utcDate() {
        return Instant.ofEpochMilli(date).atZone(ZoneOffset.UTC).toLocalDate();
    }
}
