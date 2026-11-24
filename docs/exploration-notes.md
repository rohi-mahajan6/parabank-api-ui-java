# Exploration notes (public site, 2026-11-24)

Read-only exploration of https://parabank.parasoft.com before any tests were written: page loads, login and logout, empty-form validation, and GET requests to the REST API. No data was created or changed.

These are working notes. Items marked _to be confirmed on Docker_ are not findings yet and do not go into the README until the Docker copy reproduces them.

## Observed on public site, to be confirmed on Docker

| #  | Observation                                                                                                                                                                                              |
| -- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Q1 | UI login with `john` and a wrong password, and with a username that does not exist, both landed on John Smith's accounts overview (2 of 2 attempts in fresh sessions). One earlier attempt showed "An internal error has occurred and has been logged." instead. The API rejects the same credentials with 400 "Invalid username and/or password". |
| Q2 | Opening `overview.htm` without being logged in, or after logout, shows "An internal error has occurred and has been logged." instead of redirecting to the login page.                                  |
| Q3 | `GET /accounts/99999999` returns 400 "Could not find account #99999999"; `GET /accounts/abc` returns 404 with an empty body.                                                                             |
| Q4 | The API returns XML unless `Accept: application/json` is sent.                                                                                                                                           |
| Q5 | The API declares no authentication; customer and account data can be read by ID without a session. Login sends the password in the URL path.                                                           |

| Q6 | Transaction filters only understand `MM-dd-yyyy`. An ISO date (`2026-11-24`) returns 200 with an empty list instead of a validation error. Month accepts both `November` and `Nov`. |
| Q7 | `GET /accounts/{id}/transactions` returns 400 "Could not find transactions for account #{id}" when there are none, while the date filters return 200 `[]` for the same situation. |

| Q8 | From the page source of `transfer.htm` (not yet exercised): the messages "The amount cannot be empty." and "Please enter a valid amount." exist in the markup, but the page script only ever hides them and posts whatever was typed to `/transfer`. To be confirmed on Docker. |

## Facts that shape the tests

- The public data is shared and changes constantly: John's account count went from 16 to 22 during the exploration, and about an hour later it was down to 2 (account 12345, which had 40 transactions, no longer existed). Public tests can only check structure and invariants, never exact counts or balances, and they pick accounts at run time.
- Transaction `date` values observed so far are midnight UTC, so date filters are compared on the UTC date.
- Account-service forms (transfer, bill pay, open account, loan) submit through JavaScript to `/parabank/services_proxy/bank/...` and show results in hidden panels. UI tests must wait for the result panel, not for a page navigation.
- The bill pay phone field gets a random `id` on every page load, so it is located by `name`.
- Transaction dates are epoch milliseconds; balances are JSON numbers. Money comparisons use `BigDecimal`.
- The admin page and the `initializeDB` / `cleanDB` / `setParameter` endpoints need no login. They are never used against the public site.
