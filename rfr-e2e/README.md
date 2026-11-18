# rfr-e2e

Rangiffler E2E tests: JUnit 6.1.3, Playwright Java 1.63, Allure 3, Spring `JdbcClient` for fixtures.

## Running

```bash
./gradlew :rfr-e2e:e2e                                   # UI tests only (packages test.web, test.people and test.travels)
./gradlew :rfr-e2e:test --tests '*RegistrationTest'
./gradlew :rfr-e2e:test --tests '*LoginTest'
./gradlew :rfr-e2e:allureReport                           # rfr-e2e/build/reports/allure-report/allureReport/index.html
./gradlew :rfr-e2e:allureServe                            # build the report and open it in a browser
./gradlew :rfr-e2e:showTrace -Ptrace=build/traces/<class>/<method>/trace.zip   # open the trace of a failed test in TraceViewer
```

Playwright downloads its browsers (Chromium, Firefox, WebKit) itself on the first run; `rfr.browser` picks the one the tests use.

Environment: MySQL `:3306`, `rfr-auth :9001`, `rfr-api :8081`, frontend `:3001` (see the root README).

## Configuration

`Config` reads `-Drfr.<key>` → `RFR_<KEY>` (upper case, dots replaced with underscores, e.g. `RFR_FRONT_URL`) → default value.
Gradle passes all `-Drfr.*` properties to the test JVM.

| Key | Default |
|---|---|
| `rfr.front.url` | `http://localhost:3001` |
| `rfr.auth.url` | `http://localhost:9001` |
| `rfr.api.url` | `http://localhost:8081` |
| `rfr.stand.timeout` | `180` (seconds the UI tests wait for MySQL, `rfr-auth`, `rfr-api` and the frontend to answer) |
| `rfr.stand.check` | `true` (`false` skips the wait for the stand) |
| `rfr.schema.check` | `false` (the `e2e` task sets `true`: compare the database with the checked-out migrations) |
| `rfr.commit` | `unknown` (Gradle passes the short hash of `HEAD`; shown in the Allure environment) |
| `rfr.browser` | `chromium` (`firefox` and `webkit` are the other Playwright names) |
| `rfr.headless` | `true` |
| `rfr.browser.locale` | `en-US` (pinned: `rfr-auth` localizes its messages by `Accept-Language`) |
| `rfr.viewport.width` / `rfr.viewport.height` (also `rfr.viewportWidth` / `rfr.viewportHeight`, which win when both are set) | `1920` / `1080` (viewport of every browser context) |
| `rfr.timeout.ms` | `5000` (actions such as click and fill, and Playwright assertions) |
| `rfr.navigation.timeout.ms` | `30000` (navigation: Playwright's own default, kept explicit because `setDefaultTimeout` would otherwise cover navigation too) |
| `rfr.auth.db.url` | `jdbc:mysql://localhost:3306/rangiffler-auth?serverTimezone=UTC` |
| `rfr.api.db.url` | `jdbc:mysql://localhost:3306/rangiffler-api?serverTimezone=UTC` |
| `rfr.db.user` / `rfr.db.password` | `root` / `secret` |

The browser is always Chromium. Parallel execution and extension autodetection (`ServiceLoader`, see `DemoExtension` below) are configured in `src/test/resources/junit-platform.properties`; parallel execution is off by default.

## Architecture

A single source set `src/test`, package `io.student.rangiffler`:

| Package | Contents |
|---|---|
| `config` | `Config` — the single point for reading settings |
| `jupiter` | everything JUnit-related: `annotation` (`@WebTest`, `@User`, `@StaticUser`, `@ProbeTarget`, `@DemoTest`), `extension` (`BrowserOptions`, `BrowserExtension`, `UserExtension`, `UsersQueueExtension`, `AllureEnvironmentExtension`, `StandReadinessExtension`, `StandSchemaExtension`, `DemoExtension`), `probe` — EngineTestKit tests of the extensions; their scenarios are `@ProbeTarget` nested classes (`@Nested` plus the tag `engine-kit`, which the `test` task excludes), so they run only inside the probe |
| `data` | test data and their owners: `TestUser`, `TestProfile`, `UserRequest` (builds a fixture from `@User`), `UserFixture` (interface), `SingleUserFixture` and its decorator `UserWithRelationsFixture` (one per non-zero counter, parameterised by `Relation`: `FRIEND`, `INCOME_INVITATION`, `OUTCOME_INVITATION`, which differ by `FriendshipStatus`, direction and the `TestUser.with...` method), `StaticUserFixture` over the JVM-wide `UsersQueue` of pre-created users, `UserRecords` (deletes a user from both schemas by username); `db` — `Databases`, `Jdbc` and immutable objects that own all SQL: single rows (`Row`: `AuthAccount`, `Profile`, `Friendship`), row sets (`Rows`: `Friendships` of a profile, optionally by `FriendshipStatus`), `SchemaHistory` (applied `Migration`s of a schema), `Migration` (also parsed from a `V*.sql` file with the Flyway checksum) and `SchemaDrift` between the two; UUIDs are converted with `UUID_TO_BIN`/`BIN_TO_UUID` in SQL |
| `util` | stateless static utilities: `DataUtils` — random usernames/passwords |
| `page` | Page Objects (`LandingPage`, `LoginPage`, `RegisterPage`, `MyTravelsPage`, `ProfilePage`); `component` — shared page parts (`Header`) |
| `test` | tests only: `web` — UI tests, `fake` — `SimpleTest` for checking the Allure setup and `ExtendWithPlacementsTest`, a demo of every place `@ExtendWith` can be declared (see below) |

SQL for fixtures lives in `src/test/resources/sql/<schema>/<name>.sql` and is loaded with `Jdbc.sql("auth/insert_user")`.

### Extensions

- `@WebTest` (on a class) → `@UsePlaywright(BrowserOptions.class)` plus `BrowserExtension`. Playwright's own JUnit
  extensions create the browser (one per thread, closed in `afterAll`) and a fresh `BrowserContext` and `Page` per test,
  and resolve the `Page` method parameter. `BrowserOptions` (Playwright's `OptionsFactory`) pins the settings: explicit
  headless mode (`rfr.headless`), a fixed viewport (`rfr.viewport.width` / `rfr.viewport.height`), the locale and
  `data-test-id` as the attribute behind `getByTestId`. `BrowserExtension` adds what Playwright does not: it sets the
  action timeout (`rfr.timeout.ms`) and the navigation timeout (`rfr.navigation.timeout.ms`) on the context (the
  assertion timeout is set once in a static block, outside any test, so the Allure aspect does not report it as a step)
  and attaches a full-page screenshot to Allure in `afterEach` when the test fails. The context is closed by Playwright
  from `TestWatcher`, which runs after the Store has deleted the `@User` fixtures; the page is idle by then and sends
  no requests, so the deleted profile is not recreated.
- `@WebTest` also registers `PagesExtension`: in `beforeEach` it builds one Guice injector per test around the
  Playwright `Page` and resolves page object parameters (`LandingPage landingPage`) from it, so every page and
  component exists once per test and nothing calls `new` on them.
- `@WebTest` also registers `TraceExtension`: it starts Playwright tracing of the context (screenshots, snapshots,
  sources) in `beforeEach`; in `afterEach` a failed test gets
  `build/traces/<test class>/<test method>/trace.zip`, attached to Allure as `trace.zip`, and a passed test drops
  its trace. Open the file with `./gradlew :rfr-e2e:showTrace -Ptrace=<path>`.
- `@User` (on a `TestUser` parameter) → `UserExtension` → `UserFixture`: every annotated parameter gets its own random user,
  inserted into `rangiffler-auth` before the test and deleted from both schemas afterwards, including the profile
  `rfr-api` creates on the first login.
- `@StaticUser` (on a `TestUser` parameter) → `UsersQueueExtension` → `StaticUserFixture`: a model of a stand where users
  cannot be created on the fly. `create()` takes a user from the thread-safe `UsersQueue` of three pre-created pairs
  (any username/password, they need not exist in the stand) and fails with a clear message after a 30 s wait when the
  queue is empty; `close()` returns only the taken user and deletes nothing. The fixture goes into the Store before
  `create()`, so the user comes back even after a failed test.
- `DemoExtension` is a teaching extension with no test data: it implements every `*Callback` of the test lifecycle
  (`BeforeAll`, `TestInstancePreConstruct`, `BeforeEach`, `BeforeTestExecution`, `AfterTestExecution`, `AfterEach`,
  `TestInstancePreDestroy`, `AfterAll`) and a `ParameterResolver` for `DemoExtension.DemoValue`. It numbers its
  instances and prints every call to stdout as a tree: a header line per `ExtensionContext` (outer class, nested class,
  test method) the first time it is seen, indented by its depth, and under it the callbacks as `DemoExtension#<n> <callback>`,
  with `new DemoExtension#<n>` printed before the first call of an instance; each test logs `test body runs with` and
  the `DemoValue`s it received at the same depth, and inside a test the same lines go to Allure as steps. `ExtendWithPlacementsTest` registers it once per `@Nested` scenario, in a different place each time: test
  class, superclass, interface, composed annotation `@DemoTest`, test method, static and instance field, constructor
  parameter, test method parameter, lifecycle method parameter. Every scenario has two tests, so the tree shows whether
  one instance serves the whole class (every placement except the test method and its parameter, which get a new
  instance per test and no `beforeAll`/`afterAll`). One scenario per placement is deliberate: the registry ignores an extension
  type that is already registered higher in the hierarchy, so stacking the same extension on a class and its methods
  registers it only once. Run with `./gradlew :rfr-e2e:test --tests '*ExtendWithPlacementsTest' -i` to see the trace.
- `AllureEnvironmentExtension` (a `SuiteExtension`, registered globally through `META-INF/services`) writes
  `environment.properties` into the Allure results directory (`allure.results.directory`, by default
  `rfr-e2e/build/allure-results`) in `afterSuite`: `frontUrl`, `authUrl`, `apiUrl`, `browser`, `headless`, `commit`
  (the short hash Gradle passes as `rfr.commit`, the tests never run git) and the current Flyway version of each
  schema (`authSchemaVersion`, `apiSchemaVersion`, `unavailable` when MySQL does not answer). It is the first line of
  the services file: Jupiter stops at the first `BeforeAllCallback` that throws, so only an extension registered
  before the failing one gets its `SuiteRun` and therefore its `afterSuite`. The probe `AllureEnvironmentExtensionTest`
  points the directory at a temporary one through the configuration parameter `allure.results.directory`.
- `StandReadinessExtension` (a `SuiteExtension`, registered globally through `META-INF/services`) waits for the stand
  before the first class tagged `ui` (`@WebTest` carries the tag), so both the `e2e` task and a single UI test started
  from the IDE wait instead of failing on a Playwright timeout; classes without the tag never touch the stand, and
  `rfr.stand.check=false` switches the wait off. It polls the same signs as `scripts/wait-for-stand.sh`: MySQL accepts
  a TCP connection, `rfr-auth` answers `GET /.well-known/openid-configuration` with a status below 400, `rfr-api`
  answers `POST /graphql` with any status (401 without a token is an answer), the frontend answers `GET /` with a
  status below 400. Every request is bounded by 2 s and by what is left of `rfr.stand.timeout`, so the wait never
  outlasts the timeout and nothing is asked after it; afterwards the error names the services that never answered
  and the commands that start them (`docker compose up -d --wait`, `./gradlew runAll`, `npm run dev`). The probe
  `StandReadinessExtensionTest` points the four addresses at fake servers through `rfr.stand.mysql`, `rfr.stand.auth`,
  `rfr.stand.api` and `rfr.stand.front`. It is listed before `StandSchemaExtension` in the services file because the
  schema check needs MySQL: Jupiter registers autodetected extensions in `ServiceLoader` order, which on the class path
  is the order of the lines in the file, and `BeforeAllCallback`s run in registration order.
- `StandSchemaExtension` (a `SuiteExtension`, registered globally through `META-INF/services`) is off unless
  `rfr.schema.check=true`, which only the `e2e` task sets, so `:rfr-e2e:test` does not need MySQL for it. When on, it
  runs once before every test of a run and compares `flyway_schema_history` of `rangiffler-auth` and `rangiffler-api` (version, description,
  Flyway CRC32 checksum) with the migration files of the checked-out `rfr-auth` and `rfr-api`. Switching a backend
  branch without recreating the database leaves the schema ahead of the code, which Flyway ignores by default
  (`ignoreMigrationPatterns=*:future`); the extension fails the whole run instead, naming the migrations and the action:
  check out the branch, restart the backend, or recreate the `mysql-volume`. The repository root is found by
  `settings.gradle` (`rfr.repo.root` overrides it); the probe `StandSchemaExtensionTest` points it at a temporary copy
  through the configuration parameter `rfr.migrations.root`.
- A user registered through the UI is out of reach of the fixture: `RegistrationTest` deletes it in `@AfterEach`
  with `UserRecords.delete(username)`, which also runs when the scenario fails.
- Generated usernames start with `rfr_`, so leftovers can be found with `username like 'rfr\_%'`.

Rules: `jupiter` has no SQL or locators; `data` has no JUnit; `test` has no locators or SQL; the module's own extensions keep no test state in `ThreadLocal` — it lives in the extension Store (Playwright's bundled JUnit extensions use `ThreadLocal` internally; that is theirs to manage).

## Agent

Page Objects are written by Claude Code using the `rfr-page-objects` skill; the UI is explored via Playwright MCP (`.mcp.json`).

A test is proven correct by running its Gradle task.
