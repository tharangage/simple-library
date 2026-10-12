# Publishing the Docker images to Docker Hub

GitHub Actions builds and publishes two public images (workflow `.github/workflows/docker-publish.yml`):

| Image | Built from | Contains |
|---|---|---|
| `<user>/simple-library-api` | `backend/` | Spring Boot API, port 8080 |
| `<user>/simple-library-web` | `frontend/` | React app served by nginx, port 80 |

`<user>` is the Docker Hub name stored in the secret `DOCKERHUB_USERNAME`.

## When an image is published

| Trigger | Tags |
|---|---|
| Push (merge) to `master` | `latest`, `sha-<short commit>` |
| Push of a tag such as `v1.2.0` | `1.2.0`, `1.2`, `sha-<short commit>` |
| Manual run (Actions → Docker publish → Run workflow) | `sha-<short commit>` (plus `latest` only on `master`) |

Pull requests never publish. Before anything is pushed, the workflow runs the backend build with its tests
(`./mvnw verify`) and the front-end lint, tests and build for the same commit. If one fails, nothing is published.
Only `linux/amd64` is built; the comment in the workflow shows how to add `arm64`.

To release a version: `git tag v1.2.0 && git push origin v1.2.0` (from an up-to-date `master`).

## One-time setup

1. On Docker Hub create an access token: Account settings → Personal access tokens → *Read & Write*.
2. In GitHub open the repository → Settings → Secrets and variables → Actions → *New repository secret*:
   - `DOCKERHUB_USERNAME`: your Docker Hub user or organisation name
   - `DOCKERHUB_TOKEN`: the token from step 1
3. The repositories `simple-library-api` and `simple-library-web` are created on the first push (public by default for a
   personal account; check the visibility on Docker Hub afterwards).

The workflow stops with a clear message if a secret is missing. Never commit the token.

## Running the published images

Neither image contains an environment. Both are configured when the container starts.

### Web (`simple-library-web`)

The web app is a browser app, so these must be the addresses **as the user's browser reaches them**.

| Variable | Default | Meaning |
|---|---|---|
| `API_BASE_URL` | `http://localhost:8081` | the library API |
| `KEYCLOAK_URL` | `http://localhost:8080` | Keycloak |
| `KEYCLOAK_REALM` | `library` | realm |
| `KEYCLOAK_CLIENT_ID` | `library-web` | public OIDC client of the web app |

At start-up `docker/40-runtime-config.sh` writes them to `/config.js`, which `index.html` loads before the app.
They are not secret (a browser app is a public OIDC client). For `npm run dev` and `npm run build` the values still
come from `frontend/.env` (`VITE_*`); a value in `/config.js` wins over the build-time one.

### API (`simple-library-api`)

Plain Spring Boot environment variables. The ones that matter:

| Variable | Meaning |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` / `prod` for PostgreSQL. The image default is `local`, which expects PostgreSQL on `localhost` |
| `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | database |
| `APP_KEYCLOAK_PUBLIC_URL` | Keycloak as the **browser** reaches it. Must equal the token's `iss` claim |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` | where the API fetches the signing keys (the container address of Keycloak) |
| `APP_KEYCLOAK_ADMIN_BASE_URL` | Keycloak as the **API container** reaches it (registration, Admin API) |
| `KEYCLOAK_ADMIN_CLIENT_SECRET` | secret of the `library-backend` client. No default; keep it out of files in the repository |
| `APP_CORS_ALLOWED_ORIGINS` | comma-separated origin(s) of the web app, e.g. `http://localhost:5173` |

### Example (replace `<user>`)

```bash
docker run -d --name library-web -p 5173:80 \
  -e API_BASE_URL=http://localhost:8081 -e KEYCLOAK_URL=http://localhost:8080 \
  <user>/simple-library-web:latest

docker run -d --name library-api -p 8081:8080 --network keycloak_default \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/simple_library \
  -e SPRING_DATASOURCE_USERNAME=simple_library -e SPRING_DATASOURCE_PASSWORD=... \
  -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://keycloak:8080/realms/library/protocol/openid-connect/certs \
  -e APP_KEYCLOAK_ADMIN_BASE_URL=http://keycloak:8080 \
  -e KEYCLOAK_ADMIN_CLIENT_SECRET=... \
  <user>/simple-library-api:latest
```

For local development keep using `docker compose up -d --build` (see the compose file); it builds from source and the
web image falls back to the localhost defaults above.

## Notes

- This workflow is the only publisher; the old `Jenkinsfile` (with a placeholder push) was removed.
- Images are tagged by commit (`sha-...`) so a deployment can pin an exact build; `latest` moves with `master`.
- Never commit a Keycloak client secret (not even as a default value in a compose file); the project rule is that only local-only defaults go into the repository.
