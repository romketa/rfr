## rfr-api (GraphQL)

Details in `rfr-api/README.md`.

## Run

```bash
./gradlew :rfr-api:bootRun       # start rfr-auth first: rfr-api fetches JWKS via issuer-uri. GraphiQL: http://localhost:8081/graphiql
./gradlew :rfr-api:generateJava  # DGS codegen (runs automatically on compile)
```
For running, rfr-auth must be started first

There are no unit tests yet. `./gradlew build` does not validate HQL in `@Query` — query errors surface only at application
startup against a live DB.

## Schema and generated types

- The schema `src/main/resources/graphql/query.graphqls` is the contract with the finished frontend; don't change it without changing the frontend.
- GraphQL type models are **generated** by DGS Codegen into `io.student.rangiffler.model.types` (`build/generated/sources/dgs-codegen`) —
  don't write them by hand. `UserConnection`/`PhotoConnection` are not declared in the schema (IDEA highlights them red — expected):
  Spring GraphQL synthesizes them, and `typeMapping` in `build.gradle` maps them to `Page<...>`; `Date` → `LocalDate`.
  Paginated data fetchers return `Page<T>`/`Slice<T>`.

## Controllers

- Controller arguments: `@Argument("name") @Nullable Integer` (not `int`; always name the argument explicitly; `-parameters` is enabled in the root `build.gradle`).
- Nested fields are resolved via `@SchemaMapping(typeName = ..., field = ...)` (`User.friends`, `Feed.photos`, `Photo.likes`, etc.) — never call these directly.
- Security: `POST /graphql` passes the filter chain; access is enforced by class-level `@PreAuthorize`:
  query controllers — `hasAuthority('read')`, mutation controllers — `hasAuthority('write')`. New controllers should follow this split.
- JWT: `RangifflerApiConfiguration` checks the `rangiffler-api` audience and maps the `authorities` claim to `GrantedAuthority` without a prefix.
- Errors: GraphQL always returns HTTP 200; errors go to `errors[].extensions.classification` via `GraphQlExceptionResolver`:
  `ResourceNotFoundException` → `NOT_FOUND`, `FriendshipActionException`/`IllegalArgumentException` → `BAD_REQUEST`, everything else → `INTERNAL_ERROR` with no details.

## Data

### Real (persisted in `rangiffler-api`)

`UserQueryController`, `UserMutationController`, `CountryQueryController`:

- `Query.user` — the current user's profile; created on the first call with country `ru`.
- `Query.users` — people list with search and friendship status.
- `User.friends`, `User.incomeInvitations`, `User.outcomeInvitations` — paginated friends and invitations.
- `Mutation.user` — saves first name, surname, avatar and country.
- `Mutation.friendship` — send, accept, decline an invitation, remove a friend.
- `Query.countries` — the country dictionary from the migration.

### Mocked (nothing is persisted)

`FeedMockQueryController`, `PhotoMockMutationController`, `FeedMockData` + `resources/mock/*.json`:

- `Query.feed`, `Feed.photos` — photos from `query_feed.json` / `query_feed_with_friends.json`; country filter and pagination
  work on top of these files.
- `Feed.stat` — per-country stats computed from the same files.
- `Photo.likes` — likes from the files.
- `Mutation.photo` (create/edit) — validates country and `src`, returns the built photo and stores nothing.
- `Mutation.photo` with `like` — returns the photo with likes +1; the JSON is re-read on every call, so the like is lost.
- `Mutation.deletePhoto` — always returns `true` and deletes nothing.

Tables `photo`, `like`, `photo_like`, `statistic` already exist in the migration but are unused. Until they are implemented,
e2e tests can verify persistence only for profiles, people and friendships.

**Never replace the mocks with a real implementation on your own.** Do not make the mock classes or `resources/mock/*.json`
persist data, and do not start using the `photo`/`like`/`photo_like`/`statistic` tables, unless the user explicitly asks for
it. If a task (for example, a failing e2e test on photos or likes) seems to need real data, stop and ask.

### Implementation notes

- Friendship: one `friendship` row per pair (`requester`/`addressee`, `PENDING`/`ACCEPTED`); unordered-pair uniqueness is enforced
  by `least/greatest` generated columns. `FriendshipEntity` is its own aggregate with `@Version` and transition rules inside the entity.
  Concurrent conflicts are caught via `saveAndFlush`/explicit `flush()` and turned into business errors.
- People lists are a single paginated JPQL query with `select new UserWithStatus(...)` (friendship status + country, no N+1),
  each in two variants — with and without `searchQuery`; the service picks which one to call.
- Spring Boot 4 → Jackson 3 (`tools.jackson.databind.*`, not `com.fasterxml.jackson.databind.*`). Use `java.time` for dates.
