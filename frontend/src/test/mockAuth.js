import { vi } from 'vitest'

/**
 * A fake `useAuth()` result. Tests change its fields to simulate "logged in", "loading", etc.
 * Use together with: vi.mock('react-oidc-context', () => ({ useAuth: () => mockAuth }))
 */
export function createMockAuth(overrides = {}) {
  return {
    isLoading: false,
    isAuthenticated: false,
    activeNavigator: undefined,
    error: undefined,
    user: null,
    signinRedirect: vi.fn(),
    signoutRedirect: vi.fn(),
    ...overrides,
  }
}

export const loggedInUser = {
  access_token: 'test-token',
  profile: { given_name: 'Ada', preferred_username: 'ada' },
}
