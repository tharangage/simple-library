/**
 * All environment-specific settings in one place.
 * A value comes from `window.APP_CONFIG` (public/config.js, written by the Docker image when the container
 * starts), otherwise from `import.meta.env.VITE_*`, which Vite replaces at build time with values from the .env files.
 */
function required(name) {
  const runtime = typeof window === 'undefined' ? undefined : window.APP_CONFIG
  const value = runtime?.[name] || import.meta.env[name]
  if (!value) {
    throw new Error(
      `Missing ${name}. Copy frontend/.env.example to frontend/.env (see README), or set it in the container (docs/setup/docker-hub.md).`,
    )
  }
  return value
}

export const config = {
  apiBaseUrl: required('VITE_API_BASE_URL'),
  keycloakUrl: required('VITE_KEYCLOAK_URL'),
  keycloakRealm: required('VITE_KEYCLOAK_REALM'),
  keycloakClientId: required('VITE_KEYCLOAK_CLIENT_ID'),
}
