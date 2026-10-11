import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client.js'
import { fetchMyBorrower } from '../../api/borrowers.js'
import { createMockAuth, loggedInUser } from '../../test/mockAuth.js'
import { useCurrentUser } from './useCurrentUser.js'

let mockAuth
vi.mock('react-oidc-context', () => ({ useAuth: () => mockAuth }))
vi.mock('../../api/borrowers.js', () => ({ fetchMyBorrower: vi.fn() }))

const wrapper = ({ children }) => (
  <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
    {children}
  </QueryClientProvider>
)

// One QueryClient kept across rerenders, like the app's single client.
const sharedClientWrapper = () => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

afterEach(() => vi.restoreAllMocks())

describe('useCurrentUser', () => {
  it('does nothing while the user is not logged in', () => {
    mockAuth = createMockAuth()
    renderHook(() => useCurrentUser(), { wrapper })
    expect(fetchMyBorrower).not.toHaveBeenCalled()
  })

  it('loads the borrower with the access token and logs it to the console', async () => {
    mockAuth = createMockAuth({ isAuthenticated: true, user: loggedInUser })
    const log = vi.spyOn(console, 'log').mockImplementation(() => {})
    vi.mocked(fetchMyBorrower).mockResolvedValue({ id: 'b-1', firstname: 'Ada' })

    renderHook(() => useCurrentUser(), { wrapper })

    await waitFor(() => expect(log).toHaveBeenCalledWith(expect.stringContaining('/borrowers/me'), { id: 'b-1', firstname: 'Ada' }))
    expect(fetchMyBorrower).toHaveBeenCalledWith({ token: 'test-token' })
  })

  it('fetches again for a different user instead of reusing the cached borrower', async () => {
    vi.spyOn(console, 'log').mockImplementation(() => {})
    vi.mocked(fetchMyBorrower).mockResolvedValue({ id: 'b-1' })
    mockAuth = createMockAuth({ isAuthenticated: true, user: { ...loggedInUser, profile: { sub: 'u-1' } } })
    const { rerender } = renderHook(() => useCurrentUser(), { wrapper: sharedClientWrapper() })
    await waitFor(() => expect(fetchMyBorrower).toHaveBeenCalledTimes(1))

    mockAuth = createMockAuth({ isAuthenticated: true, user: { ...loggedInUser, profile: { sub: 'u-2' } } })
    rerender()

    await waitFor(() => expect(fetchMyBorrower).toHaveBeenCalledTimes(2))
  })

  it('logs a warning, and does not throw, when the backend refuses (e.g. 403 for an admin)', async () => {
    mockAuth = createMockAuth({ isAuthenticated: true, user: loggedInUser })
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
    vi.mocked(fetchMyBorrower).mockRejectedValue(new ApiError(403, 'Access denied'))

    renderHook(() => useCurrentUser(), { wrapper })

    await waitFor(() => expect(warn).toHaveBeenCalledWith(expect.stringContaining('403')))
  })
})
