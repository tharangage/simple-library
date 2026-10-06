# Simple Library – web app

React + **JavaScript** (no TypeScript) front end for the Simple Library API.
Implements story **US-03.1 Books page**, plus the minimum foundation it needs (parts of US-07.1–07.4).

## Run it (WSL Ubuntu)
Prerequisites: Node **22.22+** (`node -v`; with nvm: `nvm install` reads `.nvmrc`), Keycloak on :8080, the backend on :8081.

```bash
cd frontend
cp .env.example .env   # first time only
npm install
npm run dev          # http://localhost:5173
```

| Command | What it does |
|---|---|
| `npm run dev` | Dev server with hot reload on port 5173 (fails if the port is taken, on purpose) |
| `npm test` | Unit/component tests (Vitest + React Testing Library), once |
| `npm run test:watch` | Tests re-run on every save |
| `npm run lint` | oxlint (fast linter that came with the Vite template) |
| `npm run build` / `npm run preview` | Production build into `dist/` / serve that build on :4173 |

Settings live in `.env` (API URL, Keycloak URL, realm, client id), created from `.env.example`. `.env` is not committed.

## One-time Keycloak setup: client `library-web`
In the Keycloak Admin Console (http://localhost:8080/admin) → realm **library** → *Clients* → *Create client*:

| Setting | Value |
|---|---|
| Client type / Client ID | OpenID Connect / `library-web` |
| Client authentication | **Off** (public client: a browser app can't keep a secret) |
| Authentication flow | **Standard flow** only (untick Direct access grants) |
| Valid redirect URIs | `http://localhost:5173/*` |
| Valid post logout redirect URIs | `http://localhost:5173/*` |
| Web origins | `http://localhost:5173` |
| Advanced → Proof Key for Code Exchange Code Challenge Method | `S256` |

You also need a user in realm `library` with a password (Users → Add user → Credentials).
The backend allows `http://localhost:5173` in `app.cors.allowed-origins` already.

## How the code is organised
```
src/
  main.jsx               App start: AuthProvider (login) → QueryClientProvider (data) → RouterProvider (pages)
  config.js              Reads VITE_* settings; fails loudly if one is missing
  auth/                  Keycloak login: oidcConfig, RequireAuth guard, /auth/callback page
  api/                   The only code that calls the backend: client.js (fetch + token + errors), books.js
  app/                   Router, Layout (header), Home and Not-found pages
  components/            Reusable UI pieces (Pagination)
  features/books/        Everything for the Books page: page, status rules, data hook, badge, tests
  index.css              Global styles; colours are CSS variables, dark mode swaps them
```
Rule of thumb: **pages never call `fetch` directly**. Page → hook (`useBooks`) → api function (`fetchBooks`) → `apiGet`.
Each layer can be replaced in tests, and the backend URL/token/error handling live in one place.

### Login flow (Authorization Code + PKCE)
1. You open `/books`. `RequireAuth` sees no user and calls `signinRedirect`, remembering `/books`.
2. The browser goes to Keycloak's login page; you type your password **there**, never in this app.
3. Keycloak redirects to `/auth/callback?code=…`. The library exchanges the code (plus the PKCE secret it
   generated in step 1) for tokens, then `onSigninCallback` navigates back to `/books`.
4. Every API call sends `Authorization: Bearer <access token>`; the backend checks the signature with Keycloak's keys.

Tokens are kept in memory only. After a page reload, Keycloak's session cookie logs you back in without a password prompt.
