package io.github.rohimahajan6.parabank.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Bill pay payee, sent as the JSON body of POST /billpay (field names taken from billpay.htm). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Payee(String name, Address address, String phoneNumber, String accountNumber) {

    /** Obviously fake payee data. */
    public static Payee fake(String name, String accountNumber) {
        return new Payee(name, new Address("1 Test Street", "Testville", "TS", "00000"), "555-0100", accountNumber);
    }
}
