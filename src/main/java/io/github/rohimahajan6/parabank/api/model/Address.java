package io.github.rohimahajan6.parabank.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Address(String street, String city, String state, String zipCode) {
}
