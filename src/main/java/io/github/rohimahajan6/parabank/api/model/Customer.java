package io.github.rohimahajan6.parabank.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Shape observed from GET /login/{username}/{password} and GET /customers/{id}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Customer(
        long id,
        String firstName,
        String lastName,
        Address address,
        String phoneNumber,
        String ssn) {
}
