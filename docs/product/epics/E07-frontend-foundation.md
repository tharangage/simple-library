# E07 – Web app foundation (React + JavaScript)

**Goal:** A maintainable React app in **JavaScript only (no TypeScript)**, with Keycloak login, an API client, routing, tests and a container image.
**Decisions:** D-03, D-04, D-09, D-10

### US-07.1 Scaffold the app (FE · S)
As a developer, I want a working React + Vite project, so that feature work can start.
- `frontend/` created with Vite (React, **JavaScript** template). Files use `.jsx`/`.js`; `jsconfig.json` only, no `tsconfig`, no `.ts(x)` files. A lint rule fails on TS files.
- ESLint (react, react-hooks, jsx-a11y) + Prettier. npm scripts: `dev`, `build`, `preview`, `lint`, `test`.
- Folder layout: `src/{app,auth,api,features/{books,my-books,register,admin},components,hooks,utils}`.
- Node LTS version pinned in `.nvmrc` and `package.json` `engines`. `README.md` explains running it in WSL (`npm run dev` → http://localhost:5173).

### US-07.2 Log in with Keycloak (FE · M)
As a user, I want to log in and out with my library account, so that the app knows who I am.
- OIDC Authorization Code + PKCE against realm `library`, client `library-web` (library per D-03).
- Tokens are kept in memory (not `localStorage`). Silent renew before expiry. On failure → redirect to login.
- Logout ends the Keycloak session (`end_session_endpoint`) and returns to the landing page.
- Landing page for anonymous visitors with *Log in* and *Register*.

### US-07.3 API client & error handling (FE · M)
As a developer, I want one API client, so that tokens, base URL and errors are handled in one place.
- A `fetch` wrapper adds `Authorization: Bearer`, sets the base URL from runtime config, and parses the common error body (US-08.1) into `{status, code, message, fieldErrors}`.
- 401 → re-authenticate. 403 → "no access" page. Network error → retry banner.
- Server state via TanStack Query (D-09). Book list and "me" are invalidated after borrow, reserve, cancel and return.

### US-07.4 App shell, routing & guards (FE · S)
- React Router routes: `/`, `/register`, `/books`, `/my-books`, `/admin/returns`, `/admin/books/new`, plus a 404 page.
- `RequireAuth` and `RequireRole` guards. Header with user name, "x of 3 used" and logout.
- Global toast/notification and loading patterns.

### US-07.5 UI kit, styling & accessibility (FE · S)
- Component library per D-10, with a small theme (colours, typography).
- WCAG 2.1 AA basics: labels, focus states, colour contrast, keyboard navigation. axe check in component tests.
- Layout works from 360 px width upward.

### US-07.6 Frontend tests (FE · M)
- Vitest + React Testing Library + MSW (mock API). Tests for the register form, books list states, borrow/reserve flows and role guards.
- Coverage report. Threshold starts at 70% lines and rises over time.
- Optional: Playwright smoke test (login → borrow) against the full compose stack.

### US-07.7 Container image & runtime config (OPS · S)
As ops, I want one image for every environment (12-factor), so that config is not baked in at build time.
- Multi-stage Dockerfile: `node` build → `nginx` (unprivileged) serving `dist/`, with SPA fallback to `index.html`.
- `config.js` is generated at container start from env vars (`API_BASE_URL`, `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`) and loaded before the app.
- Security headers (CSP allowing Keycloak + API origins, `X-Content-Type-Options`, `Referrer-Policy`).

### US-07.8 Frontend CI (OPS · S)
- GitHub Actions workflow on PRs touching `frontend/**`: `npm ci`, lint, test with coverage, build, `npm audit --audit-level=high`, dependency review.
- Builds the Docker image (push only on `main`).
