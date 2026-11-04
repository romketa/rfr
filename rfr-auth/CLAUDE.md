## rfr-auth (Spring Authorization Server)

Details in `rfr-auth/README.md`.

## Commands

```bash
./gradlew :rfr-auth:bootRun      # start before rfr-api: rfr-api fetches JWKS via issuer-uri
curl http://localhost:9001/.well-known/openid-configuration
```

There are no unit tests yet.

## Key details

- The public client `client` (no secret, Authorization Code + PKCE) is registered via properties in `src/main/resources/application.yaml`;
  its redirect URI is fixed to `http://localhost:3001/authorized`.
- `AccessTokenCustomizer` adds `aud: rangiffler-api` and an `authorities` claim (`read`, `write` from the `authority` table) to the **access token** only.
- The RSA signing key is generated on every start and authorizations are in-memory → all tokens become invalid after a restart.
- Access token TTL is set in `application.yaml` (`access-token-time-to-live`, currently 30 minutes); the yaml is the source of truth.
- Token revocation (`/oauth2/revoke`) for the public client works via `PublicClientRevocationAuthentication{Converter,Provider}`;
  `rfr-api` validates JWTs locally and is unaware of revocation.
