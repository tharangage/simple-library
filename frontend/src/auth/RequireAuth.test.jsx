import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMockAuth, loggedInUser } from '../test/mockAuth.js'
import { RequireAuth } from './RequireAuth.jsx'

let mockAuth
vi.mock('react-oidc-context', () => ({ useAuth: () => mockAuth }))

const renderAt = (url) =>
  render(
    <MemoryRouter initialEntries={[url]}>
      <RequireAuth>
        <p>Secret page</p>
      </RequireAuth>
    </MemoryRouter>,
  )

describe('RequireAuth', () => {
  beforeEach(() => {
    mockAuth = createMockAuth()
  })

  it('shows the page to a logged-in user', () => {
    mockAuth = createMockAuth({ isAuthenticated: true, user: loggedInUser })

    renderAt('/books')

    expect(screen.getByText('Secret page')).toBeInTheDocument()
    expect(mockAuth.signinRedirect).not.toHaveBeenCalled()
  })

  it('sends an anonymous user to Keycloak and remembers where to return', () => {
    renderAt('/books?page=2')

    expect(screen.queryByText('Secret page')).not.toBeInTheDocument()
    expect(screen.getByText('Redirecting to login…')).toBeInTheDocument()
    expect(mockAuth.signinRedirect).toHaveBeenCalledWith({ state: { returnTo: '/books?page=2' } })
  })

  it('waits while the login library is still starting up', () => {
    mockAuth = createMockAuth({ isLoading: true })

    renderAt('/books')

    expect(mockAuth.signinRedirect).not.toHaveBeenCalled()
  })

  it('does not start a second redirect while one is in progress', () => {
    mockAuth = createMockAuth({ activeNavigator: 'signinRedirect' })

    renderAt('/books')

    expect(mockAuth.signinRedirect).not.toHaveBeenCalled()
  })

  it('shows a login error instead of looping back to Keycloak', () => {
    mockAuth = createMockAuth({ error: new Error('Invalid redirect_uri') })

    renderAt('/books')

    expect(screen.getByRole('alert')).toHaveTextContent('Login failed: Invalid redirect_uri')
    expect(mockAuth.signinRedirect).not.toHaveBeenCalled()
  })
})
