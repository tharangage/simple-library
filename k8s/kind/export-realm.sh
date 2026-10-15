#!/usr/bin/env bash
# Exports the realm "library" from the running local Keycloak into keycloak/realm-library.json,
# so changes made in the admin console can be committed and used by the next deployment.
#
#   ./k8s/kind/export-realm.sh
#
# Optional:
#   REALM_FILE=<path>               file to write (default: keycloak/realm-library.json)
#   KEYCLOAK_ADMIN_PASSWORD=<pw>    admin password (default: read from overlays/local/secrets.env)
#   LOCAL_PORT=<port>               local port for the temporary port-forward (default: 18080)
#
# Same content as Realm settings > Action > Partial export (clients, groups and roles included, no users).
# The local-only additions (cluster URLs, sslRequired=none) are removed again; see make-realm.py.
# Review the result with:  git diff keycloak/realm-library.json
set -euo pipefail
K8S_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ROOT_DIR="$(dirname "$K8S_DIR")"
NS=simple-library
REALM=library
REALM_FILE="${REALM_FILE:-$ROOT_DIR/keycloak/realm-library.json}"
LOCAL_PORT="${LOCAL_PORT:-18080}"
ADMIN_USER="${KEYCLOAK_ADMIN_USERNAME:-admin}"
SECRETS="$K8S_DIR/overlays/local/secrets.env"

for tool in kubectl curl python3; do
  command -v "$tool" >/dev/null || { echo "Missing '$tool'." >&2; exit 1; }
done

if [ -z "${KEYCLOAK_ADMIN_PASSWORD:-}" ] && [ -f "$SECRETS" ]; then
  KEYCLOAK_ADMIN_PASSWORD="$(grep -m1 '^KEYCLOAK_ADMIN_PASSWORD=' "$SECRETS" | cut -d= -f2-)"
fi
: "${KEYCLOAK_ADMIN_PASSWORD:?Set KEYCLOAK_ADMIN_PASSWORD (not found in $SECRETS)}"

kubectl config use-context kind-simple-library >/dev/null
kubectl -n "$NS" get deployment/keycloak >/dev/null 2>&1 \
  || { echo "Keycloak is not deployed in namespace $NS. Run ./k8s/kind/deploy.sh first." >&2; exit 1; }

TMP="$(mktemp -d)"
PF_PID=""
cleanup() {
  if [ -n "$PF_PID" ]; then kill "$PF_PID" 2>/dev/null || true; fi
  rm -rf "$TMP"
}
trap cleanup EXIT

# Talk to Keycloak through a temporary port-forward, so no DNS or ingress is involved.
kubectl -n "$NS" port-forward svc/keycloak "$LOCAL_PORT:8080" >"$TMP/port-forward.log" 2>&1 &
PF_PID=$!
BASE="http://localhost:$LOCAL_PORT"
ready=""
for _ in $(seq 1 30); do
  if curl -fsS -o /dev/null "$BASE/realms/master" 2>/dev/null; then ready=yes; break; fi
  if ! kill -0 "$PF_PID" 2>/dev/null; then
    cat "$TMP/port-forward.log" >&2
    echo "The port-forward stopped. Is port $LOCAL_PORT in use? Try LOCAL_PORT=<other port>." >&2
    exit 1
  fi
  sleep 1
done
[ -n "$ready" ] || { echo "Keycloak did not answer on $BASE within 30 seconds." >&2; exit 1; }

TOKEN="$(curl -fsS -X POST "$BASE/realms/master/protocol/openid-connect/token" \
  -d grant_type=password -d client_id=admin-cli \
  --data-urlencode "username=$ADMIN_USER" --data-urlencode "password=$KEYCLOAK_ADMIN_PASSWORD" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')" \
  || { echo "Admin login failed. Check KEYCLOAK_ADMIN_PASSWORD (changed in the console?)." >&2; exit 1; }

curl -fsS -X POST -H "Authorization: Bearer $TOKEN" \
  "$BASE/admin/realms/$REALM/partial-export?exportClients=true&exportGroupsAndRoles=true" \
  -o "$TMP/raw.json" \
  || { echo "Export of realm '$REALM' failed." >&2; exit 1; }

python3 "$K8S_DIR/kind/make-realm.py" --export --previous "$REALM_FILE" "$TMP/raw.json" "$TMP/realm.json" >/dev/null

# Replace the file only after everything succeeded.
cp "$TMP/realm.json" "$REALM_FILE.new"
mv -f "$REALM_FILE.new" "$REALM_FILE"
echo "Exported realm '$REALM' to $REALM_FILE"

if git -C "$ROOT_DIR" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git -C "$ROOT_DIR" diff --stat -- "$REALM_FILE" || true
  echo "Review with: git diff ${REALM_FILE#$ROOT_DIR/}"
fi
