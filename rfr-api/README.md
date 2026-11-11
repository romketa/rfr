# RFR-API - GraphQL Backend

## Description

`rfr-api` is the backend of the Rangiffler project with a GraphQL API, running as an OAuth 2.0 Resource Server.
Users, countries and friendships are backed by a real database; photos, likes, the feed and statistics are
still served by a mock layer from JSON files and **persist nothing**.

## Technology stack

- **Java 25**, Gradle 9.7.1 (wrapper in the project root)
- **Spring Boot 4.1.1** (Jackson 3)
- **Spring for GraphQL** + **DGS Codegen** - the Java model is generated from the schema
- **Spring Security 7** - OAuth 2.0 Resource Server (JWT), `@PreAuthorize` on controllers
- **Spring Data JPA / Hibernate** - database access
- **Flyway** - migrations
- **MySQL 8.4**, schema `rangiffler-api`

## Module structure

```
rfr-api/
├── build.gradle                                   # Spring Boot + DGS codegen (generateJava)
└── src/main/
    ├── java/io/student/rangiffler/
    │   ├── RangifflerApiApplication.java          # Entry point
    │   ├── config/
    │   │   ├── RangifflerApiConfiguration.java    # Security: resource server, audience, authorities, CORS
    │   │   └── GraphQlExceptionResolver.java      # Exceptions -> GraphQL errors (BAD_REQUEST / NOT_FOUND)
    │   ├── controller/
    │   │   ├── CountryQueryController.java        # Query.countries
    │   │   ├── UserQueryController.java           # Query.user, Query.users, User.friends/...Invitations
    │   │   ├── UserMutationController.java        # Mutation.user, Mutation.friendship
    │   │   ├── FeedMockQueryController.java       # Query.feed, Feed.photos, Feed.stat, Photo.likes (mock)
    │   │   └── PhotoMockMutationController.java   # Mutation.photo, Mutation.deletePhoto (mock)
    │   ├── data/
    │   │   ├── entity/                            # CountryEntity, UserEntity, FriendshipEntity, FriendshipStatus
    │   │   ├── projection/UserWithStatus.java     # User + friendship status relative to the current user
    │   │   └── repository/                        # CountryRepository, UserRepository, FriendshipRepository
    │   ├── exception/                             # ResourceNotFoundException, FriendshipActionException
    │   ├── service/
    │   │   ├── api/                               # CountryService, UserService
    │   │   ├── impl/                              # CountryServiceImpl, UserServiceImpl
    │   │   ├── mock/FeedMockData.java             # Reads the mock feed from JSON
    │   │   └── cors/CorsCustomizer.java           # CORS for the frontend
    │   └── util/                                  # BytesAsString, StringAsBytes, GqlQueryPaginationAndSort
    └── resources/
        ├── application.yml
        ├── graphql/query.graphqls                 # GraphQL schema (codegen source)
        ├── db/migration/rangiffler-api/V1__schema_init.sql
        └── mock/
            ├── query_feed.json                    # "Only my" feed: 4 photos
            └── query_feed_with_friends.json       # "With friends" feed: the same 4 + 3 friends' photos
```

Generated classes (`io.student.rangiffler.model.*`, for example `User`, `Photo`, `UserInput`) appear in
`build/generated/sources/dgs-codegen` after `./gradlew :rfr-api:generateJava` (it runs automatically during the build).
`UserConnection` and `PhotoConnection` are mapped to `org.springframework.data.domain.Page<...>`, the `Date` scalar to `LocalDate`.

## What is implemented and what is mocked

| Feature | Implementation |
|---|---|
| User profile (`Query.user`, `Mutation.user`) | Database. The profile is created on the first `Query.user` with country `ru` |
| Country directory (`Query.countries`) | Database, populated by the migration (238 countries) |
| People, search, pagination (`Query.users`) | Database, sorted by username |
| Friendship (`Mutation.friendship`, `friends`, `incomeInvitations`, `outcomeInvitations`) | Database, one row per pair, transition checks |
| Feed and statistics (`Query.feed`) | Mock: JSON files, identical for all users; the country filter uses a hardcoded mapping |
| Create / edit / like a photo (`Mutation.photo`) | Mock: returns a valid object but persists nothing |
| Delete a photo (`Mutation.deletePhoto`) | Mock: always `true`, deletes nothing |

The tables `photo`, `like`, `photo_like`, `statistic` already exist in the migration, but the application does not use them -
implementing photos, likes and statistics is a task of the graduation project.

## GraphQL API

Schema: `src/main/resources/graphql/query.graphqls`. Endpoint: `POST http://localhost:8081/graphql`.

### Queries

```graphql
type Query {
    countries: [Country!]!
    user: User!
    users(page:Int = 0, size:Int = 10, searchQuery:String): UserConnection
    feed(withFriends: Boolean!): Feed!
}
```

The current user and their connections:

```graphql
query GetUser {
  user {
    id
    username
    firstname
    surname
    avatar
    location { code name flag }
    friends(page: 0, size: 10) {
      edges { node { id username friendStatus } }
      pageInfo { hasNextPage hasPreviousPage }
    }
  }
}
```

Feed filtered by country:

```graphql
query GetFeed($withFriends: Boolean!, $country: String) {
  feed(withFriends: $withFriends) {
    photos(page: 0, size: 12, country: $country) {
      edges {
        node {
          id src description isOwner creationDate
          country { code name flag }
          likes { total likes { user username creationDate } }
        }
      }
      pageInfo { hasNextPage hasPreviousPage }
    }
    stat { count country { code } }
  }
}
```

`withFriends: false` reads `query_feed.json`, `true` reads `query_feed_with_friends.json`. The photo list is sliced by `page/size`.
`Feed.stat` comes from the JSON and does not depend on the filter; in the JSON, the country in `stat` has only `code`.

### Mutations

```graphql
type Mutation {
    user(input: UserInput!): User!
    photo(input: PhotoInput!): Photo!
    deletePhoto(id: ID!): Boolean
    friendship(input: FriendshipInput!): User!
}
```

Profile update (a `null`/omitted field means "do not change"):

```graphql
mutation UpdateUser($input: UserInput!) {
  user(input: $input) { id username firstname surname avatar location { code } }
}
```
```json
{ "input": { "firstname": "Ivan", "surname": "Petrov", "avatar": "data:image/png;base64,...", "location": { "code": "fr" } } }
```

Friendship:

```graphql
mutation FriendshipAction($input: FriendshipInput!) {
  friendship(input: $input) { id username friendStatus }
}
```
```json
{ "input": { "user": "<API UUID of the other user>", "action": "ADD" } }
```

| Action | Who can do it | Result |
|---|---|---|
| `ADD` | Anyone, if the pair has no record yet | `PENDING` row, response `INVITATION_SENT` |
| `ACCEPT` | Only the invitation addressee | `ACCEPTED`, response `FRIEND` |
| `REJECT` | Only the invitation addressee | The row is deleted, response `NOT_FRIEND` |
| `DELETE` | Either side, any status | The row is deleted: removing a friend or cancelling an outgoing invitation |

Photo (mock): creation requires `src` and `country`; with the `id` of an existing mock photo and `like`, a like is added:

```graphql
mutation CreatePhoto($input: PhotoInput!) {
  photo(input: $input) { id src description country { code name flag } likes { total } }
}
```
```json
{ "input": { "src": "data:image/jpeg;base64,...", "description": "Paris", "country": { "code": "fr" } } }
```

## Errors

The HTTP status of a GraphQL response is 200 even on error; check `errors`. `GraphQlExceptionResolver` maps exceptions:

| Exception | `extensions.classification` | Example `message` |
|---|---|---|
| `ResourceNotFoundException` | `NOT_FOUND` | `Country not found by code: xx`, `User not found by id: ...` |
| `FriendshipActionException` | `BAD_REQUEST` | `Invitation already sent`, `No pending invitation from this user`, `Cannot perform friendship action on yourself` |
| `IllegalArgumentException` | `BAD_REQUEST` | `Photo country is required`, invalid UUID, `size=0` |
| No token / missing required authority | `UNAUTHORIZED` / `FORBIDDEN` | Spring for GraphQL |
| Other | `INTERNAL_ERROR` | No details |

## Security

- `POST /graphql` passes the filters, access is checked on the controllers:
  query controllers - `@PreAuthorize("hasAuthority('read')")`, mutation controllers - `hasAuthority('write')`.
- The JWT is validated against the `rfr-auth` JWKS (`spring.security.oauth2.resourceserver.jwt.issuer-uri`): signature, `iss`, `exp`
  and **audience** (`jwt.audiences: rangiffler-api`). Only the access token is accepted; `id_token` (aud = client_id) is rejected with 401.
- The `authorities` claim of the access token is converted to `GrantedAuthority` without a prefix (`JwtAuthenticationConverter`
  in `RangifflerApiConfiguration`).
- The current user's username is the `sub` claim (`@AuthenticationPrincipal Jwt`).
- An invalid or expired Bearer token results in HTTP 401 from the filter, before GraphQL.
- GraphiQL (`/graphiql`) is enabled, but `GET` on it requires authentication - see the Security config section in the root README.

## Database

Schema `rangiffler-api` (created automatically, `createDatabaseIfNotExist=true`), migration
`db/migration/rangiffler-api/V1__schema_init.sql`, `ddl-auto: none`.

| Table | Purpose |
|---|---|
| `user` | API profile: username (linked to `rfr-auth` by username only), first name, surname, avatar (LONGBLOB, data URL), country |
| `country` | Country directory, flags are data URLs in a BLOB |
| `friendship` | One row per pair: `requester_id`, `addressee_id`, `status` (`PENDING`/`ACCEPTED`), `created_at`, `responded_at`, `version`; unique index on the unordered pair |
| `photo`, `like`, `photo_like`, `statistic` | Prepared, not used by the application yet |

User UUIDs in `rangiffler-api` and `rangiffler-auth` are independent.

## Running

Requirements: JDK 25 (Gradle toolchain), Docker for MySQL, a running `rfr-auth` (needed for JWKS).

```bash
# from the project root: MySQL in Docker
docker volume create mysql-volume   # once per machine: compose.yaml declares the volume as external
docker compose up -d --wait

cd rfr-api
../gradlew bootRun
# or the main class io.student.rangiffler.RangifflerApiApplication from the IDE
```

Check (the access token can be taken from the frontend's localStorage after login, key `access_token`):

```bash
curl -X POST http://localhost:8081/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -d '{"query":"{ user { id username location { code } } }"}'
```
