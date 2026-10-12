#!/bin/sh
# Runs when the web container starts (the nginx image runs every executable *.sh in /docker-entrypoint.d/).
# Writes /config.js from environment variables, so the same image works for any API and Keycloak.
# Nothing here is secret: a browser app is a public OIDC client, and the values are visible in the browser anyway.
#
#   API_BASE_URL        base URL of the library API as the browser reaches it   (default http://localhost:8081)
#   KEYCLOAK_URL        base URL of Keycloak as the browser reaches it           (default http://localhost:8080)
#   KEYCLOAK_REALM     realm name                                               (default library)
#   KEYCLOAK_CLIENT_ID  public OIDC client of the web app                        (default library-web)
set -eu

# Escape backslashes and double quotes so a value cannot break out of the JavaScript string.
js() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

cat > /usr/share/nginx/html/config.js <<EOF
window.APP_CONFIG = {
  VITE_API_BASE_URL: "$(js "${API_BASE_URL:-http://localhost:8081}")",
  VITE_KEYCLOAK_URL: "$(js "${KEYCLOAK_URL:-http://localhost:8080}")",
  VITE_KEYCLOAK_REALM: "$(js "${KEYCLOAK_REALM:-library}")",
  VITE_KEYCLOAK_CLIENT_ID: "$(js "${KEYCLOAK_CLIENT_ID:-library-web}")"
}
EOF
echo "web: wrote /config.js (API ${API_BASE_URL:-http://localhost:8081}, Keycloak ${KEYCLOAK_URL:-http://localhost:8080})"
