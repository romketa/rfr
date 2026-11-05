# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Rangiffler is a QA.GURU Advanced graduation project: a travel social network (photos by country, likes, friendships,
a world map with stats). The goal is to evolve the backend into microservices and build extensive e2e/API test coverage.
Detailed implementation notes live in the root `README.md` and in each module's `README.md` (all in Russian).

Modules (Gradle multi-project, `settings.gradle`; the frontend is not part of the Gradle build):

| Module | Port | What it is |
|---|---|---|
| `rfr-auth` | 9001 | Spring Authorization Server (OAuth 2.1 / OIDC), Thymeleaf login/register pages. Complete |
| `rfr-api` | 8081 | GraphQL API (Spring for GraphQL), OAuth2 Resource Server. Partially mocked |
| `rfr-gql-client` | 3001 | React 19 + TS + Vite + Apollo Client 4 + MUI 9. Complete, not meant to be changed |
| `rfr-e2e` | — | JUnit 6 + Playwright Java + Allure 3. Skeleton only (`SimpleTest`) |

Stack: Java 25 (Gradle toolchain), Gradle 9.7.1 (wrapper), Spring Boot 4.1.1 (BOM is applied in the root `build.gradle`
to `rfr-api` and `rfr-auth` only), MySQL 8.4, Flyway, Lombok.

## Workflow

- Work only in the branch that is checked out when the request arrives. Never inspect other branches (`git log`, `git diff`,
  `git show` against them), never check out or switch branches, and never use their state as context. If a task seems to
  need another branch, stop and ask.
- Before writing any implementation, propose the options first: list the possible approaches with their trade-offs and wait
  for the user to pick one. Do not start coding right away, even when one approach looks obvious.

## Commands

```bash
bash localenv.sh                 # MySQL 8.4 in Docker (root/secret, 3306). WARNING: stops and removes ALL docker containers
bash ports.sh                    # kill processes on 9001/8081/3001

./gradlew :rfr-auth:bootRun      # start auth first: rfr-api fetches JWKS via issuer-uri
./gradlew :rfr-api:bootRun       # GraphiQL: http://localhost:8081/graphiql
./gradlew build                  # build all Java modules
./gradlew :rfr-api:generateJava  # DGS codegen (runs automatically on compile)

./gradlew :rfr-e2e:test                                                        # e2e tests
./gradlew :rfr-e2e:test --tests 'io.student.rangiffler.test.fake.SimpleTest'   # single class/method
./gradlew :rfr-e2e:allureServe                                                 # Allure report (io.qameta.allure plugin)

cd rfr-gql-client && npm ci && npm run dev    # frontend; npm run build (tsc + vite), npm run lint (--max-warnings 0)
```

There are no unit tests in `rfr-api`/`rfr-auth` yet. `./gradlew build` does not validate HQL in `@Query` — query errors
surface only at application startup against a live DB.

## Architecture and key details

### Authentication (end-to-end flow)
- The frontend is a public client `client` (no secret) using Authorization Code + PKCE. The client is registered via properties in
  `rfr-auth/src/main/resources/application.yaml`; the redirect URI is fixed to `http://localhost:3001/authorized`, so the frontend port must not change.
- `AccessTokenCustomizer` adds `aud: rangiffler-api` and an `authorities` claim (`read`, `write` from the `authority` table) to the **access token**.
  `rfr-api` checks the audience and maps `authorities` to `GrantedAuthority` without a prefix (`RangifflerApiConfiguration`).
  The API rejects `id_token` with 401 — the frontend uses it only as `id_token_hint` on logout.
- JWT `sub` = username; this is the only link between users in `rangiffler-auth` and `rangiffler-api` (UUIDs in the two DBs are independent).
  The API profile is created lazily on the first `Query.user` (default country `ru`).
- The RSA signing key is generated on every `rfr-auth` start and authorizations are in-memory → all tokens become invalid after an auth restart.
- Access token TTL is set in `application.yaml` (`access-token-time-to-live`); some READMEs say "10 minutes" — trust the yaml.
- Token revocation (`/oauth2/revoke`) for the public client works via `PublicClientRevocationAuthentication{Converter,Provider}`;
  `rfr-api` validates JWTs locally and is unaware of revocation.

### rfr-api (GraphQL)
- The schema `rfr-api/src/main/resources/graphql/query.graphqls` is the contract with the finished frontend; don't change it without changing the frontend.
- GraphQL type models are **generated** by DGS Codegen into `io.student.rangiffler.model.types` (`build/generated/sources/dgs-codegen`) —
  don't write them by hand. `UserConnection`/`PhotoConnection` are not declared in the schema (IDEA highlights them red — expected):
  Spring GraphQL synthesizes them, and `typeMapping` in `rfr-api/build.gradle` maps them to `Page<...>`; `Date` → `LocalDate`.
  Paginated data fetchers return `Page<T>`/`Slice<T>`.
- Controller arguments: `@Argument("name") @Nullable Integer` (not `int`; always name the argument explicitly; `-parameters` is enabled in the root `build.gradle`).
- Nested fields are resolved via `@SchemaMapping(typeName = ..., field = ...)` (`User.friends`, `Feed.photos`, `Photo.likes`, etc.) — never call these directly.
- Security: `POST /graphql` passes the filter chain; access is enforced by class-level `@PreAuthorize`:
  query controllers — `hasAuthority('read')`, mutation controllers — `hasAuthority('write')`. New controllers should follow this split.
- Errors: GraphQL always returns HTTP 200; errors go to `errors[].extensions.classification` via `GraphQlExceptionResolver`:
  `ResourceNotFoundException` → `NOT_FOUND`, `FriendshipActionException`/`IllegalArgumentException` → `BAD_REQUEST`, everything else → `INTERNAL_ERROR` with no details.
  The frontend (`src/api/graphqlError.ts`) shows the server message only for `BAD_REQUEST`/`NOT_FOUND`.
- Backed by the DB: users, countries, friendships. **Mocked** (`FeedMockQueryController`, `PhotoMockMutationController`, `FeedMockData`
  + `resources/mock/*.json`): feed, photos, likes, stats — nothing is persisted. Tables `photo`, `like`, `photo_like`, `statistic`
  already exist in the migration but are unused — implementing them is the main remaining task.
- Friendship: one `friendship` row per pair (`requester`/`addressee`, `PENDING`/`ACCEPTED`); unordered-pair uniqueness is enforced
  by `least/greatest` generated columns. `FriendshipEntity` is its own aggregate with `@Version` and transition rules inside the entity.
  Concurrent conflicts are caught via `saveAndFlush`/explicit `flush()` and turned into business errors.
- People lists are a single paginated JPQL query with `select new UserWithStatus(...)` (friendship status + country, no N+1),
  each in two variants — with and without `searchQuery`; the service picks which one to call.
- Spring Boot 4 → Jackson 3 (`tools.jackson.databind.*`, not `com.fasterxml.jackson.databind.*`). Use `java.time` for dates.

### Database
One MySQL instance, two schemas created automatically (`createDatabaseIfNotExist=true`): `rangiffler-auth` and `rangiffler-api`.
Flyway migrations live in `src/main/resources/db/migration/<schema>/`, `ddl-auto: none` — schema changes go only through new `V2__...` migrations.
Images (photos, avatars, flags) are stored as data URLs in BLOBs.

### Frontend (relevant for tests)
- Each GraphQL operation is a separate hook in `rfr-gql-client/src/hooks` with a named operation (`GetUser`, `GetFeed`, `FriendshipAction`,
  `CreatePhoto`...) — convenient for intercepting by `operationName` in Playwright.
- Tokens are stored in localStorage (`access_token`, `id_token`); grab them from there for GraphiQL/curl requests.
- The Apollo cache applies mutation responses immediately, so a successful UI state doesn't prove persistence — verify with an independent read in tests.
