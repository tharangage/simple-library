import { describe, expect, it, vi } from 'vitest'
import { apiGet } from './client.js'
import { fetchMe } from './me.js'

vi.mock('./client.js', () => ({ apiGet: vi.fn() }))

describe('fetchMe', () => {
  it('calls the me endpoint with the token and returns the profile', async () => {
    vi.mocked(apiGet).mockResolvedValue({ userId: 'u-1', borrower: { id: 'b-1' } })

    const me = await fetchMe({ token: 't' })

    expect(apiGet).toHaveBeenCalledWith('/apis/v1/me', { token: 't' })
    expect(me).toEqual({ userId: 'u-1', borrower: { id: 'b-1' } })
  })
})
