package io.github.rohimahajan6.parabank.api;

import io.github.rohimahajan6.parabank.api.model.AccountType;
import io.github.rohimahajan6.parabank.api.model.Payee;
import io.github.rohimahajan6.parabank.config.Config;
import io.github.rohimahajan6.parabank.config.TestEnv;
import io.github.rohimahajan6.parabank.guard.DockerGuard;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;

/**
 * Thin REST client for the ParaBank API.
 *
 * <p>Always asks for JSON: without an Accept header ParaBank answers in XML.
 * Every request and response is attached to the Allure report by {@link AllureRestAssured}.
 * Read methods return the raw {@link Response} so tests can assert on status, headers and body.
 * Write methods call {@link DockerGuard} before any request is built.
 */
public class ApiClient {

    /** Date format the transaction filter endpoints accept. ISO dates return an empty list. */
    public static final DateTimeFormatter FILTER_DATE = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final RequestSpecification jsonSpec;
    private final RequestSpecification noAcceptSpec;
    private final TestEnv env;
    private final String apiUrl;

    public ApiClient() {
        this(Config.get().apiUrl(), Config.get().env());
    }

    public ApiClient(String apiUrl, TestEnv env) {
        this.apiUrl = apiUrl;
        this.env = env;
        this.noAcceptSpec = new RequestSpecBuilder()
                .setBaseUri(apiUrl)
                .addFilter(new AllureRestAssured())
                .log(LogDetail.URI)
                .build();
        this.jsonSpec = new RequestSpecBuilder()
                .addRequestSpecification(noAcceptSpec)
                .setAccept(ContentType.JSON)
                .build();
    }

    // ---- Read-only endpoints (safe on the public site)

    @Step("GET /login/{username}/****")
    public Response login(String username, String password) {
        return given(jsonSpec).get("/login/{username}/{password}", username, password);
    }

    @Step("GET /customers/{customerId}")
    public Response customer(long customerId) {
        return given(jsonSpec).get("/customers/{id}", customerId);
    }

    @Step("GET /customers/{customerId}/accounts")
    public Response accounts(long customerId) {
        return given(jsonSpec).get("/customers/{id}/accounts", customerId);
    }

    @Step("GET /accounts/{accountId}")
    public Response account(long accountId) {
        return account(String.valueOf(accountId));
    }

    /** String overload so tests can send malformed ids such as "abc". */
    @Step("GET /accounts/{accountId}")
    public Response account(String accountId) {
        return given(jsonSpec).get("/accounts/{id}", accountId);
    }

    @Step("GET /accounts/{accountId} without an Accept header")
    public Response accountWithoutAcceptHeader(long accountId) {
        return given(noAcceptSpec).get("/accounts/{id}", accountId);
    }

    @Step("GET /accounts/{accountId}/transactions")
    public Response transactions(long accountId) {
        return given(jsonSpec).get("/accounts/{id}/transactions", accountId);
    }

    @Step("GET /accounts/{accountId}/transactions/amount/{amount}")
    public Response transactionsByAmount(long accountId, BigDecimal amount) {
        return given(jsonSpec).get("/accounts/{id}/transactions/amount/{amount}", accountId, amount.toPlainString());
    }

    @Step("GET /accounts/{accountId}/transactions/fromDate/{from}/toDate/{to}")
    public Response transactionsByDateRange(long accountId, LocalDate from, LocalDate to) {
        return given(jsonSpec).get("/accounts/{id}/transactions/fromDate/{from}/toDate/{to}",
                accountId, from.format(FILTER_DATE), to.format(FILTER_DATE));
    }

    /** month: "All" or a month name such as "November". type: "All", "Credit" or "Debit". */
    @Step("GET /accounts/{accountId}/transactions/month/{month}/type/{type}")
    public Response transactionsByMonthAndType(long accountId, String month, String type) {
        return given(jsonSpec).get("/accounts/{id}/transactions/month/{month}/type/{type}", accountId, month, type);
    }

    // ---- Write endpoints (Docker only). Each one calls DockerGuard before building the request.

    /** Resets ParaBank to its default data set. Used once at the start of every Docker suite. */
    @Step("POST /initializeDB")
    public Response initializeDb() {
        requireWritable("initializeDB");
        return given(jsonSpec).post("/initializeDB");
    }

    @Step("POST /createAccount type {type} for customer {customerId} from {fromAccountId}")
    public Response createAccount(long customerId, AccountType type, long fromAccountId) {
        requireWritable("createAccount");
        return given(jsonSpec)
                .queryParam("customerId", customerId)
                .queryParam("newAccountType", type.code())
                .queryParam("fromAccountId", fromAccountId)
                .post("/createAccount");
    }

    @Step("POST /transfer {amount} from {fromAccountId} to {toAccountId}")
    public Response transfer(long fromAccountId, long toAccountId, BigDecimal amount) {
        return transfer(fromAccountId, toAccountId, amount.toPlainString());
    }

    /** String overload so negative tests can send values such as "abc". */
    @Step("POST /transfer {amount} from {fromAccountId} to {toAccountId}")
    public Response transfer(long fromAccountId, long toAccountId, String amount) {
        requireWritable("transfer");
        return given(jsonSpec)
                .queryParam("fromAccountId", fromAccountId)
                .queryParam("toAccountId", toAccountId)
                .queryParam("amount", amount)
                .post("/transfer");
    }

    @Step("POST /billpay {amount} from {accountId}")
    public Response billPay(long accountId, BigDecimal amount, Payee payee) {
        requireWritable("billpay");
        return given(jsonSpec)
                .contentType(ContentType.JSON)
                .queryParam("accountId", accountId)
                .queryParam("amount", amount.toPlainString())
                .body(payee)
                .post("/billpay");
    }

    @Step("POST /requestLoan {amount} with down payment {downPayment} from {fromAccountId}")
    public Response requestLoan(long customerId, BigDecimal amount, BigDecimal downPayment, long fromAccountId) {
        requireWritable("requestLoan");
        return given(jsonSpec)
                .queryParam("customerId", customerId)
                .queryParam("amount", amount.toPlainString())
                .queryParam("downPayment", downPayment.toPlainString())
                .queryParam("fromAccountId", fromAccountId)
                .post("/requestLoan");
    }

    private void requireWritable(String operation) {
        DockerGuard.requireWritable(operation, env, apiUrl);
    }
}
