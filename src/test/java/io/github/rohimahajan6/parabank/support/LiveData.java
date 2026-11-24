package io.github.rohimahajan6.parabank.support;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.api.model.Account;
import io.github.rohimahajan6.parabank.api.model.Customer;
import io.github.rohimahajan6.parabank.api.model.Transaction;
import io.github.rohimahajan6.parabank.config.Config;
import io.restassured.response.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.testng.SkipException;

/**
 * Finds test data at run time with read-only GETs.
 *
 * <p>The public site's data is shared and changes all the time (during exploration the demo
 * customer's account count went from 16 to 22, then to 2 after a reset). Nothing is hard-coded:
 * tests ask for "an account with at least one debit" and skip with a clear reason if the shared
 * data has none at that moment. A skip is reported as a skip, never as a pass.
 */
public final class LiveData {

    private final ApiClient api;
    private final Config config = Config.get();

    public LiveData(ApiClient api) {
        this.api = api;
    }

    public Customer demoCustomer() {
        Response response = api.login(config.username(), config.password());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Demo login failed with HTTP " + response.statusCode()
                    + ": " + response.asString());
        }
        return response.as(Customer.class);
    }

    public List<Account> accounts(long customerId) {
        return Arrays.asList(api.accounts(customerId).as(Account[].class));
    }

    /** Transactions for an account; an empty list when ParaBank answers 400 "Could not find transactions". */
    public List<Transaction> transactions(long accountId) {
        Response response = api.transactions(accountId);
        if (response.statusCode() != 200) {
            return List.of();
        }
        return Arrays.asList(response.as(Transaction[].class));
    }

    /** Demo customer's accounts, highest balance first. Write tests fund from the first one. */
    public List<Account> accountsByBalanceDesc(long customerId) {
        return accounts(customerId).stream()
                .sorted((a, b) -> b.balance().compareTo(a.balance()))
                .toList();
    }

    /** Two different accounts of the demo customer: [richest, another]. */
    public Account[] twoAccounts(long customerId) {
        List<Account> sorted = accountsByBalanceDesc(customerId);
        if (sorted.size() < 2) {
            throw new IllegalStateException("Need at least two accounts for customer " + customerId
                    + ", found " + sorted.size());
        }
        return new Account[] {sorted.get(0), sorted.get(1)};
    }

    /** Number of transactions on an account with this type and amount (used to detect new ones). */
    public long countTransactions(long accountId, String type, java.math.BigDecimal amount) {
        return transactions(accountId).stream()
                .filter(t -> t.type().equals(type) && t.amount().compareTo(amount) == 0)
                .count();
    }

    /** First account of the demo customer that has a transaction matching the filter, or a skip. */
    public AccountWithTransaction accountWithTransaction(Predicate<Transaction> filter, String what) {
        Customer customer = demoCustomer();
        for (Account account : accounts(customer.id())) {
            Optional<Transaction> match = transactions(account.id()).stream().filter(filter).findFirst();
            if (match.isPresent()) {
                return new AccountWithTransaction(account, match.get());
            }
        }
        throw new SkipException("Shared public data has no account with " + what
                + " for customer " + customer.id() + " right now.");
    }

    public record AccountWithTransaction(Account account, Transaction transaction) {
    }
}
