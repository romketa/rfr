## rfr-auth

- `GET /login` without a saved OAuth request redirects straight to the frontend: a login flow starts from `/oauth2/authorize`
  (the frontend's Login button does that).
- The login error text is localized by the browser's `Accept-Language` (e.g. Russian instead of "Bad credentials").
