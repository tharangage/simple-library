# ADR-001 – Create the Borrower on a user's first sign-in

- **Status:** Accepted (design agreed in discussion, not implemented yet)
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
1. **Where:** `BorrowerProvisioningService.getOrCreate(Jwt)`, called from one "current borrower" place (the planned
   `GET /apis/v1/me`, borrow and return), not from a filter on every request. Lookup is by `keycloakUserId = sub`.
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
6. **Roles:** only users with role `user` get a Borrower; admins do not. Depends on E01; until then every authenticated
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
