# parabank-api-ui-java

Hybrid UI and REST API test framework for a demo banking app, checking that money movements stay consistent between the API and the UI.

[![E2E](https://github.com/rohi-mahajan6/parabank-api-ui-java/actions/workflows/e2e.yml/badge.svg)](https://github.com/rohi-mahajan6/parabank-api-ui-java/actions/workflows/e2e.yml)

> ⚠️ The target app is **ParaBank**, a public demo banking site by Parasoft used for practice. This project is not affiliated with Parasoft. The public site is only read. Anything that creates or moves data runs against a throwaway Docker copy in CI.

## 1. Problem statement

Verify that money movements (transfer, bill pay, loan, opening an account) are correct to the cent, and that the UI and the REST API agree about balances and transactions. Do it without touching shared public data.

## 2. Application under test

| Item         | Value                                                                                                        |
| ------------ | ------------------------------------------------------------------------------------------------------------ |
| App          | [ParaBank](https://parabank.parasoft.com) by Parasoft                                                        |
| Type         | Web UI + REST API (`/parabank/services/bank`, OpenAPI 3.0.1)                                                 |
| Credentials  | Public demo credentials, read from `.env` locally and from repository secrets in CI. Never hard-coded.       |
| Environments | `public`: parabank.parasoft.com, read-only smoke. `docker`: `parasoft/parabank` container in CI, full suite. |

## 3. Scope

**Read-only suite** (`testng-smoke.xml`, safe on the public site):

| Area         | Tests                                                                                                                     |
| ------------ | ------------------------------------------------------------------------------------------------------------------------- |
| Login API    | valid login (200 + schema), wrong password (400), unknown user (400)                                                      |
| Accounts API | account list (schema, owner, non-empty), single account matches list, missing account (400), non-numeric id (404), XML by default |
| Transactions | list (schema, owner), filter by amount, date range, month + type                                                          |
| Contract     | JSON schemas for Customer, Account, Account[], Transaction[]                                                              |
| UI           | valid login, empty credentials, logout, accounts overview structure, activity Debit filter                                |
| DockerGuard  | 5 unit tests: writes blocked on public, public host blocked even in Docker mode, unknown target blocked                   |

**Docker-only suite** (`testng-regression.xml`, adds the groups `write` and `docker`). _Written, not yet run: results will be added from the first CI run._

| Area         | Tests                                                                                                                                  |
| ------------ | -------------------------------------------------------------------------------------------------------------------------------------- |
| Open account | API: opening deposit moves exactly; hybrid: open SAVINGS in the UI, API shows type and owner                                          |
| Transfer     | API: exact move (source −X, destination +X); invalid amounts (0, 0.00, −5.00, abc), same account, non-existent account, overdraft; hybrid: UI transfer, API balances and debit/credit pair |
| Bill pay     | UI: empty submit validation, account mismatch; hybrid: UI payment, API balance and debit                                               |
| Loan         | API: approved within limits (LOAN account, down payment debited), denied for down payment above balance, denied far above limits; hybrid: UI loan approved |
| Login        | wrong password rejected (confirms or rejects finding Q1)                                                                               |

## 4. Architecture

```
├── src/main/java/io/github/rohimahajan6/parabank/
│   ├── config/        # Config (system property > env var > .env), TestEnv (public | docker)
│   ├── guard/         # DockerGuard, WriteBlockedException
│   ├── api/           # ApiClient (RestAssured, JSON by default, Allure request/response attachments)
│   │   └── model/     # Customer, Account, Transaction, Payee, LoanResponse (BigDecimal for money)
│   └── ui/
│       ├── driver/    # DriverFactory (Chrome via Selenium Manager)
│       └── pages/     # LoginPage, OverviewPage, ActivityPage, OpenAccountPage,
│                      # TransferPage, BillPayPage, RequestLoanPage
├── src/test/java/io/github/rohimahajan6/parabank/
│   ├── tests/api/     # API tests
│   ├── tests/ui/      # UI tests
│   ├── tests/hybrid/  # act in the UI, verify through the API
│   ├── tests/contract/# JSON schema checks
│   ├── tests/unit/    # DockerGuard unit tests
│   └── support/       # BaseUiTest, LiveData, Money, DockerResetListener
├── src/test/resources/schemas/
├── testng-smoke.xml       # read-only, public-safe
├── testng-regression.xml  # full suite, Docker only
├── docker-compose.yml
└── .github/workflows/e2e.yml
```

- **DockerGuard.** Every API method and page-object action that creates or changes data calls the guard first. It throws unless `TEST_ENV=docker` **and** the target is not the public ParaBank host. Writing to the public site is blocked in code, not just by convention.
- **Two suites.** `testng-smoke.xml` runs only read-only tests. `testng-regression.xml` runs everything and is used only with Docker.
- **Known starting data.** `DockerResetListener` calls `POST /initializeDB` once before the Docker suite (through the guard).
- **Data found at run time.** The public data is shared and changes constantly, so tests pick accounts through read-only API calls and skip with a reason if the data they need is not there. Write tests measure balances before and after instead of assuming starting values.
- **Money as BigDecimal.** Balances and amounts are compared with `BigDecimal`, never `double`.

## 5. Tech stack

Java 17, Maven, Selenium 4 (Selenium Manager), REST Assured, TestNG, AssertJ, Jackson, REST Assured JSON Schema Validator, Allure, dotenv-java, SLF4J, Docker (CI service container), GitHub Actions, GitHub Pages.

Surefire is kept on 3.5.x: 3.6+ runs TestNG on the JUnit Platform and no longer reads TestNG suite XML files.

## 6. Execution evidence

**Local run, 2026-11-24 15:38 IST** (Windows 11, Java 17.0.21, Maven 3.10.2, public site, read-only suite, one thread, headless Chrome):

```
mvn -B -ntp test -DsuiteXml=testng-smoke.xml -DHEADLESS=true
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

All 39 API requests recorded in the Allure attachments for that run were `GET`. An earlier run the same day had 27 tests (26 passed, 1 skipped): the skipped one was the wrong-password UI test, which has since moved to the Docker-only group.

| Evidence                 | Link                         |
| ------------------------ | ---------------------------- |
| Latest CI run            | _pending_                    |
| Live Allure report       | _pending_                    |
| Docker suite results     | _pending_                    |
| Scheduled runs (cron)    | `30 0 * * *` UTC = 06:00 IST |

## 7. Known defects / findings

Observed on the public site on 2026-11-24. Details in [docs/exploration-notes.md](docs/exploration-notes.md).

| ID | Area             | Observation                                                                                                                                                  | Status                       |
| -- | ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------- |
| Q1 | Login (UI)       | A wrong password, and an unknown username, logged in as John Smith (2 of 2 attempts in fresh sessions). The API rejects the same credentials with 400.       | To be confirmed on Docker    |
| Q6 | Transactions API | Date filters only accept `MM-dd-yyyy`. An ISO date (`2026-11-24`) returns 200 with an empty list instead of a validation error.                             | Observed on public site      |
| Q7 | Transactions API | `GET /accounts/{id}/transactions` returns 400 "Could not find transactions for account #{id}" when there are none; the date filters return 200 `[]` instead. | Observed on public site      |

## 8. Run locally

Requires Java 17 and Maven. Locally, only the public read-only smoke suite runs.

```bash
git clone https://github.com/rohi-mahajan6/parabank-api-ui-java.git
cd parabank-api-ui-java
cp .env.example .env          # then set PARABANK_USERNAME and PARABANK_PASSWORD
mvn test                      # smoke suite (default)
mvn allure:serve              # open the Allure report
```

## 9. CI/CD

`.github/workflows/e2e.yml` runs on push to `main`, on pull requests, nightly at 06:00 IST, and on demand.

- **Smoke (public site):** read-only suite against parabank.parasoft.com, `TEST_ENV=public`.
- **Full suite (Docker):** ParaBank runs as a `services:` container pinned by image digest. The job waits up to 180 s for it to start, then runs the regression suite with `TEST_ENV=docker`.
- **Allure report:** results from both jobs are tagged with their environment (so the read-only tests that run in both jobs are not merged as retries), combined into one report, uploaded as an artifact, and deployed to GitHub Pages from `main` only.

Setup on GitHub: add `PARABANK_USERNAME` and `PARABANK_PASSWORD` repository secrets, and set Pages to deploy from GitHub Actions.

## 10. Responsible use

- Public site: read-only, one thread, one scheduled run per day. No account creation, transfers, bill payments, loans, registrations or profile updates.
- Everything that writes data runs against a throwaway container.
- Only obviously fake data is entered.
- ParaBank's login API takes the password in the URL path (`GET /login/{username}/{password}`), so the public demo password appears in console logs and in the Allure request attachments. This is a known limitation of the demo API, not a secret leak: the credentials are ParaBank's published demo account.

## 11. Next steps

_Pending: after the first CI run._
