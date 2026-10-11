# ADR-001 – Create the Borrower on a user's first sign-in

- **Status:** Accepted and implemented in US-02.4 (see "Implementation decisions" below for what changed from the first design)
- **Date:** 2026-10-11
- **Decision ID:** D-11
- **Affects:** US-02.4 ([SCRUM-24](https://tharangage.atlassian.net/browse/SCRUM-24)), E01 (roles, `me` endpoint)

## Context
Self-registration (US-02.1) creates the Keycloak user and the Borrower together. Keycloak users can also come into
existence in two other ways, and both must end up with a Borrower in the app database:

1. A user **created in the Keycloak console** by an admin.
2. A user **created automatically by Keycloak on the first sign-in through a brokered identity provider** (Keycloak's
   "first broker login" flow).

Assumption A-01 applies: one Keycloak user ↔ one Borrower, linked by the token's `sub` (stored as `Borrower.keycloakUserId`,
unique and nullable today).

## Options considered
| Option | How | Verdict |
|---|---|---|
| **1. Event stream** | A Keycloak **Event Listener SPI** (admin events on) publishes user-created events to Kafka; a consumer in the app creates the Borrower. A custom *authentication flow* alone would **not** see console-created users, because creating a user in the console runs no authentication flow. | Rejected for now: needs a Java SPI jar in Keycloak, a Kafka cluster and a consumer; events can be lost, so a reconciliation job is still needed; self-registration events would arrive as duplicates. Revisit if other systems need user events. |
| **2a. App creates it on first sign-in** | On the first authenticated API call, the app looks up the Borrower by `sub` and creates it from the token claims if missing. | **Chosen.** |
| **2b. Keycloak calls the app** | A custom authenticator in the first-broker-login flow calls the app. | Rejected: Keycloak customisation, couples sign-in to app availability, and does not cover console-created users. |

## Decision
Use **option 2a**. One mechanism covers both kinds of users: Keycloak has already created the user by the time a token
is issued, so the first token (console user or brokered user) carries a valid `sub`, and the app creates the Borrower on
that user's first call.

### Design
1. **Where:** `BorrowerProvisioningService.getOrCreate(Jwt)`, called from a few places that need a Borrower, not on
   every request (see "Implementation decisions"). Lookup is by `keycloakUserId = sub`.
2. **Create from claims:** `given_name`, `family_name`, `email`. Fallbacks: `preferred_username` when a name is missing;
   a null email is allowed (the unique email index permits several nulls). `mobile` stays empty.
3. **Concurrency:** insert in its own transaction (`REQUIRES_NEW`); on a duplicate-key error on `keycloakUserId`,
   re-read the row and return it. Two simultaneous first calls give one Borrower.
4. **Link by `sub` only, never by email.** If a brokered user's email matches an existing *Keycloak* user, Keycloak's
   first-broker-login flow decides (for example "verify existing account"); the `sub` then stays the same and the
   existing Borrower is found.
5. **Email clash with an older Borrower row** (created before `keycloakUserId` existed, no `keycloakUserId` set): link it
   (set `keycloakUserId`) only if the token has `email_verified=true`; otherwise answer **409** with a clear message.
   Default rule; can be changed later.
6. **Roles:** every caller with role `user` gets a Borrower (also when they have other roles); an admin without `user` does not. Implemented by `BorrowerProvisioningInterceptor`, registered only for `/apis/v1/me` and borrow (not return). Depends on E01; until then every authenticated
   user would qualify.
7. **After creation the app database owns the profile:** later tokens do not overwrite names or email.

### Keycloak configuration to document per identity provider
- Mappers that import first name, last name and email from the IdP's claims.
- "Trust email" only for IdPs known to verify emails (this is what makes `email_verified` meaningful in rule 5).
- The `library-web` client must include the `profile` and `email` scopes in the access token.

## Consequences
- **Good:** no new infrastructure or Keycloak customisation; the token stays the source of truth; one code path for
  console users and brokered users; works with the existing unique index on `keycloakUserId`.
- **Cost:** a Borrower does not exist until the user's first API call, so staff cannot see or assign borrowers who have
  never used the app. If that is needed, add a **reconciliation job** that lists users through the Keycloak Admin API
  (`library-backend` already has `view-users`) and creates the missing Borrowers. This stays far cheaper than Kafka.
- **Cost:** brokered IdPs may omit names or email, hence the fallbacks; legacy Borrower rows need the rule in item 5 or a
  one-off backfill.
- **Revisit when:** other systems need user events (then option 1), or staff need borrowers before first login.

## Testing (no real Keycloak, per project rules)
Unit tests for: create from claims, claim fallbacks, existing Borrower returned, concurrent duplicate re-read,
email-linking rule (verified / not verified), role filtering; `@WebMvcTest` with a mocked `jwt()`; a repository test for
the unique index.

## Open points
- Confirm the default for rule 5, and whether a one-off backfill of legacy Borrowers is wanted.
- Rule 6 waits on E01 (roles) and the `me` endpoint (US-01.4).
- Keycloak event names and the first-broker-login behaviour were described from general knowledge: verify them against the
  Keycloak version in use when this is implemented.

## Implementation decisions (US-02.4, agreed 2026-10-11)
Decisions taken while implementing, after the first design above. They replace the matching points of the first design.

**Roles** (read from `realm_access.roles`; `user` → `ROLE_USER`, `admin` → `ROLE_ADMIN`; converter in `SecurityConfig`)
- Every caller with role `user` has a Borrower, also when they have other roles (for example `user` and `admin`).
- An admin without `user` has no Borrower and is never provisioned.
- Rules exist only for the endpoints below. All other endpoints still just need a valid token (E01 adds the rest).

| Endpoint | Allowed | Provisions the caller's Borrower? |
|---|---|---|
| `GET /apis/v1/borrowers/me` | `user` (admin only → 403) | Yes, and returns the Borrower (`BorrowerResponse`). The web app calls it after login. |
| `GET /apis/v1/me` | `user`, `admin` | Yes for role `user`. Returns the same token profile for everybody (`UserProfileResponse`: `userId`, `firstname`, `lastname`, `email`). |
| `POST /apis/v1/borrowers/{id}/books` (borrow) | `user`, `admin` | Yes for role `user`. An admin may use any `{id}` (library desk); everybody else only their own Borrower, otherwise 403 (SCRUM-62, US-04.1). |
| `DELETE /apis/v1/borrowers/{id}/books/{book-id}` (return) | `admin` | No. Users cannot return books themselves. |

**Where it runs**
- `BorrowerProvisioningInterceptor` (a Spring `HandlerInterceptor`, registered in `WebConfig`) calls `getOrCreate` for `/apis/v1/me` and borrow only. An interceptor runs after Spring Security, can be limited to paths, and its exceptions still reach the `@ControllerAdvice`. Return is not included. It is not applied to every API: that would add a database lookup to every call and let a provisioning failure (such as a 409) block unrelated endpoints such as the book list.
- `GET /apis/v1/borrowers/me` calls the service directly because it must return the Borrower. The interceptor does not pass the Borrower on to controllers (no request attribute), since nothing else needs it.

**Changes to the first design**
- An email that is not verified (`email_verified` not true) is **not stored** on a new Borrower, so a claimed address cannot occupy the unique email slot and lock out its real owner. Linking an older Borrower by email still needs `email_verified=true` (rule 5).
- A token with no name and no email is rejected with 409 instead of creating an empty Borrower.
- Linking an older Borrower is one conditional `UPDATE ... WHERE keycloak_user_id IS NULL` (`BorrowerRepository.linkIfUnlinked`), so two different users cannot overwrite each other's link. If 0 rows change, the row is re-read: ours means success, someone else's means 409.
- The insert runs in `BorrowerInserter` (`REQUIRES_NEW`, a separate bean so the transaction proxy applies). On a duplicate key the row is re-read.
- `AccessDeniedException` is mapped to 403 in `CustomResponseEntityExceptionHandler`.

**Web app**
- After login the app calls `GET /apis/v1/borrowers/me` (hook `useCurrentUser`, mounted in `Layout`) and for now only logs the result to the browser console. Later pages can read the same cached query to fill the screens. An admin without role `user` gets 403, which is logged as a warning.

**Known gaps**
- `GET /apis/v1/me` shows only the token profile but, for role `user`, provisions first. A provisioning failure (409: unverified email matching an older Borrower, or a token with no name and no email) therefore also fails `/me` and borrow for that user. Accepted so a problem is seen at the first call.
- Because `user` is a realm default role, admins added in the Keycloak console also get `user`, so they are provisioned as borrowers too. Accepted: admins can also be library members. To keep desk-only accounts out of the borrower table, either unassign `default-roles-library` from that admin in Keycloak, or change the rule so `admin` wins.
- (Closed by US-04.1 / SCRUM-62) Borrowing for another borrower is now allowed for admins only.
- `getOrCreate` is not wired into return and the book APIs by design.
