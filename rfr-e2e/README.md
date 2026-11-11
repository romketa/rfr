# rfr-e2e

Rangiffler E2E tests: JUnit 6.1.3, Playwright Java 1.63, Allure 3, Spring `JdbcClient` for fixtures.

## Running

```bash
./gradlew :rfr-e2e:e2e                                   # UI tests only (packages test.web and test.people)
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
| `jupiter` | everything JUnit-related: `annotation` (`@WebTest`, `@User`, `@StaticUser`, `@ProbeTarget`, `@DemoTest`), `extension` (`BrowserExtension`, `UserExtension`, `UsersQueueExtension`, `DemoExtension`), `probe` — EngineTestKit tests of the extensions; their scenarios are `@ProbeTarget` nested classes (`@Nested` plus the tag `engine-kit`, which the `test` task excludes), so they run only inside the probe |
| `data` | test data and their owners: `TestUser`, `TestProfile`, `UserRequest` (builds a fixture from `@User`), `UserFixture` (interface), `SingleUserFixture` and its decorator `UserWithRelationsFixture` (one per non-zero counter, parameterised by `Relation`: `FRIEND`, `INCOME_INVITATION`, `OUTCOME_INVITATION`, which differ by `FriendshipStatus`, direction and the `TestUser.with...` method), `StaticUserFixture` over the JVM-wide `UsersQueue` of pre-created users, `UserRecords` (deletes a user from both schemas by username); `db` — `Databases`, `Jdbc` and immutable objects that own all SQL: single rows (`Row`: `AuthAccount`, `Profile`, `Friendship`), row sets (`Rows`: `Friendships` of a profile, optionally by `FriendshipStatus`), `SchemaHistory` (applied `Migration`s of a schema), `Migration` (also parsed from a `V*.sql` file with the Flyway checksum) and `SchemaDrift` between the two; UUIDs are converted with `UUID_TO_BIN`/`BIN_TO_UUID` in SQL |
| `util` | stateless static utilities: `DataUtils` — random usernames/passwords |
| `page` | Page Objects (`LandingPage`, `LoginPage`, `RegisterPage`, `MyTravelsPage`, `ProfilePage`); `component` — shared page parts (`Header`) |
| `test` | tests only: `web` — UI tests, `fake` — `SimpleTest` for checking the Allure setup and `ExtendWithPlacementsTest`, a demo of every place `@ExtendWith` can be declared (see below) |

SQL for fixtures lives in `src/test/resources/sql/<schema>/<name>.sql` and is loaded with `Jdbc.sql("auth/insert_user")`.

### Extensions

- `@WebTest` (on a class) → `BrowserExtension`: a fresh Playwright browser per test, a `Page` method parameter,
  a full-page screenshot attached to Allure when the test fails.
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
- `StandSchemaExtension.verify(root)` is called by `StandSchemaSessionListener`, a `LauncherSessionListener`
  registered through `META-INF/services/org.junit.platform.launcher.LauncherSessionListener`, when the JUnit Platform
  launcher session opens, before discovery, and compares `flyway_schema_history` of `rangiffler-auth` and
  `rangiffler-api` (version, description,
  Flyway CRC32 checksum) with the migration files of the checked-out `rfr-auth` and `rfr-api`. Switching a backend
  branch without recreating the database leaves the schema ahead of the code, which Flyway ignores by default
  (`ignoreMigrationPatterns=*:future`); the extension fails the whole run instead, naming the migrations and the action:
  check out the branch, restart the backend, or recreate the `mysql-volume`. The repository root is found by
  `settings.gradle` (`rfr.repo.root` overrides it); the probe `StandSchemaExtensionTest` points it at a temporary copy
  through the configuration parameter `rfr.migrations.root`.
- A user registered through the UI is out of reach of the fixture: `RegistrationTest` deletes it in `@AfterEach`
  with `UserRecords.delete(username)`, which also runs when the scenario fails.
- Generated usernames start with `rfr_`, so leftovers can be found with `username like 'rfr\_%'`.

Rules: `jupiter` has no SQL or locators; `data` has no JUnit; `test` has no locators or SQL; `ThreadLocal` is not used for test state — state lives in the extension Store.

## Agent

Page Objects are written by Claude Code using the `rfr-page-objects` skill; the UI is explored via Playwright MCP (`.mcp.json`).

A test is proven correct by running its Gradle task.
