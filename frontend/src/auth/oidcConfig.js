import { InMemoryWebStorage, WebStorageStateStore } from 'oidc-client-ts'
import { config } from '../config.js'

/**
 * Settings for react-oidc-context (which uses oidc-client-ts underneath).
 *
 * Flow: Authorization Code + PKCE. The browser is redirected to Keycloak's login page,
 * comes back with a one-time `code`, and the library swaps it for tokens. No password
 * ever touches this app.
 */
export const oidcConfig = {
  authority: `${config.keycloakUrl}/realms/${config.keycloakRealm}`,
  client_id: config.keycloakClientId,
  redirect_uri: `${window.location.origin}/auth/callback`,
  post_logout_redirect_uri: `${window.location.origin}/`,
  scope: 'openid profile email',
  // Renew the access token shortly before it expires (uses Keycloak's refresh token).
  automaticSilentRenew: true,
  // Keep tokens in memory only. A malicious script can't read them from storage, and
  // closing the tab forgets them. After a page reload, Keycloak's own session cookie
  // logs the user straight back in, so the user doesn't have to type the password again.
  userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
}
