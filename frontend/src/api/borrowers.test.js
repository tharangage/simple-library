import { describe, expect, it, vi } from 'vitest'
import { borrowBook } from './borrowers.js'
import { apiPost } from './client.js'

vi.mock('./client.js', () => ({ apiPost: vi.fn() }))

describe('borrowBook', () => {
  it('posts the book id to the borrower books with the token', async () => {
    vi.mocked(apiPost).mockResolvedValue(null)

    await borrowBook({ token: 't', borrowerId: 'b 1', bookId: 'book-1' })

    expect(apiPost).toHaveBeenCalledWith('/apis/v1/borrowers/b%201/books', { bookId: 'book-1' }, { token: 't' })
  })
})
