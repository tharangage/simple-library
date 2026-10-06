import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { createMockAuth } from '../test/mockAuth.js'
import { SigninCallbackPage } from './SigninCallbackPage.jsx'

let mockAuth
vi.mock('react-oidc-context', () => ({ useAuth: () => mockAuth }))

describe('SigninCallbackPage', () => {
  it('shows progress while the code is exchanged for tokens', () => {
    mockAuth = createMockAuth({ isLoading: true })
    render(<SigninCallbackPage />)
    expect(screen.getByText('Signing you in…')).toBeInTheDocument()
  })

  it('shows the error when the exchange fails', () => {
    mockAuth = createMockAuth({ error: new Error('invalid_grant') })
    render(<SigninCallbackPage />)
    expect(screen.getByRole('alert')).toHaveTextContent('Login failed: invalid_grant')
  })
})
