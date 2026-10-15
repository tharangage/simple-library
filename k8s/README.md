# Running Simple Library on Kubernetes (local, kind)

Runs the published Docker Hub images (`<user>/simple-library-api`, `<user>/simple-library-web`) together with
PostgreSQL and Keycloak in a local [kind](https://kind.sigs.k8s.io/) cluster inside WSL.

```
browser ──► Traefik (ports 80/443) ──┬─ library.localhost ─► web      (React + nginx)
                                     ├─ api.localhost     ─► api      (Spring Boot, Swagger UI)
                                     └─ localhost    ─► keycloak ─┐
                                                                   api ───┼─► postgres (keycloak + simple_library databases)
```

`*.localhost` resolves to 127.0.0.1 in Chrome, Edge and Firefox, so no hosts-file edits are needed. Browsers also treat `*.localhost` as a secure context even over plain http, which the login needs (`crypto.subtle` / PKCE). Do not use `*.localtest.me` here: it is plain http on a non-secure origin and login fails. Keycloak is deliberately served at plain `http://localhost` (not a subdomain): Microsoft Entra ID only accepts redirect URIs that are https or exactly `http://localhost`, so the broker URI is `http://localhost/realms/library/broker/<alias>/endpoint`.

## Layout

| Path | Purpose |
|---|---|
| `kind/` | Cluster config, Traefik values, `setup.sh`, `deploy.sh`, `export-realm.sh`, `teardown.sh`, `make-realm.py` |
| `base/` | Environment-independent manifests: Postgres (StatefulSet), Keycloak, API, web |
| `overlays/local/` | Hostnames, Ingress, secrets and realm for the local cluster |
| `../archive/k8s-old/` | The earlier plain manifests (kept for reference) |

## Prerequisites (once, in WSL Ubuntu)

- Docker running in WSL (you already have this)
- `kubectl`, `kind`, `helm`: install from the official pages
  ([kubectl](https://kubernetes.io/docs/tasks/tools/), [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation),
  [helm](https://helm.sh/docs/intro/install/))
- `envsubst`: `sudo apt install gettext-base`
- About 3 GB of free memory for the cluster. Set `memory=6GB` under `[wsl2]` in `%UserProfile%\.wslconfig`, then run
  `wsl --shutdown` in PowerShell.
- Free ports 80 and 443 on the machine. The old Docker Compose Keycloak and Postgres (ports 8080 and 5432) do not
  conflict, but stop them to save memory.
- Work in the Linux file system (for example `~/simple-library`), not under `/mnt/c`.

## Start

```bash
./k8s/kind/setup.sh                                  # cluster + Traefik, about 2 minutes
DOCKERHUB_USER=<your-docker-hub-user> ./k8s/kind/deploy.sh
```

`deploy.sh` generates `overlays/local/secrets.env` (random database passwords and the `library-backend` client secret)
and `overlays/local/realm-library.json` (a copy of `keycloak/realm-library.json` plus the cluster URLs). Neither is committed.
Use `IMAGE_TAG=sha-<commit>` to deploy a specific build instead of `latest`.

| URL | What |
|---|---|
| http://library.localhost | Web app |
| http://api.localhost/swagger-ui/index.html | Swagger UI |
| http://localhost | Keycloak (admin / admin) |

## Check that it works

```bash
kubectl -n simple-library get pods                   # all Running and Ready
kubectl -n simple-library logs deploy/api --tail=50  # no 401 on the /certs request
kubectl -n simple-library exec deploy/api -- wget -qO- http://keycloak:8080/realms/library/protocol/openid-connect/certs | head -c 120
```

Then: log in on the web app, call an API from Swagger UI (Authorize first), and test persistence with
`kubectl -n simple-library delete pod postgres-0` (the data must still be there after the pod restarts).

## Update to a new image

```bash
IMAGE_TAG=sha-<commit> DOCKERHUB_USER=<user> ./k8s/kind/deploy.sh
kubectl -n simple-library rollout restart deploy/api deploy/web   # only needed when you keep the tag "latest"
```

## Reset

```bash
./k8s/kind/teardown.sh                  # deletes the cluster and the database volume
rm k8s/overlays/local/secrets.env       # only when you also want new passwords
```

The realm is imported only when it does not yet exist in the Keycloak database (see "Change the realm" below).

## Change the realm

`keycloak/realm-library.json` is the committed starting copy of the realm. Keycloak reads it once, on the first start
with an empty database, and ignores it afterwards. So:

- **Change a setting now:** use the Keycloak admin console. It applies immediately and is stored in the database.
- **Keep the change for other deployments:** run `./k8s/kind/export-realm.sh`. It exports the realm from the running
  cluster into `keycloak/realm-library.json` (the same content as Realm settings > Action > Partial export, without
  users), removes the local-only values again (cluster URLs, `sslRequired=none`), and prints a git diff summary.
  Review it with `git diff keycloak/realm-library.json`, then commit.
- **Start a cluster from the updated file:** `./k8s/kind/teardown.sh`, then `setup.sh` and `deploy.sh`.
  The running cluster already has the change, so it needs no reimport.

What to expect from an export: the `library-backend` secret stays masked (`**********`) and `deploy.sh` supplies the
real one; users are not included, apart from the service account. A new client that has a secret or needs cluster
URLs must be added to `make-realm.py`.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `*.localhost` does not resolve | Use Chrome, Edge or Firefox (curl and other tools may not resolve it). If it still fails, add `127.0.0.1 library.localhost api.localhost` to `C:\Windows\System32\drivers\etc\hosts` (and `/etc/hosts` in WSL). |
| Keycloak shows "HTTPS required" | The master realm still requires TLS for your client address. Run `kubectl -n simple-library exec deploy/keycloak -- /opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE --server http://localhost:8080 --realm master --user admin --password admin`. |
| API pod restarts, log shows a database login error | `secrets.env` was regenerated but the Postgres volume is old. Run `teardown.sh` and set up again. |
| `ImagePullBackOff` | Wrong `DOCKERHUB_USER` or tag, or the Docker Hub repository is private. |
| Login redirects fail with "Invalid redirect URI" | The client in Keycloak lacks the cluster URL. Check `library-web` and `library-swagger` in the admin console. |
| 404 page from Traefik | The Ingress is missing or the host name differs: `kubectl -n simple-library get ingress`. |

## Notes

- Keycloak runs in `start-dev` mode, which is meant for development. A production setup would use `start` with HTTPS
  and a configured hostname.
- The ingress controller is Traefik, because the Kubernetes project retired ingress-nginx in March 2026.
- The init script in `base/postgres.yaml` duplicates `keycloak/initdb/01-simple-library.sh` (Kustomize cannot read
  files outside its folder). Change both together.
