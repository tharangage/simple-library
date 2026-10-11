import { describe, expect, it, vi } from 'vitest'
import { borrowBook, fetchMyBorrower } from './borrowers.js'
import { apiGet, apiPost } from './client.js'

vi.mock('./client.js', () => ({ apiGet: vi.fn(), apiPost: vi.fn() }))

describe('fetchMyBorrower', () => {
  it('calls the borrower endpoint with the token and returns the borrower', async () => {
    vi.mocked(apiGet).mockResolvedValue({ id: 'b-1', firstname: 'Ada' })

    const borrower = await fetchMyBorrower({ token: 't' })

    expect(apiGet).toHaveBeenCalledWith('/apis/v1/borrowers/me', { token: 't' })
    expect(borrower).toEqual({ id: 'b-1', firstname: 'Ada' })
  })
})

describe('borrowBook', () => {
  it('posts the book id to the borrower books with the token', async () => {
    vi.mocked(apiPost).mockResolvedValue(null)

    await borrowBook({ token: 't', borrowerId: 'b 1', bookId: 'book-1' })

    expect(apiPost).toHaveBeenCalledWith('/apis/v1/borrowers/b%201/books', { bookId: 'book-1' }, { token: 't' })
  })
})
