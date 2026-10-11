import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { RequireAdmin } from './RequireAdmin.jsx'

const mockAdmin = { isAdmin: false, isLoading: false, isError: false }
vi.mock('../features/user/useIsAdmin.js', () => ({ useIsAdmin: () => mockAdmin }))

const renderGuard = (state) => {
  Object.assign(mockAdmin, { isAdmin: false, isLoading: false, isError: false }, state)
  render(
    <RequireAdmin>
      <p>Secret admin page</p>
    </RequireAdmin>,
  )
}

describe('RequireAdmin', () => {
  it('shows the page to an admin', () => {
    renderGuard({ isAdmin: true })
    expect(screen.getByText('Secret admin page')).toBeInTheDocument()
  })

  it('explains the missing access to anybody else', () => {
    renderGuard({ isAdmin: false })
    expect(screen.getByRole('alert')).toHaveTextContent("You don't have access")
    expect(screen.queryByText('Secret admin page')).not.toBeInTheDocument()
  })

  it('shows a waiting message while the roles are loading, not the page', () => {
    renderGuard({ isLoading: true })
    expect(screen.getByRole('status')).toHaveTextContent('Checking your access')
    expect(screen.queryByText('Secret admin page')).not.toBeInTheDocument()
  })

  it('reports a failed access check instead of claiming there is no access', () => {
    renderGuard({ isError: true })
    expect(screen.getByRole('alert')).toHaveTextContent('Could not check your access')
  })
})
