import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { createMockAuth, loggedInUser } from '../test/mockAuth.js'
import { Layout } from './Layout.jsx'

let mockAuth
vi.mock('react-oidc-context', () => ({ useAuth: () => mockAuth }))

const renderLayout = () =>
  render(
    <MemoryRouter initialEntries={['/books']}>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/books" element={<p>Page content</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )

describe('Layout', () => {
  it('shows navigation, the user name and a working logout for a logged-in user', async () => {
    mockAuth = createMockAuth({ isAuthenticated: true, user: loggedInUser })
    renderLayout()

    expect(screen.getByText('Page content')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Books' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByText('Hi, Ada')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Log out' }))
    expect(mockAuth.signoutRedirect).toHaveBeenCalled()
  })

  it('shows only a login button to an anonymous visitor', async () => {
    mockAuth = createMockAuth()
    renderLayout()

    expect(screen.queryByRole('link', { name: 'Books' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }))
    expect(mockAuth.signinRedirect).toHaveBeenCalled()
  })

  it('shows no login button while the login state is loading', () => {
    mockAuth = createMockAuth({ isLoading: true })
    renderLayout()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })
})
