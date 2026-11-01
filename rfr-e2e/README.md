# rfr-e2e

Rangiffler E2E tests: JUnit 6.1.3, Playwright Java 1.63, Allure 3, Spring `JdbcClient` for fixtures.

## Running

```bash
./gradlew :rfr-e2e:test --tests '*RegistrationTest' --rerun-tasks
./gradlew :rfr-e2e:test --tests '*LoginTest' --rerun-tasks
./gradlew :rfr-e2e:allureReport                           # rfr-e2e/build/reports/allure-report/allureReport/index.html
./gradlew :rfr-e2e:allureServe                            # build the report and open it in a browser
```

Playwright downloads Chromium itself on the first run.

Environment: MySQL `:3306`, `rfr-auth :9001`, `rfr-api :8081`, frontend `:3001` (see the root README).

## Configuration

`Config` reads `-Drfr.<key>` → `RFR_<KEY>` (upper case, dots replaced with underscores, e.g. `RFR_FRONT_URL`) → default value.
Gradle passes all `-Drfr.*` properties to the test JVM.

| Key | Default |
|---|---|
| `rfr.front.url` | `http://localhost:3001` |
| `rfr.auth.url` | `http://localhost:9001` |
| `rfr.headless` | `true` |
| `rfr.timeout.ms` | `10000` (actions, navigation and Playwright assertions) |
| `rfr.auth.db.url` | `jdbc:mysql://localhost:3306/rangiffler-auth?serverTimezone=UTC` |
| `rfr.api.db.url` | `jdbc:mysql://localhost:3306/rangiffler-api?serverTimezone=UTC` |
| `rfr.db.user` / `rfr.db.password` | `root` / `secret` |

The browser is always Chromium. Parallel execution is configured in `src/test/resources/junit-platform.properties` and is off by default.

## Architecture

A single source set `src/test`, package `io.student.rangiffler`:

| Package | Contents |
|---|---|
| `config` | `Config` — the single point for reading settings |
| `jupiter` | everything JUnit-related: `annotation` (`@WebTest`, `@User`), `extension` (`BrowserExtension`, `UserExtension`) |
| `data` | test data and their owners: `TestUser`, `UserFixture`; `db` — `Databases`, `Jdbc` |
| `util` | stateless static utilities: `DataUtils` — random usernames/passwords |
| `page` | Page Objects (`LandingPage`, `LoginPage`, `RegisterPage`, `MyTravelsPage`); `component` — shared page parts (`Header`) |
| `test` | tests only: `web` — UI tests, `fake` — `SimpleTest` for checking the Allure setup |

SQL for fixtures lives in `src/test/resources/sql/<schema>/<name>.sql` and is loaded with `Jdbc.sql("auth/insert_user")`.

### Extensions

- `@WebTest` (on a class) → `BrowserExtension`: a fresh Playwright browser per test, a `Page` method parameter,
  a full-page screenshot attached to Allure when the test fails.
- `@User` (on a method) → `UserExtension` → `UserFixture`: inserts a random user into `rangiffler-auth` before the test,
  passes it as a `TestUser` method parameter, and afterwards deletes it from both schemas, including the profile
  `rfr-api` creates on the first login.

Rules: `jupiter` has no SQL or locators; `data` has no JUnit; `test` has no locators or SQL; `ThreadLocal` is not used for test state — state lives in the extension Store.

## Agent

Page Objects are written by Claude Code using the `page-object` skill; the UI is explored via Playwright MCP (`.mcp.json`).

A test is proven correct by running its Gradle task.
