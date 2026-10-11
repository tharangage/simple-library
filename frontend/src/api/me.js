import { apiGet } from './client.js'

/**
 * GET /apis/v1/me – the signed-in user's profile, the same call for every user.
 * `borrower` is the library membership of a user with the role `user` (created by the backend on the first call,
 * US-02.4) and is null for someone who is not a member, such as a desk-only admin.
 * `mobile` comes from the member's Borrower record and is null without one.
 * @returns {Promise<{ userId: string, firstname: string|null, lastname: string|null, email: string|null,
 *   mobile: string|null, borrower: null | { id: string, borrowedCount: number, maxItems: number,
 *   remainingSlots: number } }>}
 */
export function fetchMe({ token }) {
  return apiGet('/apis/v1/me', { token })
}
