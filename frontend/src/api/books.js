import { apiGet } from './client.js'

/**
 * GET /apis/v1/books – one page of books.
 * @param {{ token: string, page: number, size: number, sort?: string }} args  page is 0-based (Spring Data)
 */
export async function fetchBooks({ token, page, size, sort }) {
  const body = await apiGet('/apis/v1/books', { token, params: { page, size, sort } })
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
