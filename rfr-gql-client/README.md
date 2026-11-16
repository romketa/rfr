# rfr-gql-client - Rangiffler frontend application

## Description

`rfr-gql-client` is a React SPA for Rangiffler: a travel map, a photo feed, people and friendship, a profile.
It talks to the backend through GraphQL (`rfr-api`) and authenticates against `rfr-auth` using OAuth 2.1 Authorization Code + PKCE.

The feed, photos and likes on the backend are still mocked: the UI shows successful operations, but after the feed is re-read
the data returns to the contents of the JSON (see the `rfr-api` README for details).

## Technology stack

- **React 19**, **TypeScript 6**
- **Vite 8** - bundler and dev server (Node.js `^20.19.0` or `>=22.12.0`)
- **Apollo Client 4** - GraphQL client (`@apollo/client/react` for hooks)
- **MUI 9** - components, light and dark theme (CSS variables)
- **React Router 8** - routing
- **crypto-js** - PKCE (`code_verifier`, `code_challenge`)
- **react-svg-worldmap** - world map
- **ESLint 10** (flat config, `eslint.config.js`), `typescript-eslint`

## Project structure

```
rfr-gql-client/src/
├── api/
│   ├── apolloClient.ts     # Apollo: Bearer access token, 401 handling, cache
│   ├── authClient.ts       # POST /oauth2/token and /oauth2/revoke
│   ├── authUtils.ts        # PKCE, authorize/register/logout links, tokens in localStorage
│   └── graphqlError.ts     # User-facing error text by extensions.classification
├── components/
│   ├── AppContent/         # Routes
│   ├── PrivateRoute/       # Protected routes: GetUser + context providers
│   ├── MenuAppBar/         # Top bar: profile, theme switch, Logout
│   ├── Drawer/, Sidebar/   # Side menu
│   ├── CollapsibleMapCard/ # Collapsible map card
│   ├── WorldMap/           # Map with statistics, click on a country = feed filter
│   ├── VisitedCountries/   # "N countries visited" popup list
│   ├── Toggle/             # "Only my" / "With friends"
│   ├── PhotoContainer/     # Photo grid, skeletons, infinite scroll, empty states
│   ├── PhotoCard/          # Card: 4:3 photo, country, like, owner menu (Edit / Delete)
│   ├── AddPhotoFab/        # Floating "Add photo" button
│   ├── PhotoModal/         # Only formValidate.ts - initial state and validation of the photo form
│   ├── ImageUpload/, CountrySelect/
│   ├── ProfileForm/        # Profile form
│   ├── PeopleTable/        # All / Friends / Income / Outcome, cards on narrow screens
│   ├── Table/              # ActionButtons, TableToolbar (search), Pagination, TableHead
│   ├── QueryErrorAlert/    # Query error with a Retry button
│   ├── Loader/, TabPanel/
├── context/                # SessionContext, CountriesContext, DialogContext (photo form), SnackBarContext + use* hooks
├── hooks/                  # One hook per GraphQL operation (see below)
├── pages/                  # LandingPage, Redirect, Authorized, Logout, MyTravelsPage, PeoplePage, ProfilePage
├── types/                  # Connection, Country, Likes, Order, Photo, Stat, User
├── utils/                  # arrays, comparator
├── theme.tsx               # MUI theme (light / dark)
├── App.tsx, main.tsx
```

## Routes

| Path | Purpose |
|---|---|
| `/` | Landing page: Login and Register; if the user is already logged in - redirect into the app |
| `/redirect` | Generates PKCE and sends the user to `rfr-auth` `/oauth2/authorize` |
| `/authorized` | OAuth callback: exchanges `code` for tokens |
| `/logout` | Post-logout redirect: clears localStorage |
| `/my-travels` | Map and feed (protected) |
| `/people` | People and friendship (protected) |
| `/profile` | Profile (protected) |

Protected routes are wrapped by `PrivateRoute`: it runs `GetUser` (this query also creates the API profile on first login),
on an `UNAUTHORIZED`/`FORBIDDEN` error redirects to `/`, and on other errors shows `QueryErrorAlert` with Retry.

## Authentication

1. The Login button on `/` (or the `/redirect` route) generates `code_verifier` and `code_challenge` (S256), saves them in localStorage and opens
   `/oauth2/authorize?response_type=code&client_id=client&scope=openid&redirect_uri=.../authorized&code_challenge=...&theme=...`.
2. After login, `rfr-auth` redirects back to `/authorized?code=...`.
3. `AuthorizedPage` exchanges `code` + `code_verifier` for tokens (`POST /oauth2/token`) and saves
   `access_token` and `id_token`.
4. Apollo sends **only the access token** to `rfr-api`: `Authorization: Bearer <access_token>`.
5. HTTP 401 from `rfr-api` (expired or invalid token) - `ErrorLink` clears the session and redirects to `/`.
6. Logout (`MenuAppBar`): `POST /oauth2/revoke` for the access token -> clear the Apollo store ->
   `/connect/logout?id_token_hint=<id_token>&post_logout_redirect_uri=.../logout` -> `/logout` clears localStorage.

The access token lives for 30 minutes (`access-token-time-to-live` in `rfr-auth`), no refresh token is issued to the public client: after expiry the user logs in again
(without entering a password while the `rfr-auth` session is alive).

### localStorage

| Key | Value |
|---|---|
| `access_token` | Token for `rfr-api` |
| `id_token` | Only for `id_token_hint` on logout |
| `codeVerifier`, `codeChallenge` | PKCE of the current login |
| `mui-mode` | `light` / `dark` / `system`; passed to `rfr-auth` as the `theme` parameter |
| `rangiffler-map-collapsed` | Whether the map is collapsed |

## GraphQL operations

Each operation is encapsulated in a hook in `src/hooks`. Operation names are convenient for interception in tests (`operationName`):

| Hook | Operation | Purpose |
|---|---|---|
| `useGetUser` | `query GetUser` | Current user |
| `useGetCountries` | `query GetCountries` | Country directory |
| `useGetFeed` | `query GetFeed` | Feed (`withFriends`, `country`, `page`, `size=12`), statistics; `fetchMore` for infinite scroll |
| `useQueryPeople` | `query GetPeople` | All People with search and pagination |
| `useGetFriends` | `query GetFriends` | Friends |
| `useGetInvitations` | `query GetInvitations` | Incoming requests |
| `useGetOutcomeInvitations` | `query GetOutcomeInvitations` | Outgoing requests |
| `useUpdateUser` | `mutation UpdateUser` | `user(input)` - profile |
| `useUpdateFriendshipStatus` | `mutation FriendshipAction` | `friendship(input)` - ADD / ACCEPT / REJECT / DELETE; re-reads the 4 people queries |
| `useCreatePhoto` | `mutation CreatePhoto` | `photo(input)` without id; re-reads `GetFeed` |
| `useUpdatePhoto` | `mutation UpdatePhoto` | `photo(input)` with id; re-reads `GetFeed` |
| `useLikePhoto` | `mutation LikePhoto` | `photo(input)` with `like` |
| `useDeletePhoto` | `mutation DeletePhoto` | `deletePhoto(id)`; re-reads `GetFeed` |

Errors with `extensions.classification` `BAD_REQUEST` / `NOT_FOUND` are shown to the user with the server text
(snackbar or `QueryErrorAlert`), all others - with a generic message.

The Apollo cache normalizes entities by `id`, `Feed` is merged (`merge: true`). A mutation response updates the card immediately,
so a successful snackbar or an updated card does not prove persistence - an independent read is needed to verify it.

## Configuration

`.env`:

```env
VITE_AUTH_URL=http://localhost:9001
VITE_API_URL=http://localhost:8081
VITE_FRONT_HOST=localhost
VITE_FRONT_URL=http://localhost:3001
VITE_CLIENT_ID=client
```

The dev server listens on `VITE_FRONT_HOST:3001` (`vite.config.ts`). The port must match the client's redirect URI in `rfr-auth`.

## Running

```bash
cd rfr-gql-client
npm ci          # or npm install
npm run dev     # http://localhost:3001
```

`rfr-auth` (9001) and `rfr-api` (8081) must be running.

Build and checks:

```bash
npm run build   # tsc + vite build -> dist/
npm run lint    # eslint, --max-warnings 0
npm run preview # serve dist/
```
