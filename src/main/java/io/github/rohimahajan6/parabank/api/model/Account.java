package io.github.rohimahajan6.parabank.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/** Shape observed from GET /accounts/{id} and GET /customers/{id}/accounts. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Account(long id, long customerId, String type, BigDecimal balance) {
}
