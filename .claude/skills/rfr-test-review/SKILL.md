---
name: rfr-test-review
description: Review of rfr-e2e test code for ownership of test resources. Use it for any diff in rfr-e2e/src/test/java/io/student/rangiffler/jupiter or data, for a new @User precondition or a new UserFixture decorator, and when a failing test looks like leaked data or shared state between tests, even if the user only says "look at the extension", "why is a user left in the database" or "review the fixture".
---

# What is reviewed
A resource is everything a test receives and must give back: rows in rangiffler-auth and rangiffler-api, `Page`, `BrowserContext`.
For every resource find the owner, where it is acquired, where it is kept and where it is released; a resource with no answer to one of these is the finding.

# Rules
1. **Owner is the decorator tree.** The `ExtensionContext.Store` holds only the root `UserFixture` from `UserRequest.fixture()` and closes it as `AutoCloseable`; each `UserWithRelationsFixture` (one per `Relation`: `FRIEND`, `INCOME_INVITATION`, `OUTCOME_INVITATION`) owns its `origin` and the `related` users it created. Extension fields, statics and `ThreadLocal`s never own a resource.
2. **Register before partial setup.** `UserExtension.beforeEach` calls `store.put(parameter, fixture)` before `fixture.create()`; `create()` is not atomic (`AuthAccount`, `Profile`, related users, `Friendship`), so `close()` must work after a failure at any step and be a no-op before `create()`.
3. **create and close are symmetric.** A decorator deletes exactly what it created, addressed by username or id, in reverse order (its friendship rows, related users, `delegate`) through `TearDown`, so every step runs and the first failure is thrown with the rest suppressed. Broad `DELETE` or `TRUNCATE` is a defect.
4. **New relation is a new `Relation` value, new kind of precondition is a new decorator.** Compose a decorator in `UserRequest.fixture()`; a flag in `SingleUserFixture`, a branch in `UserExtension` or a fourth copy of `UserWithRelationsFixture` is a defect. The extension knows only `UserRequest` and `UserFixture`.
5. **One @User is one parameter.** Each `@User TestUser` parameter is one fixture under its own `Parameter` key in the Store; related users live inside `TestUser` (`friends`, `incomeInvitations`, `outcomeInvitations`) and the fixture tree, never as extra parameters.
6. **Browser resources belong to BrowserExtension.** `@WebTest` registers it; `Page`, `BrowserContext` and `Playwright` are created and closed there only, never in tests, page objects or other extensions.
7. **Layers.** `jupiter` has no SQL and no locators; `data` has no JUnit API and no `Jdbc` calls outside `data/db`; `data/db` does not depend on `data`; `test` has no locators and no SQL.
8. **Every conclusion is backed.** A finding cites a probe in `jupiter/probe/UserExtensionTest` (EngineTestKit, `find()`/`exists()` from `data/db`, entries published through `TestReporter`, the failing and the unknown-country scenarios) or a concrete file and line. Without a found cause, propose no change.

# Answer format
- For every resource: owner, acquisition, storage, release.
- For every finding: file and line, the failure scenario, how to reproduce it (which probe, or which `create()` step has to fail).
- Three lists: confirmed by a run, by reading the code, not checked.
- No defects: say so and list the limits of the current design.
