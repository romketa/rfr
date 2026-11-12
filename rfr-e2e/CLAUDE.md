## rfr-e2e

- No comments or Javadoc in `rfr-e2e` code (Java, Gradle, SQL, properties): names and `@Step` carry the meaning.
- New classes go into the existing packages.
- Tests log in only through the UI; never inject tokens into localStorage.
- Walking through a scenario with Playwright MCP is not running the test. Success is confirmed by a JUnit run through Gradle.
- Test data is created and deleted by fixtures (`@User`), never by broad `DELETE`/`TRUNCATE`. A user created through the UI is
  deleted by username in `@AfterEach` (`UserRecords.delete`).
- A JUnit extension that provides test data follows the `UserExtension` shape:
  - `beforeEach` finds the annotated parameters of the test method, puts an `AutoCloseable` fixture into the store
    (keyed by the parameter) and only then creates the data, so the store cleans up even when creation fails;
  - `supportsParameter` only checks that the parameter has the annotation and the expected type, using the same check
    as `beforeEach`;
  - `resolveParameter` only takes the fixture from the store and returns its data; it never creates anything.
- An implementation of `SuiteExtension` is registered only globally: its class name goes into
  `src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension` (autodetection is on in
  `junit-platform.properties`). Never register it with `@ExtendWith` or `@RegisterExtension`, and never list the
  `SuiteExtension` interface itself: a partial registration breaks the once-per-run semantics of `beforeSuite`/`afterSuite`,
  and the interface has no constructor for `ServiceLoader`, so no test in the module starts.
- The order of the lines in that services file is the order in which the extensions run, and Jupiter stops calling
  `BeforeAllCallback`s of a class at the first one that throws. `AllureEnvironmentExtension` stays first, so its
  `SuiteRun` exists and `afterSuite` writes `environment.properties` even when a later `beforeSuite` fails;
  `StandReadinessExtension` stays above `StandSchemaExtension`, because the schema check needs MySQL to be up.
