import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { createMockAuth, loggedInUser } from '../test/mockAuth.js'
import { HomePage } from './HomePage.jsx'

let mockAuth
vi.mock('react-oidc-context', () => ({ useAuth: () => mockAuth }))

const renderHome = () =>
  render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/books" element={<p>Books page</p>} />
      </Routes>
    </MemoryRouter>,
  )

describe('HomePage', () => {
  it('invites an anonymous visitor to log in', async () => {
    mockAuth = createMockAuth()
    renderHome()

    await userEvent.click(screen.getByRole('button', { name: 'Log in' }))

    expect(screen.getByRole('heading', { name: 'Welcome to Simple Library' })).toBeInTheDocument()
    expect(mockAuth.signinRedirect).toHaveBeenCalled()
  })

  it('offers a visitor without an account the registration page', () => {
    mockAuth = createMockAuth()
    renderHome()
    expect(screen.getByRole('link', { name: 'Create an account' })).toHaveAttribute('href', '/register')
  })

  it('sends a logged-in user straight to the books', () => {
    mockAuth = createMockAuth({ isAuthenticated: true, user: loggedInUser })
    renderHome()
    expect(screen.getByText('Books page')).toBeInTheDocument()
  })

  it('shows a loading message while the login state is unknown', () => {
    mockAuth = createMockAuth({ isLoading: true })
    renderHome()
    expect(screen.getByText('Loading…')).toBeInTheDocument()
  })

  it('shows why the last login failed', () => {
    mockAuth = createMockAuth({ error: new Error('Network error') })
    renderHome()
    expect(screen.getByRole('alert')).toHaveTextContent('Login failed: Network error')
  })
})
