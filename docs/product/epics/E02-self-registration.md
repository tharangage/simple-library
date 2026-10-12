# E02 – User self-registration

**Jira:** [SCRUM-6](https://tharangage.atlassian.net/browse/SCRUM-6)

**Goal:** A visitor creates their own account (role `user`) from the web app with a name, a valid-format email and mobile number, and a password.
**Requirement refs:** Frontend #1
**Decisions:** D-01 (stories below assume option **a**), D-11 ([ADR-001](../../decisions/ADR-001-borrower-provisioning.md), for US-02.4), assumptions A-01, A-08, A-12

### US-02.1 Public registration API (BE · L) · [SCRUM-21](https://tharangage.atlassian.net/browse/SCRUM-21) – *done* (merged in PR #11)
As a visitor, I want to register with my details, so that I can log in and borrow books.
- `POST /apis/v1/borrowers` (first `/apis/v1/registrations`; the old admin-style borrower endpoint was removed) takes `{firstName, lastName, email, mobile, password}` and needs no token.
- Validation → 400 with field errors: all fields required. Email format. Mobile matches E.164 `^\+[1-9]\d{7,14}$`. Password ≥ 8 characters.
- Creates the Keycloak user (email as username, `emailVerified=false`, role `user`) through the Admin REST API using the `library-backend` service account. Then creates the Borrower with `keycloakUserId`.
- Email already used in Keycloak or DB → 409 `EMAIL_ALREADY_REGISTERED`.
- If saving the Borrower fails after Keycloak succeeded, the Keycloak user is deleted (compensation) and the call returns 500. Both paths are tested with a mocked Keycloak client.
- Returns 201 with `Location: /apis/v1/me` and a `BorrowerResponse` (`id`, `firstname`, `lastname`, `email`, `mobile`, dates) with no password. Password and mobile are never logged.
- Basic abuse protection: request size limit, plus a note/ticket for rate limiting at the ingress.

### US-02.2 Mobile number on Borrower (BE · S) · [SCRUM-22](https://tharangage.atlassian.net/browse/SCRUM-22) – *done* (merged in PR #11)
As the library, I want to store each borrower's mobile number, so that we can contact them later.
- `Borrower.mobile` column (nullable for existing rows). Included in `BorrowerResponse` / `GET /me`, and never shown to other users.
- H2 and PostgreSQL schema update via `ddl-auto: update` for now (migrations: see US-10.3).

### US-02.3 Registration page (FE · M) · [SCRUM-23](https://tharangage.atlassian.net/browse/SCRUM-23) – *done* (merged in PR #11)
As a visitor, I want a sign-up form, so that I can create an account without contacting the library.
- `/register` route, reachable from the landing page and the "not logged in" state.
- Fields: first name, last name, email, mobile (with `+country code` hint), password, confirm password.
- Client-side validation uses the same rules as the API, shown inline. Submit is disabled while sending.
- Field errors from the 400 response appear next to their fields. A 409 shows "This email is already registered" with a link to log in.
- On success: confirmation message, then a "Log in" button that starts the OIDC login (US-07.2).
- Accessible: labels, error text linked with `aria-describedby`, keyboard-only flow works.

### US-02.4 Borrower created on first sign-in (console and brokered IdP users) (BE · S) · [SCRUM-24](https://tharangage.atlassian.net/browse/SCRUM-24) – *implemented*
As a user whose Keycloak account was created in the Keycloak console or by a brokered identity provider, I want to be able to borrow without a separate registration step.
Design: [ADR-001](../../decisions/ADR-001-borrower-provisioning.md).
- On the first authenticated call that needs a Borrower (`me`, borrow, return), if none exists for the token's `sub`, one is created from the token claims `given_name`, `family_name`, `email`. Fallbacks: `preferred_username` for a missing name; a missing email is allowed. Mobile stays empty.
- Idempotent under concurrent first calls: unique `keycloakUserId`, insert in its own transaction, on a duplicate-key error re-read the row.
- Linked by `sub` only. An older Borrower row with the same email and no `keycloakUserId` is linked only when the token has `email_verified=true`; otherwise the call answers 409.
- Only users with role `user` get a Borrower (also if they have other roles). Later tokens never overwrite the stored profile.
- An email without `email_verified=true` is not stored on a new Borrower. A token with neither name nor email gets 409.
- Web app: after login it calls `GET /apis/v1/me` (identity from the token plus a `borrower` section for role `user`) and logs the response to the browser console in development; the Books page uses it.
- Document the Keycloak setup per IdP (name and email mappers, "trust email", `profile` and `email` scopes) in `docs/setup/`.
- Tested with mocked JWTs only; no real Keycloak.

## Implementation notes
Status: US-02.1–02.3 merged to `master` in [PR #11](https://github.com/tharangage/simple-library/pull/11) and tested against a real Keycloak. US-02.4 is implemented: `BorrowerProvisioningService`, minimal `GET /apis/v1/me`, and borrow/return call it first. Details and the endpoint/role table: [ADR-001](../../decisions/ADR-001-borrower-provisioning.md). Roles `user` / `admin` are read from `realm_access.roles`: `user` gets a Borrower; the borrower id in the borrow path is not checked against the caller (admins borrow on behalf of borrowers); an admin without `user` gets none; return is admin-only; `GET /me` is generic: it returns the token profile (`userId`, `firstname`, `lastname`, `email`) for every user and provisions the Borrower as a side effect for role `user`.

Keycloak setup: [`docs/setup/keycloak-registration.md`](../../setup/keycloak-registration.md).
- `Borrower` also got a unique, nullable `keycloakUserId` (US-01.x will make it required for new rows).
- A duplicate email that slips past the pre-check (race) is answered **409**, not 500; the Keycloak user is still deleted again.
- Endpoint moved: registration is now `POST /apis/v1/borrowers` in `BorrowerController` (was `POST /apis/v1/registrations` in `RegistrationController`). The request record `RegistrationRequest` is now `BorrowerCreateRequest` (same fields) and the response is `BorrowerResponse` instead of `RegistrationResponse`, so JSON field names are `firstname` / `lastname` and `mobile` is included (it is the caller's own number). The old admin-style borrower endpoint and `BorrowerService` were removed. Breaking: web app and backend must be deployed together.
- "Request size limit": requests declaring more than 4 KB get 413; field `@Size` limits bound the rest. Rate limiting: ingress.
- The role `user` comes from the realm's default roles; the backend does not assign it (it only has `manage-users`, `view-users`).
