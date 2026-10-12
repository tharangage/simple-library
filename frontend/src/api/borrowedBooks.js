import { apiDelete, apiGet, apiPost } from './client.js'
import { normalizePage } from './books.js'

/**
 * GET /apis/v1/borrowed-books – one page of borrowed books (role `admin`), newest first by default.
 * `q` matches book id, ISBN, title or the borrower's email. Each item:
 * { bookId, title, author, isbn, borrowedDate, borrowerId, borrowerFirstname, borrowerLastname, borrowerEmail }
 * @param {{ token: string, page: number, size: number, q?: string }} args  page is 0-based (Spring Data)
 */
export async function fetchBorrowedBooks({ token, page, size, q }) {
  const body = await apiGet('/apis/v1/borrowed-books', { token, params: { page, size, q } })
  return normalizePage(body)
}

const borrowedBookPath = (bookId, borrowerId) =>
  `/apis/v1/borrowed-books/${encodeURIComponent(bookId)}/borrowers/${encodeURIComponent(borrowerId)}`

/**
 * POST /apis/v1/borrowed-books/{bookId}/borrowers/{borrowerId} – borrow a book. Answers 200 with no body.
 * 409 bodies are the codes BOOK_NOT_AVAILABLE and BORROW_LIMIT_REACHED.
 */
export function borrowBook({ token, borrowerId, bookId }) {
  return apiPost(borrowedBookPath(bookId, borrowerId), undefined, { token })
}

/**
 * DELETE /apis/v1/borrowed-books/{bookId}/borrowers/{borrowerId} – return a book (role `admin`, library desk).
 * Answers 200 with no body. 409 means the book is not borrowed by that borrower (for example another admin
 * returned it first).
 */
export function returnBook({ token, borrowerId, bookId }) {
  return apiDelete(borrowedBookPath(bookId, borrowerId), { token })
}
