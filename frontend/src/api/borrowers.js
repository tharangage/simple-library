import { apiGet } from './client.js'

/**
 * GET /apis/v1/borrowers/me – the signed-in user's Borrower (role `user`).
 * The backend creates it on the first call (US-02.4), so this is also what provisions a new user.
 * @returns {Promise<{ id: string, firstname: string, lastname: string, email: string, mobile: string|null }>}
 */
export function fetchMyBorrower({ token }) {
  return apiGet('/apis/v1/borrowers/me', { token })
}
