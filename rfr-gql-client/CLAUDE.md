## rfr-gql-client (React frontend)

Complete and not meant to be changed. Details in `rfr-gql-client/README.md`.

## Commands

```bash
npm ci && npm run dev    # http://localhost:3001
npm run build            # tsc + vite build
npm run lint             # eslint, --max-warnings 0
```

## Key details

- The port must stay `3001`: the redirect URI `http://localhost:3001/authorized` is fixed in `rfr-auth`.
- Each GraphQL operation is a separate hook in `src/hooks` with a named operation (`GetUser`, `GetFeed`, `FriendshipAction`, `CreatePhoto`...).
- `src/api/graphqlError.ts` shows the server message only for `BAD_REQUEST`/`NOT_FOUND`; other errors get a generic text.
- Tokens are stored in localStorage (`access_token`, `id_token`); `id_token` is used only as `id_token_hint` on logout.
