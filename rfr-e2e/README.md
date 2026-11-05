# rfr-e2e

Rangiffler E2E tests: JUnit 6.1.3, Playwright Java 1.63, Allure 3, Spring `JdbcClient` for fixtures.

## Running

```bash
./gradlew :rfr-e2e:e2e                                   # UI tests only (package test.web)
./gradlew :rfr-e2e:test --tests '*RegistrationTest'
./gradlew :rfr-e2e:test --tests '*LoginTest'
./gradlew :rfr-e2e:allureReport                           # rfr-e2e/build/reports/allure-report/allureReport/index.html
./gradlew :rfr-e2e:allureServe                            # build the report and open it in a browser
```

Playwright downloads its browsers (Chromium, Firefox, WebKit) itself on the first run; the tests use Chromium only.

Environment: MySQL `:3306`, `rfr-auth :9001`, `rfr-api :8081`, frontend `:3001` (see the root README).

## Configuration

`Config` reads `-Drfr.<key>` → `RFR_<KEY>` (upper case, dots replaced with underscores, e.g. `RFR_FRONT_URL`) → default value.
Gradle passes all `-Drfr.*` properties to the test JVM.

| Key | Default |
|---|---|
| `rfr.front.url` | `http://localhost:3001` |
| `rfr.auth.url` | `http://localhost:9001` |
| `rfr.headless` | `true` |
| `rfr.browser.locale` | `en-US` (pinned: `rfr-auth` localizes its messages by `Accept-Language`) |
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
| `data` | test data and their owners: `TestUser`, `UserFixture`, `UserRecords` (deletes a user from both schemas by username); `db` — `Databases`, `Jdbc` |
| `util` | stateless static utilities: `DataUtils` — random usernames/passwords |
| `page` | Page Objects (`LandingPage`, `LoginPage`, `RegisterPage`, `MyTravelsPage`, `ProfilePage`); `component` — shared page parts (`Header`) |
| `test` | tests only: `web` — UI tests, `fake` — `SimpleTest` for checking the Allure setup |

SQL for fixtures lives in `src/test/resources/sql/<schema>/<name>.sql` and is loaded with `Jdbc.sql("auth/insert_user")`.

### Extensions

- `@WebTest` (on a class) → `BrowserExtension`: a fresh Playwright browser per test, a `Page` method parameter,
  a full-page screenshot attached to Allure when the test fails.
- `@User` (on a `TestUser` parameter) → `UserExtension` → `UserFixture`: every annotated parameter gets its own random user,
  inserted into `rangiffler-auth` before the test and deleted from both schemas afterwards, including the profile
  `rfr-api` creates on the first login.
- A user registered through the UI is out of reach of the fixture: `RegistrationTest` deletes it in `@AfterEach`
  with `UserRecords.delete(username)`, which also runs when the scenario fails.
- Generated usernames start with `rfr_`, so leftovers can be found with `username like 'rfr\_%'`.

Rules: `jupiter` has no SQL or locators; `data` has no JUnit; `test` has no locators or SQL; `ThreadLocal` is not used for test state — state lives in the extension Store.

## Agent

Page Objects are written by Claude Code using the `rfr-page-objects` skill; the UI is explored via Playwright MCP (`.mcp.json`).

A test is proven correct by running its Gradle task.
