package io.github.rohimahajan6.parabank.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;

/**
 * Response of POST /requestLoan, as read by requestloan.htm:
 * responseDate, loanProviderName, approved, message, accountId.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoanResponse(Long responseDate, String loanProviderName, boolean approved, String message, Long accountId) {

    /** Denial codes that requestloan.htm knows how to display. */
    public static final Set<String> DENIAL_CODES = Set.of(
            "error.insufficient.funds.for.down.payment",
            "error.insufficient.funds.and.down.payment",
            "error.insufficient.funds",
            "error.insufficient.down.payment");
}
