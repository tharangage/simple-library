#!/usr/bin/env bash
# Builds the cloud overlay and applies it to the current (cloud) cluster, then waits until everything is ready.
#
#   DOCKERHUB_USER=<user> BASE_DOMAIN=<domain> ./k8s/cloud/deploy.sh
#
# Host names (override one by one if you want other names):
#   BASE_DOMAIN   e.g. example.com  ->  library.example.com, api.example.com, auth.example.com
#   WEB_HOST, API_HOST, AUTH_HOST
# Optional:
#   CERT_ISSUER   letsencrypt-staging (default: certificates browsers do NOT trust, safe for testing) or
#                 letsencrypt-prod (real certificates; switch to it once the staging run works)
#   IMAGE_TAG     image tag to deploy (default: latest)
#   REALM_SRC     Keycloak partial export (default: keycloak/realm-library.json)
set -euo pipefail
K8S_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ROOT_DIR="$(dirname "$K8S_DIR")"
OVERLAY="$K8S_DIR/overlays/cloud"

: "${DOCKERHUB_USER:?Set DOCKERHUB_USER to your Docker Hub user name}"
: "${BASE_DOMAIN:=}"
WEB_HOST="${WEB_HOST:-${BASE_DOMAIN:+library.$BASE_DOMAIN}}"
API_HOST="${API_HOST:-${BASE_DOMAIN:+api.$BASE_DOMAIN}}"
AUTH_HOST="${AUTH_HOST:-${BASE_DOMAIN:+auth.$BASE_DOMAIN}}"
: "${WEB_HOST:?Set BASE_DOMAIN (or WEB_HOST, API_HOST and AUTH_HOST)}"
: "${API_HOST:?Set BASE_DOMAIN (or WEB_HOST, API_HOST and AUTH_HOST)}"
: "${AUTH_HOST:?Set BASE_DOMAIN (or WEB_HOST, API_HOST and AUTH_HOST)}"
export DOCKERHUB_USER WEB_HOST API_HOST AUTH_HOST
export IMAGE_TAG="${IMAGE_TAG:-latest}"
export CERT_ISSUER="${CERT_ISSUER:-letsencrypt-staging}"
REALM_SRC="${REALM_SRC:-$ROOT_DIR/keycloak/realm-library.json}"

for tool in kubectl envsubst python3 openssl; do
  command -v "$tool" >/dev/null || { echo "Missing '$tool'. (envsubst: sudo apt install gettext-base)" >&2; exit 1; }
done
[ -f "$REALM_SRC" ] || { echo "Realm export not found: $REALM_SRC" >&2; exit 1; }

CONTEXT="$(kubectl config current-context)"
case "$CONTEXT" in
  kind-*) echo "The current kubectl context is '$CONTEXT' (a local kind cluster); this script is for the cloud cluster." >&2; exit 1 ;;
esac
echo "kubectl context: $CONTEXT"
echo "Hosts:           https://$WEB_HOST  https://$API_HOST  https://$AUTH_HOST"
echo "Certificates:    $CERT_ISSUER"
read -r -p "Deploy to this cluster? [y/N] " answer
[ "$answer" = "y" ] || { echo "Cancelled."; exit 1; }

# Secrets: generated once, never committed (see .gitignore). The Keycloak admin password is random here.
if [ ! -f "$OVERLAY/secrets.env" ]; then
  ( umask 077
    cat > "$OVERLAY/secrets.env" <<SECRETS
POSTGRES_PASSWORD=$(openssl rand -hex 16)
SIMPLE_LIBRARY_DB_PASSWORD=$(openssl rand -hex 16)
KEYCLOAK_ADMIN_PASSWORD=$(openssl rand -hex 12)
KEYCLOAK_ADMIN_CLIENT_SECRET=$(openssl rand -hex 24)
SECRETS
  )
  echo "Created $OVERLAY/secrets.env (the Keycloak admin password is in it)"
fi
if ! grep -q '^ENTRA_CLIENT_SECRET=.' "$OVERLAY/secrets.env"; then
  echo "Note: ENTRA_CLIENT_SECRET is not set in $OVERLAY/secrets.env, so the Entra ID login will have no secret." >&2
fi

# The realm file with the public addresses (same script as for kind; see make-realm.py).
REALM_WEB_URL="https://$WEB_HOST" REALM_API_URL="https://$API_HOST" REALM_AUTH_URL="https://$AUTH_HOST" \
  REALM_SSL_REQUIRED=external \
  python3 "$K8S_DIR/kind/make-realm.py" "$REALM_SRC" "$OVERLAY/realm-library.json"

# Only these variables are substituted, so other ${...} placeholders (e.g. in the realm file) stay untouched.
kubectl kustomize "$OVERLAY" | envsubst '${DOCKERHUB_USER} ${IMAGE_TAG} ${WEB_HOST} ${API_HOST} ${AUTH_HOST} ${CERT_ISSUER}' | kubectl apply -f -

NS=simple-library
kubectl -n "$NS" rollout status statefulset/postgres --timeout=300s
kubectl -n "$NS" rollout status deployment/keycloak --timeout=600s
kubectl -n "$NS" rollout status deployment/api --timeout=300s
kubectl -n "$NS" rollout status deployment/web --timeout=120s

cat <<DONE

Deployed. The certificate is requested now; this usually takes a minute or two:
  kubectl -n $NS get certificate        (READY must become True)
  kubectl -n $NS describe certificate simple-library-tls    (events show what cert-manager is doing)

  Web app:     https://$WEB_HOST
  Swagger UI:  https://$API_HOST/swagger-ui/index.html
  Keycloak:    https://$AUTH_HOST   (admin / the KEYCLOAK_ADMIN_PASSWORD in $OVERLAY/secrets.env)

Entra ID redirect URI:  https://$AUTH_HOST/realms/library/broker/entra-id/endpoint
SAML test provider:     entity ID https://$AUTH_HOST/realms/library
DONE
if [ "$CERT_ISSUER" = "letsencrypt-staging" ]; then
  echo
  echo "Staging certificates are not trusted by browsers. When the staging run works, run again with CERT_ISSUER=letsencrypt-prod."
fi
