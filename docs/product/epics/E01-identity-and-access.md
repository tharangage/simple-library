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
| `POST /apis/v1/books` | `admin` |
| `GET/POST/DELETE /apis/v1/me/**` (loans, reservations) | `user` |
| `DELETE /apis/v1/loans/{bookId}` (return), `GET /apis/v1/loans` | `admin` |
| Swagger, `/v3/api-docs/**`, `OPTIONS` | public |

- No token → 401; valid token with the wrong role → 403 (uses the common error body, US-08.1).
- `@WebMvcTest` tests cover 401 / 403 / 2xx for every row.
- `.claude/CLAUDE.md` "no role checks yet" note is updated.

### US-01.4 Borrower bound to the logged-in user ("me") · [SCRUM-18](https://tharangage.atlassian.net/browse/SCRUM-18) (BE · M)
As a user, I want the API to act only on my own borrower record, so that nobody can borrow in my name.
- `Borrower` gets `keycloakUserId` (unique, not null for new rows), set from the token `sub`.
- `GET /apis/v1/me` returns my profile: name, email, mobile, active loan count, reservation count, remaining slots.
- Borrow/reserve endpoints resolve the borrower from the token. They take no borrower id in the path.
- The old `/apis/v1/borrowers/{id}/books` endpoints are removed or made admin-only (with a changelog note).

### US-01.5 Validate token audience (BE · S) · [SCRUM-19](https://tharangage.atlassian.net/browse/SCRUM-19)
As the backend, I want to reject tokens that weren't issued for this API, so that a token for another client in the realm can't be reused.
- A JWT validator requires `library-backend` in `aud` (configurable). Otherwise → 401.
- Closes the "JWTs are not checked for audience/azp" known issue.

### US-01.6 Swagger UI login with roles (BE · S) · [SCRUM-20](https://tharangage.atlassian.net/browse/SCRUM-20)
As an API user, I want to log in from Swagger UI's *Authorize* button with my library account, so that I can try every endpoint my role allows.
- Swagger UI keeps client `library-swagger` and realm `library` (already working). Each operation shows its required role in the description.
- `POST /apis/v1/registrations` is marked as needing no auth.
