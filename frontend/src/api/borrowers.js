import { apiGet, apiPost } from './client.js'

/**
 * GET /apis/v1/borrowers/me – the signed-in user's Borrower (role `user`) and how many items they hold.
 * The backend creates the Borrower on the first call (US-02.4), so this is also what provisions a new user.
 * @returns {Promise<{ id: string, firstname: string, lastname: string, email: string, mobile: string|null,
 *   borrowedCount: number, maxItems: number, remainingSlots: number }>}
 */
export function fetchMyBorrower({ token }) {
  return apiGet('/apis/v1/borrowers/me', { token })
}

/**
 * POST /apis/v1/borrowers/{borrowerId}/books – borrow a book. Answers 200 with no body.
 * 409 bodies are the codes BOOK_NOT_AVAILABLE and BORROW_LIMIT_REACHED.
 */
export function borrowBook({ token, borrowerId, bookId }) {
  return apiPost(`/apis/v1/borrowers/${encodeURIComponent(borrowerId)}/books`, { bookId }, { token })
}
