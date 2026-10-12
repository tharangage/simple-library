import { apiGet, apiPost } from './client.js'

/**
 * GET /apis/v1/books – one page of books. `q` narrows it to books whose title, author or ISBN contain the text.
 * @param {{ token: string, page: number, size: number, sort?: string, q?: string }} args  page is 0-based (Spring Data)
 */
export async function fetchBooks({ token, page, size, sort, q }) {
  const body = await apiGet('/apis/v1/books', { token, params: { page, size, sort, q } })
  return normalizePage(body)
}

/**
 * Spring can serialise a page in two shapes:
 *  - today:  { content, number, size, totalElements, totalPages, ... }      (PageImpl)
 *  - planned (US-08.4): { content, page: { number, size, totalElements, totalPages } }  (PagedModel)
 * The rest of the app only sees this one simple shape, so switching the backend later is a one-file change.
 */
export function normalizePage(body) {
  const meta = body.page ?? body
  return {
    items: body.content ?? [],
    page: meta.number ?? 0,
    size: meta.size ?? 0,
    totalElements: meta.totalElements ?? 0,
    totalPages: meta.totalPages ?? 0,
  }
}

/**
 * POST /apis/v1/books – register one copy of a book (role `admin`). Answers 201 with the new book.
 * 400 gives {field: message} (see ApiError.fieldErrors); 409 means the ISBN exists with another title or author.
 * @param {{ token: string, isbn: string, title: string, author: string }} args
 */
export function createBook({ token, isbn, title, author }) {
  return apiPost('/apis/v1/books', { isbn, title, author }, { token })
}
