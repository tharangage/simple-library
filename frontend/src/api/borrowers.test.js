import { describe, expect, it, vi } from 'vitest'
import { fetchMyBorrower } from './borrowers.js'
import { apiGet } from './client.js'

vi.mock('./client.js', () => ({ apiGet: vi.fn() }))

describe('fetchMyBorrower', () => {
  it('calls the borrower endpoint with the token and returns the borrower', async () => {
    vi.mocked(apiGet).mockResolvedValue({ id: 'b-1', firstname: 'Ada' })

    const borrower = await fetchMyBorrower({ token: 't' })

    expect(apiGet).toHaveBeenCalledWith('/apis/v1/borrowers/me', { token: 't' })
    expect(borrower).toEqual({ id: 'b-1', firstname: 'Ada' })
  })
})
