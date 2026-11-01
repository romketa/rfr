## Configuration

Test configuration - `Config`: `-Drfr.<key>` or variable `RFR_<KEY>`, else value by default. Details in - `rfr-e2e/README.md`. 

## Creation Page Objects

Use skill 'page-object' (`.claude/skills/page-object/SKILL.md`).

## Project architecture

- Package structure: `rfr-e2e/README.md`, section "Architecture": `config`, `jupiter`, `data`, `api`, `ui`, `assertions`, `test`. New classes go into the existing packages.
- No comments or Javadoc in `rfr-e2e` code (Java, Gradle, SQL, properties): names and `@Step` carry the meaning.
- Tests log in only through the UI; never inject tokens into localStorage.
- Walking through a scenario with Playwright MCP is not running the test. Success is confirmed by a JUnit run through Gradle.
- Test data is created and deleted by fixtures (`@User` → `UserExtension` → `UserFixture`), without broad `DELETE`/`TRUNCATE`.
