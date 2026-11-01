# RFR-AUTH - Authorization Server

## Description

`rfr-auth` is the Rangiffler registration and authentication service, built on Spring Authorization Server (OAuth 2.1 / OpenID Connect 1.0).
It stores user accounts, serves the login and registration pages, and issues tokens to the frontend using
Authorization Code + PKCE. `rfr-api` validates the issued access tokens against this service's JWKS.

## Technology stack

- **Java 25**, Gradle 9.7.1 (wrapper in the project root)
- **Spring Boot 4.1.1**
- **Spring Security 7** + **Spring Authorization Server** - OAuth 2.1 / OIDC
- **Spring Data JPA**, **Flyway**, **MySQL 8.4** (`rangiffler-auth` schema)
- **Thymeleaf** - login / register / error pages

## Module structure

```
rfr-auth/src/main/
├── java/io/student/rangiffler/
│   ├── RangifflerAuthApplication.java
│   ├── config/
│   │   ├── RangifflerAuthServiceConfig.java                  # Two SecurityFilterChains, JWKS, PasswordEncoder
│   │   ├── AccessTokenCustomizer.java                        # aud and authorities in the access token
│   │   ├── PublicClientRevocationAuthenticationConverter.java # /oauth2/revoke for the public client
│   │   ├── PublicClientRevocationAuthenticationProvider.java
│   │   └── ThemeCookieFilter.java                            # ?theme=light|dark -> rangiffler-theme cookie
│   ├── controller/
│   │   ├── LoginController.java                              # GET /login, GET /
│   │   ├── RegisterController.java                           # GET/POST /register
│   │   └── CustomErrorController.java                        # /error
│   ├── data/                                                 # UserEntity, AuthorityEntity, Authority (read, write)
│   │   └── repository/UserRepository.java
│   ├── model/RegistrationForm.java                           # Registration form + validation
│   ├── service/
│   │   ├── api/UserService.java, impl/UserServiceImpl.java   # Registration
│   │   ├── impl/DatabaseUserDetailsService.java              # UserDetails from the DB
│   │   ├── OidcClearCookiesLogoutHandler.java                # Clears JSESSIONID and XSRF-TOKEN on OIDC logout
│   │   ├── GlobalExceptionHandler.java
│   │   └── cors/CorsCustomizer.java                          # CORS for the frontend (3001)
│   └── validation/                                           # @EqualPasswords, @NoWhitespace
└── resources/
    ├── application.yaml                                      # Port 9001, OAuth client, token TTLs
    ├── db/migration/rangiffler-auth/V1__schema_init.sql
    ├── templates/                                            # login.html, register.html, error.html
    └── static/                                               # styles, images, fonts
```

## OAuth client

The frontend client is registered through Spring Boot properties in `application.yaml`
(`spring.security.oauth2.authorizationserver.client.oidc-client`):

| Parameter | Value |
|---|---|
| `client-id` | `client` |
| Client authentication | `none` - public client (SPA), no secret |
| Grant type | `authorization_code`, PKCE is required (`require-proof-key: true`) |
| Redirect URI | `http://localhost:3001/authorized` |
| Post-logout redirect URI | `http://localhost:3001/logout` |
| Scopes | `openid`, `profile` |
| Consent | `require-authorization-consent: true`, but the frontend requests only `scope=openid`, and Spring Authorization Server does not ask for consent for it - there is no consent screen |
| Access token TTL | `30m` |
| Authorization code TTL | `5m` |

No refresh token is issued to the public client. After the access token expires, the frontend gets a 401 from `rfr-api`
and the user goes through authorization again; while the `rfr-auth` session is alive (30 minutes), there is no need to enter the password.

## Tokens

| Token | Intended for | Content | Lifetime |
|---|---|---|---|
| **access token** (JWT) | `rfr-api` | `sub` = username, `aud: ["rangiffler-api"]`, `authorities: ["read","write"]`, `scope` | 10 minutes |
| **id_token** (JWT) | Frontend (`aud` = `client`) | `sub`, `sid` and other OIDC claims | 30 minutes (fixed in Spring Authorization Server) |

- `aud` and `authorities` are added only to the access token, by `AccessTokenCustomizer`. Authorities are taken from the user's
  `authority` table; the service `FACTOR_*` entries (added by Spring Security 7) are dropped. The audience value is the
  `rangiffler-api.audience` property.
- `rfr-api` accepts only the access token: it checks the audience and requires `read` for queries and `write` for mutations.
  The frontend uses `id_token` only as `id_token_hint` on logout.
- The RSA signing key is generated on every start (`jwkSource()`): after `rfr-auth` restarts, all previously issued
  tokens become invalid.

## Endpoints

| Endpoint | Purpose |
|---|---|
| `GET /oauth2/authorize` | Authorization endpoint (code + PKCE) |
| `POST /oauth2/token` | Exchange a code for tokens |
| `POST /oauth2/revoke` | Token revocation (RFC 7009); `client_id` is enough for the public client |
| `POST /oauth2/introspect` | Introspection (requires a confidential client, not used by the frontend) |
| `GET /oauth2/jwks` | JWKS for signature verification |
| `GET /.well-known/openid-configuration` | OIDC discovery |
| `GET /userinfo` | OIDC UserInfo |
| `GET/POST /connect/logout` | OIDC RP-initiated logout |
| `GET /login`, `POST /login` | Login page and form |
| `GET /register`, `POST /register` | Registration page and form |

### Security filter chains

1. **Order 1 - Authorization Server**: all OAuth/OIDC endpoints. An unauthenticated browser request
   (`text/html`) is redirected to `/login`. CORS and public client authentication on `/oauth2/revoke` are also wired in here.
2. **Order 2 - application**: public `/.well-known/**`, `/register`, `/error`, static resources; form login on `/login`.

`GET /login` without a saved OAuth request (`/oauth2/authorize` with the frontend redirect URI) redirects to the frontend -
login must be started from `/oauth2/authorize` or from the Login button in the frontend.

### Token revocation by the public client

Spring Authorization Server authenticates a client with the `none` method only at the token endpoint and only when a PKCE
`code_verifier` is present, so `/oauth2/revoke` called from the SPA returns `401 invalid_client` out of the box.
`PublicClientRevocationAuthenticationConverter` and `PublicClientRevocationAuthenticationProvider` recognize the
public client by `client_id` on the revocation endpoint only (RFC 7009 allows this); all other endpoints
use the standard client authentication.

```bash
curl -X POST http://localhost:9001/oauth2/revoke \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "token=<access_token>&token_type_hint=access_token&client_id=client"
# 200 OK (also for an unknown token - RFC 7009 requires this)
```

Revocation invalidates the token in the `rfr-auth` authorization store. `rfr-api` validates the JWT locally by signature and does not
know about revocation, so a revoked access token is accepted by the API until `exp` (at most 30 minutes).

## Main processes

### Registration

`POST /register` - `application/x-www-form-urlencoded` with a CSRF token from the same session (do `GET /register` first).

| Field | Rules |
|---|---|
| `username` | Not empty, 3-50 characters, no whitespace |
| `password` | Not empty, 3-12 characters, no whitespace |
| `passwordSubmit` | Equal to `password` |

Whitespace is checked with `Character.isWhitespace` (space, tab, line feed, etc.; the non-breaking space U+00A0 is not included).
Success - HTTP 201 and the page "Congratulations! You've registered!". A validation error or a taken username
(`` Username `x` already exists ``) - HTTP 400 and the same form with a message.
The user gets the authorities `read` and `write`. The password is stored via `DelegatingPasswordEncoder` (`{bcrypt}`).

Registration creates a record only in `rangiffler-auth`. The API profile in `rangiffler-api` appears on the first
authorized `Query.user`.

### Login: Authorization Code + PKCE

```
1. Frontend -> GET /oauth2/authorize?response_type=code&client_id=client&scope=openid
               &redirect_uri=http://localhost:3001/authorized
               &code_challenge=<S256>&code_challenge_method=S256
2. rfr-auth -> 302 /login (if there is no session)
3. User     -> POST /login (username, password, _csrf)
4. rfr-auth -> 302 http://localhost:3001/authorized?code=...   (the consent screen appears only if a scope other than openid is requested)
5. Frontend -> POST /oauth2/token
               grant_type=authorization_code&code=...&redirect_uri=...&client_id=client&code_verifier=...
6. rfr-auth -> { "access_token": "...", "id_token": "...", "token_type": "Bearer", "expires_in": 599, "scope": "openid" }
```

The frontend stores `access_token` and `id_token` in localStorage and passes only the access token to `rfr-api`.

### Logout

1. The frontend revokes the access token: `POST /oauth2/revoke` (an error does not block logout).
2. Redirect to `/connect/logout?id_token_hint=<id_token>&post_logout_redirect_uri={front url}/logout`.
3. `OidcClearCookiesLogoutHandler` clears `JSESSIONID` and `XSRF-TOKEN`, and the `rfr-auth` session ends.
4. Redirect to the frontend's `/logout`, which clears localStorage.

### Theme

`ThemeCookieFilter` saves the `?theme=light|dark` parameter (the frontend appends it to auth links) in the
`rangiffler-theme` cookie for one year. Without the parameter and the cookie, the pages use the system color scheme.

## Database

The `rangiffler-auth` schema (created automatically), migration `db/migration/rangiffler-auth/V1__schema_init.sql`:

| Table | Content |
|---|---|
| `user` | UUID, unique username (up to 50), encoded password, flags `enabled`, `account_non_expired`, `account_non_locked`, `credentials_non_expired` |
| `authority` | `user_id`, `authority` - `read` / `write` |

OAuth authorizations, consents and keys are not stored in the DB (in-memory) and are lost on restart.

## Running

Requirements: JDK 25 (Gradle toolchain), Docker for MySQL.

```bash
# from the project root: MySQL in Docker
# warning: localenv.sh stops and removes ALL docker containers on the machine
bash localenv.sh

cd rfr-auth
../gradlew bootRun
# or run the main class io.student.rangiffler.RangifflerAuthApplication from the IDE
```

Verification:

```bash
curl http://localhost:9001/.well-known/openid-configuration
```

## Troubleshooting

- **Tokens stopped working after `rfr-auth` restarted.** The signing key is generated on start - log in again.
- **`rfr-api` returns 401 for a seemingly valid token.** Check that the Bearer is the access token and not the id_token
  (the id_token has `aud` = `client`), and that the token is not older than 10 minutes.
- **CORS errors.** The frontend must run on `rangiffler-front.base-uri` (`http://localhost:3001`); origins are set by `CorsCustomizer`.
- **`GET /login` immediately redirects to the frontend.** This is expected without a saved OAuth request - start from `/oauth2/authorize`.
