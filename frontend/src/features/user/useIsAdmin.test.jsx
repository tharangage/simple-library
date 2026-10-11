import { renderHook } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { useIsAdmin } from './useIsAdmin.js'

const mockQuery = { data: undefined, isPending: false, isError: false }
vi.mock('./useCurrentUser.js', () => ({ useCurrentUser: () => mockQuery }))

const run = (query) => {
  Object.assign(mockQuery, { data: undefined, isPending: false, isError: false }, query)
  return renderHook(() => useIsAdmin()).result.current
}

describe('useIsAdmin', () => {
  it('is true when the roles of /me contain admin', () => {
    expect(run({ data: { roles: ['user', 'admin'] } }).isAdmin).toBe(true)
  })

  it('is false for a plain user, a missing roles list and no data', () => {
    expect(run({ data: { roles: ['user'] } }).isAdmin).toBe(false)
    expect(run({ data: {} }).isAdmin).toBe(false)
    expect(run({}).isAdmin).toBe(false)
  })

  it('passes the loading and error state on', () => {
    expect(run({ isPending: true }).isLoading).toBe(true)
    expect(run({ isError: true }).isError).toBe(true)
  })
})
