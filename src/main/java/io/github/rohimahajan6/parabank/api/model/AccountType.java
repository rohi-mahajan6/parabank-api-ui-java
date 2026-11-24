package io.github.rohimahajan6.parabank.api.model;

/** Account types and the numeric codes createAccount expects (the UI's type select uses the same codes). */
public enum AccountType {
    CHECKING(0),
    SAVINGS(1),
    LOAN(2);

    private final int code;

    AccountType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
