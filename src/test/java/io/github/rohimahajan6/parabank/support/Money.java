package io.github.rohimahajan6.parabank.support;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Account;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money helpers. Amounts are always BigDecimal; never double. */
public final class Money {

    private Money() {
    }

    public static BigDecimal of(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal balance(ApiClient api, long accountId) {
        return api.account(accountId).as(Account.class).balance();
    }

    /** "$1234.50": the transfer and bill pay result panels format with toFixed(2), no grouping. */
    public static String display(BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
