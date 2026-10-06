/**
 * All environment-specific settings in one place.
 * Vite replaces `import.meta.env.VITE_*` at build time with values from the .env files.
 */
function required(name) {
  const value = import.meta.env[name]
  if (!value) {
    throw new Error(`Missing ${name}. Copy frontend/.env.example to frontend/.env (see README).`)
  }
  return value
}

export const config = {
  apiBaseUrl: required('VITE_API_BASE_URL'),
  keycloakUrl: required('VITE_KEYCLOAK_URL'),
  keycloakRealm: required('VITE_KEYCLOAK_REALM'),
  keycloakClientId: required('VITE_KEYCLOAK_CLIENT_ID'),
}
