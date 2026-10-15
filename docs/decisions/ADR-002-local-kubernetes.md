# ADR-002 - Run the application on Kubernetes locally with kind, Traefik and `*.localhost`

- **Status:** Accepted and implemented (`k8s/`)
- **Date:** 2026-10-15
- **Decision ID:** D-12
- **Affects:** US-10.3 (delivery), E01 (Keycloak realm and identity providers)

## Context
The application is published as two Docker Hub images (GitHub Actions). The next step is to run them, with Keycloak and
PostgreSQL, in Kubernetes: first locally on a Windows 11 machine with WSL Ubuntu, later in a free cloud tier. Local
runs must work without paid accounts, survive a reset, and keep the Keycloak realm reproducible from git.

## Decisions
| Topic | Chosen | Alternatives and why not |
|---|---|---|
| Local cluster | **kind** (Kubernetes in Docker), one node, host ports 80/443 mapped | k3d: also fine, but kind tracks upstream Kubernetes versions closely and is a CNCF conformance-tested distribution. MicroK8s: needs snap, adds a VM-like layer in WSL. |
| Manifests | **Kustomize** (`k8s/base` + `k8s/overlays/local`) | Helm charts: more machinery than three small services need; Kustomize is built into `kubectl`. A cloud overlay can be added next to `local`. |
| Ingress | **Traefik v3** installed with Helm | ingress-nginx: retired in March 2026. Gateway API: kept as an optional later overlay; Traefik supports it too. |
| Hostnames | **`library.localhost`**, **`api.localhost`** and Keycloak at plain **`http://localhost`** | `*.localtest.me`: plain http there is not a secure context, so PKCE (`crypto.subtle`) fails. A hosts-file or HTTPS with a local CA: more setup for no gain locally. |
| Keycloak at `http://localhost` | Required by Entra ID, which accepts redirect URIs only with `https://` or exactly `http://localhost` | A subdomain such as `auth.localhost` is rejected by Azure. |
| Realm | `keycloak/realm-library.json` is a seed, imported only if the realm is missing; `make-realm.py` adapts it for the cluster, `export-realm.sh` brings console changes back | Mounting the realm as the source of truth: would discard console changes on every restart. |
| Secrets | Generated into a gitignored `secrets.env`; secrets of external identity providers are injected into the realm import as `${...}` placeholders and exported masked | Committing secrets (never); typing the Entra secret in the console after every reset. |
| API token validation | `issuer-uri` is the public URL, `jwk-set-uri` is the in-cluster `http://keycloak:8080/...` | Using the public URL for JWKS: inside a pod `localhost` is the pod itself, which caused the earlier 401 on `/certs`. |

## Consequences
- One command (`deploy.sh`) recreates the whole stack from the repository; `kubectl delete namespace simple-library`
  gives a clean reset, including a fresh realm import with Entra ID and SAML test working without console steps.
- Local-only values stay out of git because the realm file is converted in both directions; exports are repeatable
  (a second export gives an empty diff).
- The seed file serves both Docker Compose (Keycloak on `:8080`) and the cluster (Keycloak on `:80`).
  `CLUSTER_ORIGIN` in `make-realm.py` must match `KC_HOSTNAME` in `overlays/local/env-local.yaml`.
- A cloud deployment needs its own overlay: a real HTTPS hostname, a certificate, the Entra redirect URI and SAML entity
  ID for that hostname, and `arm64` images if the nodes are Ampere/ARM.
- `*.localhost` works in Chrome, Edge and Firefox; `curl` and some other tools may need a hosts entry.
