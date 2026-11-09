---
name: rfr-test-data
description: How test data is created, owned and released in rfr-e2e. Use it whenever a test needs a user, a profile, friends or invitations, whenever an attribute is added to @User or a fixture, decorator or row object in data or data/db is written or changed, and whenever someone asks for "a test user", "preconditions" or "test data", even without naming the fixtures.
---

# What test data is
Everything a test needs in rangiffler-auth and rangiffler-api before it starts: the auth account, the api profile, friends, income and outcome invitations. It is requested through `@User` on a `TestUser` parameter and nothing else; a test never inserts rows itself.

# Rules
1. **Every value is unique.** Usernames and passwords come from `DataUtils.randomUsername()` and `DataUtils.randomPassword()`, ids from the `random(...)` factories of the row objects. No fixed usernames, no reuse between tests, no lookup of "the user from the previous test": tests must survive a parallel run and a run against a dirty database.
2. **The owner is the decorator tree.** `UserRequest.fixture()` builds `SingleUserFixture` wrapped in `UserWithFriendsFixture`, `UserWithIncomeInvitationFixture` and `UserWithOutcomeInvitationsFixture`; the root goes into the `ExtensionContext.Store` before `create()` and is closed by it. Each decorator creates its related users through `Fixtures.createRelated` and releases exactly them through `TearDown`, in reverse order. Nothing else (statics, extension fields, `@AfterEach` in tests) owns fixture data; `UserRecords.delete` is only for a user registered through the UI.
3. **A new precondition is a new decorator.** Add the attribute to `@User`, the component to `UserRequest` (validated in its compact constructor, counted in `needsProfile()`), the decorator to `fixture()`, the field and `with...` method to `TestUser`, and one argument in `UserExtension.request()`. `SingleUserFixture`, the other decorators and the extension logic stay untouched.
4. **SQL lives only in the row objects of `data/db`.** `Row` (`AuthAccount`, `Profile`, `Friendship`) and `Rows` (`Friendships`) own `create`, `delete`, `exists` and `find`; statements are files in `src/test/resources/sql/<schema>/`, loaded through `Jdbc.sql`, with ids passed as strings through `UUID_TO_BIN(?)` and read through `BIN_TO_UUID`. No `Jdbc` call and no SQL string outside `data/db`, and `data/db` does not depend on `data`.
5. **The api profile is created explicitly when the test needs it.** rfr-api creates a profile lazily on the first `GetUser` with country `ru`; a bare `@User` relies on that and tests it. Any profile attribute, friend or invitation makes `UserRequest.needsProfile()` true and `SingleUserFixture` inserts the profile with `Profile.random(...).create()`, because a user without a profile cannot be found, invited or befriended. A test that searches for or relates to a user gives that user a profile attribute.
6. **A pool of pre-created users only where creation is impossible.** Everything in rangiffler-auth and rangiffler-api can be inserted, so no pool exists and none is added for speed. A pool is acceptable only for data the fixtures cannot create (an external identity provider, a third-party account); then the pool is read-only, a test takes a user through an extension with the same Store lifecycle and returns it unchanged.

# Checklist for a change
- The test declares its data only through `@User`; the data is unique, owned by a fixture in the Store and released by it.
- New SQL is a file under `sql/<schema>/` used by one row object; nothing outside `data/db` touches the database.
- `./gradlew :rfr-e2e:test --tests io.student.rangiffler.jupiter.probe.UserExtensionTest` passes and the database holds no `rfr_*` user afterwards.
