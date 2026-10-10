import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client.js'
import { registerUser } from '../../api/registrations.js'
import { createMockAuth, loggedInUser } from '../../test/mockAuth.js'
import { RegisterPage } from './RegisterPage.jsx'

let mockAuth
vi.mock('react-oidc-context', () => ({ useAuth: () => mockAuth }))
vi.mock('../../api/registrations.js', () => ({ registerUser: vi.fn() }))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/register']}>
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/books" element={<p>Books page</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

async function fillValidForm(overrides = {}) {
  const values = {
    'First name': 'Ann',
    'Last name': 'Lee',
    Email: 'ann@example.com',
    'Mobile number': '+14155550100',
    Password: 'secret-pass',
    'Confirm password': 'secret-pass',
    ...overrides,
  }
  for (const [label, value] of Object.entries(values)) {
    await userEvent.type(screen.getByLabelText(label), value)
  }
}

const submitButton = () => screen.getByRole('button', { name: /create account|creating/i })

describe('RegisterPage', () => {
  beforeEach(() => {
    mockAuth = createMockAuth()
    vi.mocked(registerUser).mockReset()
  })

  it('shows every field with its label and the mobile hint', () => {
    renderPage()

    for (const label of ['First name', 'Last name', 'Email', 'Mobile number', 'Password', 'Confirm password']) {
      expect(screen.getByLabelText(label)).toBeRequired()
    }
    expect(screen.getByLabelText('Mobile number')).toHaveAccessibleDescription(/country code/)
  })

  it('shows inline errors linked to their fields and sends nothing when the form is invalid', async () => {
    renderPage()

    await userEvent.click(submitButton())

    const email = screen.getByLabelText('Email')
    expect(email).toBeInvalid()
    expect(email).toHaveAccessibleDescription('Email is required')
    expect(screen.getByLabelText('First name')).toHaveFocus() // the first invalid field gets focus
    expect(registerUser).not.toHaveBeenCalled()
  })

  it('checks a field when the user leaves it, not while typing', async () => {
    renderPage()
    const mobile = screen.getByLabelText('Mobile number')

    await userEvent.type(mobile, '0415')
    expect(mobile).toHaveAccessibleDescription(/country code/) // only the hint so far
    await userEvent.tab()

    expect(mobile).toHaveAccessibleDescription(/international format/)
  })

  it('flags a confirmation that differs from the password', async () => {
    renderPage()

    await fillValidForm({ 'Confirm password': 'other-pass' })
    await userEvent.click(submitButton())

    expect(screen.getByLabelText('Confirm password')).toHaveAccessibleDescription('Passwords do not match')
    expect(registerUser).not.toHaveBeenCalled()
  })

  it('sends the values, disables the button while sending, then offers to log in', async () => {
    let finish
    vi.mocked(registerUser).mockReturnValue(new Promise((resolve) => (finish = resolve)))
    renderPage()

    await fillValidForm()
    await userEvent.click(submitButton())

    // TanStack Query also passes a context object as 2nd argument, so look at the first one only.
    expect(vi.mocked(registerUser).mock.calls[0][0]).toMatchObject({
      firstName: 'Ann',
      email: 'ann@example.com',
      mobile: '+14155550100',
    })
    expect(submitButton()).toBeDisabled()

    finish({ id: '1' })
    expect(await screen.findByRole('heading', { name: 'Account created' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }))
    expect(mockAuth.signinRedirect).toHaveBeenCalled()
  })

  it('tells the visitor the email is taken (409) and links to log in', async () => {
    vi.mocked(registerUser).mockRejectedValue(new ApiError(409, 'EMAIL_ALREADY_REGISTERED'))
    renderPage()

    await fillValidForm()
    await userEvent.click(submitButton())

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('This email is already registered.')
    await userEvent.click(within(alert).getByRole('button', { name: 'Log in' }))
    expect(mockAuth.signinRedirect).toHaveBeenCalled()
    expect(submitButton()).toBeEnabled() // they can correct the email and retry
  })

  it('puts server-side field errors (400) next to their fields', async () => {
    vi.mocked(registerUser).mockRejectedValue(
      new ApiError(400, 'Some fields are invalid.', { mobile: 'Mobile number must be valid for our region' }),
    )
    renderPage()

    await fillValidForm()
    await userEvent.click(submitButton())

    expect(await screen.findByLabelText('Mobile number')).toHaveAccessibleDescription(/valid for our region/)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows a generic message for a server failure (500) without technical details', async () => {
    vi.mocked(registerUser).mockRejectedValue(new ApiError(500, 'Internal server error'))
    renderPage()

    await fillValidForm()
    await userEvent.click(submitButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong on our side')
  })

  it('shows the network message when the service cannot be reached', async () => {
    vi.mocked(registerUser).mockRejectedValue(new ApiError(0, 'Cannot reach the library service.'))
    renderPage()

    await fillValidForm()
    await userEvent.click(submitButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the library service.')
  })

  it('sends a logged-in user to the books instead', () => {
    mockAuth = createMockAuth({ isAuthenticated: true, user: loggedInUser })
    renderPage()
    expect(screen.getByText('Books page')).toBeInTheDocument()
  })
})
