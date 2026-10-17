# Running Simple Library on Kubernetes (local, kind)

The Docker Hub images (see [docker-hub.md](docker-hub.md)) run in a local [kind](https://kind.sigs.k8s.io/) cluster
together with Keycloak and PostgreSQL. The manifests and scripts live in `k8s/`; this page is the short guide, and
`k8s/README.md` has the full reference and troubleshooting table.

| What | Where |
|---|---|
| Web app | http://library.localhost |
| Swagger UI | http://api.localhost/swagger-ui/index.html |
| Keycloak (admin / admin) | http://localhost |

Open them in Chrome, Edge or Firefox: these browsers resolve `*.localhost` to 127.0.0.1 themselves.

## Prerequisites (WSL Ubuntu)
Docker (already running), `kind`, `kubectl` and `helm` (for example kind v0.33, kubectl 1.36, Helm 4), plus `envsubst`,
`curl`, `openssl` and `python3`. kubectl must be within one minor version of the cluster (Kubernetes 1.37), otherwise
use a matching kubectl. Give WSL about 6 GB of memory (`memory=6GB` in `%UserProfile%\.wslconfig`, then
`wsl --shutdown`) and keep ports 80 and 443 free on Windows.

## Start
```bash
./k8s/kind/setup.sh                                      # once: cluster + Traefik ingress controller
DOCKERHUB_USER=<your-docker-hub-user> ./k8s/kind/deploy.sh   # deploy or update the application
```
`deploy.sh` generates `k8s/overlays/local/secrets.env` on the first run (random database passwords and the Keycloak
service-account secret; Keycloak admin password `admin`). The file is gitignored. Add the secrets of external
identity providers to it by hand, for example:

```
ENTRA_CLIENT_SECRET=<value of the Entra client secret, not the secret ID>
```

Update after a new image build: `kubectl -n simple-library rollout restart deploy/api deploy/web`
(or `IMAGE_TAG=sha-<commit> ./k8s/kind/deploy.sh` to pin an exact build).

## Reset and remove
```bash
kubectl delete namespace simple-library      # wipes the database too; the next deploy re-imports the realm
./k8s/kind/teardown.sh                       # deletes the whole cluster
```

## The Keycloak realm
`keycloak/realm-library.json` is only the **seed**: Keycloak imports it when the realm `library` does not exist yet.
Changes made later in the admin console live in the database. To keep them:

```bash
./k8s/kind/export-realm.sh        # exports the running realm back into keycloak/realm-library.json
git diff keycloak/realm-library.json
```

The file in git is the same one Docker Compose uses. `k8s/kind/make-realm.py` converts it in both directions, so
the cluster-only values never reach git:

| In the cluster | In `realm-library.json` |
|---|---|
| `http://library.localhost`, `http://api.localhost` added to the redirect URIs and web origins | not present |
| `sslRequired: none` | `external` |
| client secrets injected from `secrets.env` (`library-backend`, Entra ID) | masked `**********` |
| SAML service provider entity ID with host `http://localhost` | the host of the committed file (for example `http://localhost:8080`) |

Exports are repeatable: two runs on an unchanged realm give an identical file, so a `git diff` shows only real changes.

## External identity providers (Entra ID, SAML test)
Keycloak is served at plain `http://localhost` on purpose: Microsoft Entra ID accepts only `https://` redirect URIs or
exactly `http://localhost`, so a host such as `auth.localhost` is rejected. Register these addresses:

| Provider | Setting | Value |
|---|---|---|
| Entra ID (OIDC) | Redirect URI (Web) | `http://localhost/realms/library/broker/entra-id/endpoint` |
| Entra ID (OIDC) | Client secret in Keycloak / `secrets.env` | the secret **Value** (the secret ID is rejected with AADSTS7000215) |
| SAML test (samltest.dev) | Service provider entity ID | `http://localhost/realms/library` |
| SAML test (samltest.dev) | SP metadata | `http://localhost/realms/library/broker/samltest/endpoint/descriptor` |

Never paste a secret into a chat, an issue or a committed file; rotate it if that happens.

## Why `*.localhost`
The login uses PKCE, which needs `crypto.subtle`, and browsers expose it only in a secure context. `localhost` and
`*.localhost` count as secure even over plain http; `*.localtest.me` does not (error "Crypto.subtle is available only in
secure contexts"). In a cloud deployment the same is achieved with real HTTPS.

See also [ADR-002](../decisions/ADR-002-local-kubernetes.md) and the
[deployment diagram](../architecture/README.md#7-deployment-kubernetes-kind).

For a public cloud cluster (OKE, GKE, AKS) with https and Let's Encrypt certificates, see [kubernetes-cloud.md](kubernetes-cloud.md).
