import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMockAuth, loggedInUser } from '../test/mockAuth.js'
import { Layout } from './Layout.jsx'

let mockAuth
vi.mock('react-oidc-context', () => ({ useAuth: () => mockAuth }))
const mockCurrentUser = { data: undefined }
vi.mock('../features/user/useCurrentUser.js', () => ({ useCurrentUser: () => mockCurrentUser }))

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
  beforeEach(() => {
    mockCurrentUser.data = undefined
  })

  it('shows navigation, the user name and a working logout for a logged-in user', async () => {
    mockAuth = createMockAuth({ isAuthenticated: true, user: loggedInUser })
    renderLayout()

    expect(screen.getByText('Page content')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Books' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByText('Hi, Ada')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Log out' }))
    expect(mockAuth.signoutRedirect).toHaveBeenCalled()
  })

  it('shows how many books the user holds once the borrower is loaded', () => {
    mockAuth = createMockAuth({ isAuthenticated: true, user: loggedInUser })
    mockCurrentUser.data = { userId: 'u-1', borrower: { id: 'b-1', borrowedCount: 2, maxItems: 3, remainingSlots: 1 } }
    renderLayout()

    expect(screen.getByText('2 of 3 used')).toBeInTheDocument()
  })

  it('shows no usage text for a user without a borrower (admin only)', () => {
    mockAuth = createMockAuth({ isAuthenticated: true, user: loggedInUser })
    mockCurrentUser.data = { userId: 'u-2', borrower: null }
    renderLayout()

    expect(screen.queryByText(/used$/)).not.toBeInTheDocument()
  })

  it('shows only a login button to an anonymous visitor', async () => {
    mockAuth = createMockAuth()
    renderLayout()

    expect(screen.queryByRole('link', { name: 'Books' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Register' })).toHaveAttribute('href', '/register')
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }))
    expect(mockAuth.signinRedirect).toHaveBeenCalled()
  })

  it('shows no login button while the login state is loading', () => {
    mockAuth = createMockAuth({ isLoading: true })
    renderLayout()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })
})
