# Keycloak setup for self-registration (E02)

`POST /apis/v1/registrations` creates users through the Keycloak **Admin REST API**. The backend signs in
as a service account, so Keycloak needs one more client. Do this once per Keycloak (the realm is not yet
exported to the repo; see E01 for the full realm plan).

## 1. Client `library-backend` (service account)
Admin Console → realm **library** → *Clients* → *Create client*:

| Setting | Value |
|---|---|
| Client ID | `library-backend` |
| Client authentication | **On** (confidential) |
| Authentication flow | only **Service accounts roles** (turn Standard flow and Direct access grants off) |

Then:
1. *Clients → library-backend → Service accounts roles → Assign role* → filter by clients → `realm-management`:
   add **`manage-users`** and **`view-users`** and nothing else.
2. *Credentials* tab → copy the **Client secret**.
3. Give it to the backend as the environment variable **`KEYCLOAK_ADMIN_CLIENT_SECRET`**
   (`export KEYCLOAK_ADMIN_CLIENT_SECRET=…` before `./mvnw spring-boot:run`; for Docker Compose put
   `KEYCLOAK_ADMIN_CLIENT_SECRET=…` in an untracked `.env` next to `docker-compose.yaml`, which is git-ignored).
   **Never put the value in a compose file or any tracked file.** Without it the endpoint answers 500 and the log says why.

## 2. Realm role `user` as a default role
The backend does not assign a role itself (that would need more permissions). New users get the realm's
**default roles**, so: *Realm roles → create `user`* (if missing), then *Realm settings → User registration →
Default roles → Assign role → `user`*.

## 3. Allow the `mobile` attribute
The mobile number is sent as the user attribute `mobile`. Keycloak 24+ drops unknown attributes unless allowed:
*Realm settings → General → Unmanaged attributes → **Enabled*** (or add `mobile` to the User profile).

## 4. Backend URL when running in Docker
Inside the app container Keycloak is not `localhost`. `docker-compose.yaml` already sets
`APP_KEYCLOAK_ADMIN_BASE_URL=http://keycloak:8080`. Outside Docker the default `http://localhost:8080` is right.

## Troubleshooting
| Symptom | Cause |
|---|---|
| Token call returns `unauthorized_client: Client not enabled to retrieve service account` | Turn on **Service accounts roles** for `library-backend` (step 1) |
| `invalid_client` | Wrong or stale client secret |
| Registration returns 500 | Read the backend log: the message names the cause (missing secret, 401/403 from Keycloak, connection refused) |
| 403 when creating the user | The service account lacks `manage-users` / `view-users` |

On Windows PowerShell `curl` is an alias for `Invoke-WebRequest`; use `curl.exe` (with a here-string body) or run the example in WSL.

## Try it
```bash
curl -i -X POST http://localhost:8081/apis/v1/registrations -H 'Content-Type: application/json' \
  -d '{"firstName":"Ann","lastName":"Lee","email":"ann@example.com","mobile":"+14155550100","password":"s3cret-pass"}'
# 201 + Location: /apis/v1/me  (a second call with the same email gives 409 EMAIL_ALREADY_REGISTERED)
```
Then log in to the web app with that email and password.

## Not covered yet
- `Location: /apis/v1/me` points to an endpoint that does not exist until E01 (US-01.4).
- Tokens are not yet required to carry `aud=library-backend` (E01), and there are still no role checks.
- Email verification (`emailVerified=false`) is not enforced. Rate limiting belongs at the ingress / reverse proxy.

## Users created in the Keycloak console or by an identity provider (US-02.4)

These users have no Borrower yet. The backend creates it on their first call to `GET /apis/v1/me`, borrow or
return (see [ADR-001](../decisions/ADR-001-borrower-provisioning.md)). It reads `given_name`, `family_name` and
`email` from the token, so per identity provider (and for console users) check:

- **Scopes:** the `library-web` client must include the `profile` and `email` scopes in the access token.
- **Mappers:** the IdP imports first name, last name and email into the Keycloak user (attribute importer
  mappers: IdP `given_name` / `family_name` / `email` → user `firstName` / `lastName` / `email`). Console users:
  fill in first name, last name and email.
- **Trust email:** enable it only for IdPs that verify emails. `email_verified=true` in the token is what lets the
  backend link an older Borrower row that has the same email but no Keycloak id; otherwise the call answers 409.
- If the token has no name, `preferred_username` is used; a missing email is allowed.
