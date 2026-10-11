import { apiPost } from './client.js'

/**
 * POST /apis/v1/borrowers/{borrowerId}/books – borrow a book. Answers 200 with no body.
 * 409 bodies are the codes BOOK_NOT_AVAILABLE and BORROW_LIMIT_REACHED.
 */
export function borrowBook({ token, borrowerId, bookId }) {
  return apiPost(`/apis/v1/borrowers/${encodeURIComponent(borrowerId)}/books`, { bookId }, { token })
}
