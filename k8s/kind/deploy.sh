#!/usr/bin/env bash
# Builds the local overlay and applies it to the kind cluster, then waits until everything is ready.
#
#   DOCKERHUB_USER=<your-docker-hub-user> ./k8s/kind/deploy.sh
#
# Optional:
#   IMAGE_TAG=sha-1a2b3c4   image tag to deploy (default: latest). A sha tag makes the deployed version explicit.
#   REALM_SRC=<file>        Keycloak partial export (default: keycloak/realm-library.json)
set -euo pipefail
K8S_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ROOT_DIR="$(dirname "$K8S_DIR")"
OVERLAY="$K8S_DIR/overlays/local"

: "${DOCKERHUB_USER:?Set DOCKERHUB_USER to your Docker Hub user name}"
export DOCKERHUB_USER
export IMAGE_TAG="${IMAGE_TAG:-latest}"
REALM_SRC="${REALM_SRC:-$ROOT_DIR/keycloak/realm-library.json}"

for tool in kubectl envsubst python3 openssl; do
  command -v "$tool" >/dev/null || { echo "Missing '$tool'. (envsubst: sudo apt install gettext-base)" >&2; exit 1; }
done
[ -f "$REALM_SRC" ] || { echo "Realm export not found: $REALM_SRC" >&2; exit 1; }
kubectl config use-context kind-simple-library >/dev/null

# Local secrets: generated once, never committed (see .gitignore).
if [ ! -f "$OVERLAY/secrets.env" ]; then
  ( umask 077
    cat > "$OVERLAY/secrets.env" <<SECRETS
POSTGRES_PASSWORD=$(openssl rand -hex 16)
SIMPLE_LIBRARY_DB_PASSWORD=$(openssl rand -hex 16)
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_ADMIN_CLIENT_SECRET=$(openssl rand -hex 24)
SECRETS
  )
  echo "Created $OVERLAY/secrets.env"
fi

if ! grep -q '^ENTRA_CLIENT_SECRET=.' "$OVERLAY/secrets.env"; then
  echo "Note: ENTRA_CLIENT_SECRET is not set in $OVERLAY/secrets.env." >&2
  echo "      A fresh Keycloak import will have no Entra ID secret (enter it in the admin console, or add the line and re-create Keycloak's database)." >&2
fi

python3 "$K8S_DIR/kind/make-realm.py" "$REALM_SRC" "$OVERLAY/realm-library.json"

# Only these two variables are substituted, so ${...} placeholders elsewhere stay untouched.
kubectl kustomize "$OVERLAY" | envsubst '${DOCKERHUB_USER} ${IMAGE_TAG}' | kubectl apply -f -

NS=simple-library
kubectl -n "$NS" rollout status statefulset/postgres --timeout=180s
kubectl -n "$NS" rollout status deployment/keycloak --timeout=300s
kubectl -n "$NS" rollout status deployment/api --timeout=300s
kubectl -n "$NS" rollout status deployment/web --timeout=120s

cat <<DONE

Ready.
  Web app:     http://library.localhost
  Swagger UI:  http://api.localhost/swagger-ui/index.html
  Keycloak:    http://localhost   (admin / admin)
DONE
