# E01 – Identity & access (Keycloak realm `library`)

**Jira:** [SCRUM-5](https://tharangage.atlassian.net/browse/SCRUM-5)

**Goal:** Only users registered in the Simple Library can use the system. `user` and `admin` roles are enforced by the backend, and the realm is configured as code.
**Requirement refs:** User Access · Frontend (role `user` for all APIs except registration; role `admin` for return) · API Documentation
**Decisions:** D-01

### US-01.1 Realm `library` as code (IAM · M) · [SCRUM-15](https://tharangage.atlassian.net/browse/SCRUM-15)
As a developer, I want the existing `library` realm kept as a versioned export, so that every environment has the same roles and clients.
- The current `library` realm is exported to `keycloak/realm-library.json` and imported when the compose stack starts (`--import-realm`).
- Realm roles `user` and `admin`. New self-registered users get `user` (default role).
- Clients:
  - `library-web`: public, Authorization Code + PKCE (S256), redirect `http://localhost:5173/*`, web origin `http://localhost:5173`.
  - `library-swagger`: public, PKCE, redirect `http://localhost:8081/swagger-ui/*`.
  - `library-backend`: confidential, service account only, with **only** `realm-management: manage-users, view-users` (needed if D-01 = a).
- An audience mapper adds `library-backend` to the `aud` claim of tokens issued to `library-web` and `library-swagger`.
- Password policy is set (minimum length 8). Self-registration on the Keycloak login page is **off** if D-01 = a.
- Dev-only seed users (`user1`, `admin1`) are documented. No real secrets are committed: the client secret comes from an env var.

### US-01.2 Map Keycloak realm roles to Spring authorities (BE · S) · [SCRUM-16](https://tharangage.atlassian.net/browse/SCRUM-16)
As the backend, I want to read `realm_access.roles` from the JWT, so that I can authorize by role.
- A `JwtAuthenticationConverter` maps `user` → `ROLE_USER` and `admin` → `ROLE_ADMIN`.
- `issuer-uri` stays on realm `library` (already configured: `app.keycloak.realm: library`).
- Unit test covers the converter: a token with no roles gets no authorities.

### US-01.3 Role-based endpoint rules (BE · M) · [SCRUM-17](https://tharangage.atlassian.net/browse/SCRUM-17)
As the library, I want each endpoint restricted to the right role, so that users can't perform admin actions.

| Endpoint | Access |
|---|---|
| `POST /apis/v1/registrations` | public (no token) |
| `GET /apis/v1/books` | `user`, `admin` |
| `POST /apis/v1/books` | `admin` (done with US-03.4) |
| `GET/POST/DELETE /apis/v1/me/**` (loans, reservations) | `user` |
| `DELETE /apis/v1/borrowed-books/{bookId}` (return), `GET /apis/v1/borrowed-books` | `admin` |
| Swagger, `/v3/api-docs/**`, `OPTIONS` | public |

- No token → 401; valid token with the wrong role → 403 (uses the common error body, US-08.1).
- `@WebMvcTest` tests cover 401 / 403 / 2xx for every row.
- `.claude/CLAUDE.md` "no role checks yet" note is updated.

### US-01.4 Borrower bound to the logged-in user ("me") · [SCRUM-18](https://tharangage.atlassian.net/browse/SCRUM-18) (BE · M)
As a user, I want the API to act only on my own borrower record, so that nobody can borrow in my name.
- `Borrower` gets `keycloakUserId` (unique, not null for new rows), set from the token `sub`.
- `GET /apis/v1/me` returns my profile: name, email, mobile, active loan count, reservation count, remaining slots.
- Borrow/reserve endpoints resolve the borrower from the token. They take no borrower id in the path.
- The old `/apis/v1/borrowers/{id}/books` endpoints (now `/apis/v1/borrowed-books/{book-id}/borrowers/{id}`) are removed or made admin-only (with a changelog note).

### US-01.5 Validate token audience (BE · S) · [SCRUM-19](https://tharangage.atlassian.net/browse/SCRUM-19)
As the backend, I want to reject tokens that weren't issued for this API, so that a token for another client in the realm can't be reused.
- A JWT validator requires `library-backend` in `aud` (configurable). Otherwise → 401.
- Closes the "JWTs are not checked for audience/azp" known issue.

### US-01.6 Swagger UI login with roles (BE · S) · [SCRUM-20](https://tharangage.atlassian.net/browse/SCRUM-20)
As an API user, I want to log in from Swagger UI's *Authorize* button with my library account, so that I can try every endpoint my role allows.
- Swagger UI keeps client `library-swagger` and realm `library` (already working). Each operation shows its required role in the description.
- `POST /apis/v1/registrations` is marked as needing no auth.

### US-01.7 Borrow on behalf of another borrower: admin only (BE · S) · [SCRUM-62](https://tharangage.atlassian.net/browse/SCRUM-62) – *implemented together with US-04.1*
As the library, I want only admins to borrow on behalf of another borrower, so that a user cannot create loans under someone else's name.
- Origin: accepted gap of US-02.4 ([ADR-001](../../decisions/ADR-001-borrower-provisioning.md)). Today `POST /apis/v1/borrowed-books/{book-id}/borrowers/{borrower-id}` does not compare `{borrower-id}` with the caller, so any `user` can borrow for any existing borrower id.
- A `user` (not admin) may borrow only for their own Borrower; another id → 403. An `admin` may borrow for any existing borrower (library desk); unknown id → 404.
- Decide the rule for a token with both `user` and `admin` (admins also get `user` from the realm default role): they must still be able to borrow for others at the desk.
- `@WebMvcTest` tests: user-own, user-other, admin-other, admin+user-other. ADR-001 and `.claude/CLAUDE.md` (borrow rule) are updated.
- Overlaps US-01.4 and US-04.1, which remove the borrower id from the path. If they land first, close this story as superseded.

### US-01.8 Desk-only admin accounts are not created as borrowers (BE/IAM · S) · [SCRUM-63](https://tharangage.atlassian.net/browse/SCRUM-63)
As the library, I want desk-only staff accounts to stay out of the borrower table, so that borrower records only exist for real library members.
- Origin: accepted gap of US-02.4. `user` is a realm default role, so an admin created in the Keycloak console also gets `user` and is provisioned as a Borrower on their first call to `/apis/v1/me` or borrow.
- Options, to decide in refinement: (1) Keycloak only: unassign `default-roles-library` from desk-only admins, assign `admin` alone, and document it in `docs/setup/`; (2) backend rule: a caller with `admin` gets no Borrower even with `user` (`BorrowerProvisioningInterceptor`, `GET /me`, tests, ADR-001); (3) keep as is.
- Done when a desk-only admin has no Borrower row after using the API and a self-registered user still gets `user` and a Borrower.

### US-01.9 `GET /me` must not fail when Borrower provisioning fails (BE · S) · [SCRUM-64](https://tharangage.atlassian.net/browse/SCRUM-64)
As a user, I want `GET /apis/v1/me` to show my profile even when my Borrower cannot be created yet, so that a provisioning problem does not break a read-only call.
- Origin: accepted gap of US-02.4. The interceptor provisions before `/me`, so a 409 (token email matches an older Borrower with `email_verified` not true, token without name and email, email clash) also fails `/me`. `GET /me` is also a read with a side effect.
- `/me` returns the token profile even when provisioning throws; the failure is logged without personal data. Provisioning stays strict on borrow. Alternative: leave `borrower` out of `/me` when provisioning fails, or stop provisioning on `/me` altogether.
- Tests cover `/me` with a provisioning failure; the ADR-001 endpoint table is updated.
