## rfr-e2e

- No comments or Javadoc in `rfr-e2e` code (Java, Gradle, SQL, properties): names and `@Step` carry the meaning.
- New classes go into the existing packages.
- Tests log in only through the UI; never inject tokens into localStorage.
- Walking through a scenario with Playwright MCP is not running the test. Success is confirmed by a JUnit run through Gradle.
- Test data is created and deleted by fixtures (`@User`), never by broad `DELETE`/`TRUNCATE`. A user created through the UI is
  deleted by username in `@AfterEach` (`UserRecords.delete`).
