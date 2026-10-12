import { describe, expect, it, vi } from 'vitest'
import { apiDelete, apiGet, apiPost } from './client.js'
import { borrowBook, fetchBorrowedBooks, returnBook } from './borrowedBooks.js'

vi.mock('./client.js', () => ({ apiGet: vi.fn(), apiPost: vi.fn(), apiDelete: vi.fn() }))

describe('fetchBorrowedBooks', () => {
  it('asks for one page of borrowed books with the search text and token, and returns the simple page shape', async () => {
    vi.mocked(apiGet).mockResolvedValue({ content: [{ bookId: 'b1' }], number: 1, size: 5, totalElements: 6, totalPages: 2 })

    const result = await fetchBorrowedBooks({ token: 't', page: 1, size: 5, q: 'dune' })

    expect(apiGet).toHaveBeenCalledWith('/apis/v1/borrowed-books', { token: 't', params: { page: 1, size: 5, q: 'dune' } })
    expect(result).toEqual({ items: [{ bookId: 'b1' }], page: 1, size: 5, totalElements: 6, totalPages: 2 })
  })
})

describe('borrowBook', () => {
  it('posts to the book and borrower in the path, without a body, encoding both ids', async () => {
    vi.mocked(apiPost).mockResolvedValue(null)

    await borrowBook({ token: 't', borrowerId: 'b 1', bookId: 'book/1' })

    expect(apiPost).toHaveBeenCalledWith('/apis/v1/borrowed-books/book%2F1/borrowers/b%201', undefined, { token: 't' })
  })
})

describe('returnBook', () => {
  it('deletes the book and borrower in the path with the token, encoding both ids', async () => {
    vi.mocked(apiDelete).mockResolvedValue(null)

    await returnBook({ token: 't', borrowerId: 'b 1', bookId: 'book/1' })

    expect(apiDelete).toHaveBeenCalledWith('/apis/v1/borrowed-books/book%2F1/borrowers/b%201', { token: 't' })
  })
})
