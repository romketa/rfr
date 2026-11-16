# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Rangiffler is a QA.GURU Advanced graduation project: a travel social network (photos by country, likes, friendships,
a world map with stats). The goal is to evolve the backend into microservices and build extensive e2e/API test coverage.
Detailed implementation notes live in the root `README.md` (in Russian) and in each module's `README.md` (in English).
Module-specific rules live in each module's `CLAUDE.md`; this file keeps only what spans several modules.

Modules (Gradle multi-project, `settings.gradle`; the frontend is not part of the Gradle build):

| Module | Port | What it is | CLAUDE.md |
|---|---|---|---|
| `rfr-auth` | 9001 | Spring Authorization Server (OAuth 2.1 / OIDC), Thymeleaf login/register pages. Complete | [rfr-auth/CLAUDE.md](rfr-auth/CLAUDE.md) |
| `rfr-api` | 8081 | GraphQL API (Spring for GraphQL), OAuth2 Resource Server. Partially mocked | [rfr-api/CLAUDE.md](rfr-api/CLAUDE.md) |
| `rfr-gql-client` | 3001 | React 19 + TS + Vite + Apollo Client 4 + MUI 9. Complete, not meant to be changed | [rfr-gql-client/CLAUDE.md](rfr-gql-client/CLAUDE.md) |
| `rfr-e2e` | — | JUnit 6 + Playwright Java + Allure 3. Page Objects, `@User` fixture (JDBC), web tests for registration and login. In progress | [rfr-e2e/CLAUDE.md](rfr-e2e/CLAUDE.md) |

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
docker volume create mysql-volume   # once per machine: compose.yaml declares the volume as external
docker compose up -d             # MySQL 8.4 (root/secret, 3306)
bash ports.sh                    # kill processes on 9001/8081/3001

./gradlew :rfr-auth:bootRun      # start auth first: rfr-api fetches JWKS via issuer-uri
./gradlew :rfr-api:bootRun       # GraphiQL: http://localhost:8081/graphiql
./gradlew runAll                 # rfr-auth and rfr-api together (needs org.gradle.parallel=true from gradle.properties)
./gradlew build                  # build all Java modules
cd rfr-gql-client && npm ci && npm run dev    # frontend

./gradlew :rfr-e2e:test                                                        # e2e tests
./gradlew :rfr-e2e:e2e                                                         # UI tests only (package io.student.rangiffler.test.web)
./gradlew :rfr-e2e:test --tests 'io.student.rangiffler.test.fake.SimpleTest'   # single class/method
./gradlew :rfr-e2e:allureServe                                                 # Allure report (io.qameta.allure plugin)
```

## Architecture and key details

### Authentication (end-to-end flow)
- The frontend is a public client `client` (no secret) using Authorization Code + PKCE; the redirect URI is fixed to
  `http://localhost:3001/authorized`, so the frontend port must not change.
- `rfr-auth` puts `aud: rangiffler-api` and an `authorities` claim (`read`, `write`) into the **access token**; `rfr-api` checks both.
  The API rejects `id_token` with 401 — the frontend uses it only as `id_token_hint` on logout.
- JWT `sub` = username; this is the only link between users in `rangiffler-auth` and `rangiffler-api` (UUIDs in the two DBs are independent).
  The API profile is created lazily on the first `Query.user` (default country `ru`).
- All tokens become invalid after an `rfr-auth` restart; `rfr-api` validates JWTs locally and is unaware of revocation.

### GraphQL contract
- The schema `rfr-api/src/main/resources/graphql/query.graphqls` is the contract with the finished frontend; don't change it without changing the frontend.
- GraphQL always returns HTTP 200; errors go to `errors[].extensions.classification` (`NOT_FOUND`, `BAD_REQUEST`, `INTERNAL_ERROR`).
  The frontend shows the server message only for `BAD_REQUEST`/`NOT_FOUND`.
- Only users, countries and friendships are persisted; feed, photos, likes and stats are mocked in `rfr-api`.

### Database
One MySQL instance, two schemas created automatically (`createDatabaseIfNotExist=true`): `rangiffler-auth` and `rangiffler-api`.
Flyway migrations live in `src/main/resources/db/migration/<schema>/`, `ddl-auto: none` — schema changes go only through new `V2__...` migrations.
Images (photos, avatars, flags) are stored as data URLs in BLOBs.

### Frontend (relevant for tests)
- Each GraphQL operation is a separate hook in `rfr-gql-client/src/hooks` with a named operation (`GetUser`, `GetFeed`, `FriendshipAction`,
  `CreatePhoto`...) — convenient for intercepting by `operationName` in Playwright.
- Tokens are stored in localStorage (`access_token`, `id_token`); grab them from there for GraphiQL/curl requests.
- The Apollo cache applies mutation responses immediately, so a successful UI state doesn't prove persistence — verify with an independent read in tests.
