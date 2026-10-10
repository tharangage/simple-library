import { apiPost } from './client.js'

/**
 * POST /apis/v1/registrations – public, so no token. The password goes to the backend over HTTPS
 * and straight on to Keycloak; this app never stores it.
 * @param {{ firstName: string, lastName: string, email: string, mobile: string, password: string }} values
 */
export function registerUser({ firstName, lastName, email, mobile, password }) {
  return apiPost('/apis/v1/registrations', { firstName, lastName, email, mobile, password })
}
