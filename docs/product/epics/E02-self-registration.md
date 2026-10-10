# E02 – User self-registration

**Goal:** A visitor creates their own account (role `user`) from the web app with a name, a valid-format email and mobile number, and a password.
**Requirement refs:** Frontend #1
**Decisions:** D-01 (stories below assume option **a**), assumptions A-01, A-08, A-12

### US-02.1 Public registration API (BE · L) – *implemented*
As a visitor, I want to register with my details, so that I can log in and borrow books.
- `POST /apis/v1/registrations` takes `{firstName, lastName, email, mobile, password}` and needs no token.
- Validation → 400 with field errors: all fields required. Email format. Mobile matches E.164 `^\+[1-9]\d{7,14}$`. Password ≥ 8 characters.
- Creates the Keycloak user (email as username, `emailVerified=false`, role `user`) through the Admin REST API using the `library-backend` service account. Then creates the Borrower with `keycloakUserId`.
- Email already used in Keycloak or DB → 409 `EMAIL_ALREADY_REGISTERED`.
- If saving the Borrower fails after Keycloak succeeded, the Keycloak user is deleted (compensation) and the call returns 500. Both paths are tested with a mocked Keycloak client.
- Returns 201 with `Location: /apis/v1/me` and no password in the response. Password and mobile are never logged.
- Basic abuse protection: request size limit, plus a note/ticket for rate limiting at the ingress.

### US-02.2 Mobile number on Borrower (BE · S) – *implemented*
As the library, I want to store each borrower's mobile number, so that we can contact them later.
- `Borrower.mobile` column (nullable for existing rows). Included in `BorrowerResponse` / `GET /me`, and never shown to other users.
- H2 and PostgreSQL schema update via `ddl-auto: update` for now (migrations: see US-10.3).

### US-02.3 Registration page (FE · M) – *implemented*
As a visitor, I want a sign-up form, so that I can create an account without contacting the library.
- `/register` route, reachable from the landing page and the "not logged in" state.
- Fields: first name, last name, email, mobile (with `+country code` hint), password, confirm password.
- Client-side validation uses the same rules as the API, shown inline. Submit is disabled while sending.
- Field errors from the 400 response appear next to their fields. A 409 shows "This email is already registered" with a link to log in.
- On success: confirmation message, then a "Log in" button that starts the OIDC login (US-07.2).
- Accessible: labels, error text linked with `aria-describedby`, keyboard-only flow works.

### US-02.4 Borrower auto-created for console-created users (BE · S) – *not started*
As an admin who creates a `user` in the Keycloak console, I want that person to be able to borrow without a separate registration step.
- On the first authenticated `user` call, if no Borrower exists for `sub`, one is created from token claims (`given_name`, `family_name`, `email`). Mobile stays empty.
- Idempotent under concurrent first calls (unique `keycloakUserId`; on conflict, re-read the row).

## Implementation notes
Keycloak setup: [`docs/setup/keycloak-registration.md`](../../setup/keycloak-registration.md).
- `Borrower` also got a unique, nullable `keycloakUserId` (US-01.x will make it required for new rows).
- A duplicate email that slips past the pre-check (race) is answered **409**, not 500; the Keycloak user is still deleted again.
- "Request size limit": requests declaring more than 4 KB get 413; field `@Size` limits bound the rest. Rate limiting: ingress.
- The role `user` comes from the realm's default roles; the backend does not assign it (it only has `manage-users`, `view-users`).
