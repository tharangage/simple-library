import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createBook } from '../../api/books.js'
import { ApiError } from '../../api/client.js'
import { AddBookPage } from './AddBookPage.jsx'

vi.mock('react-oidc-context', () => ({ useAuth: () => ({ user: { access_token: 'test-token' } }) }))
vi.mock('../../api/books.js', () => ({ createBook: vi.fn() }))

let queryClient
const renderPage = () => {
  queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <AddBookPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

const fill = async (user, { isbn = '978-955-675-510-7', title = 'Dune', author = 'Frank Herbert' } = {}) => {
  await user.type(screen.getByLabelText('ISBN'), isbn)
  await user.type(screen.getByLabelText('Title'), title)
  await user.type(screen.getByLabelText('Author'), author)
}

// Braces matter: a function returned from beforeEach would be called by vitest as a cleanup step.
beforeEach(() => {
  vi.mocked(createBook).mockReset()
})

describe('AddBookPage', () => {
  it('shows inline errors and sends nothing while the form is invalid', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByRole('button', { name: 'Add book' }))

    expect(screen.getByLabelText('ISBN')).toHaveAccessibleDescription(/ISBN cannot be blank/)
    expect(screen.getByLabelText('ISBN')).toHaveFocus()
    expect(screen.getByText('Title cannot be blank')).toBeInTheDocument()
    expect(createBook).not.toHaveBeenCalled()
  })

  it('sends the trimmed values with the token and reports the new book', async () => {
    vi.mocked(createBook).mockResolvedValue({ id: 'bk-1' })
    const user = userEvent.setup()
    renderPage()

    await fill(user, { title: '  Dune  ' })
    await user.click(screen.getByRole('button', { name: 'Add book' }))

    await waitFor(() =>
      expect(createBook).toHaveBeenCalledWith({
        token: 'test-token',
        isbn: '978-955-675-510-7',
        title: 'Dune',
        author: 'Frank Herbert',
      }),
    )
    expect(await screen.findByRole('status')).toHaveTextContent('“Dune” was added')
  })

  it('reloads the book list after a success', async () => {
    vi.mocked(createBook).mockResolvedValue({ id: 'bk-1' })
    const user = userEvent.setup()
    renderPage()
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries')

    await fill(user)
    await user.click(screen.getByRole('button', { name: 'Add book' }))

    await screen.findByRole('status')
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['books'] })
  })

  it('adds another copy with the same values in one click and counts them', async () => {
    vi.mocked(createBook).mockResolvedValue({ id: 'bk-1' })
    const user = userEvent.setup()
    renderPage()
    await fill(user)
    await user.click(screen.getByRole('button', { name: 'Add book' }))

    await user.click(await screen.findByRole('button', { name: 'Add another copy' }))

    await waitFor(() => expect(createBook).toHaveBeenCalledTimes(2))
    expect(vi.mocked(createBook).mock.calls[1][0]).toMatchObject({ isbn: '978-955-675-510-7', title: 'Dune' })
    expect(await screen.findByText(/2 copies in a row/)).toBeInTheDocument()
  })

  it('clears the form to add a different book', async () => {
    vi.mocked(createBook).mockResolvedValue({ id: 'bk-1' })
    const user = userEvent.setup()
    renderPage()
    await fill(user)
    await user.click(screen.getByRole('button', { name: 'Add book' }))

    await user.click(await screen.findByRole('button', { name: 'Add a different book' }))

    expect(screen.getByLabelText('ISBN')).toHaveValue('')
    expect(screen.getByLabelText('ISBN')).toHaveFocus()
    expect(screen.getByRole('status')).toBeEmptyDOMElement()
  })

  it('shows the API message for an ISBN that exists with another title or author', async () => {
    vi.mocked(createBook).mockRejectedValue(new ApiError(409, 'Title and Author should be same for same ISBN'))
    const user = userEvent.setup()
    renderPage()
    await fill(user)

    await user.click(screen.getByRole('button', { name: 'Add book' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Title and Author should be same for same ISBN')
  })

  it('puts field errors from a 400 answer next to their fields', async () => {
    vi.mocked(createBook).mockRejectedValue(new ApiError(400, 'Some fields are invalid.', { isbn: 'ISBN must be in correct format' }))
    const user = userEvent.setup()
    renderPage()
    await fill(user)

    await user.click(screen.getByRole('button', { name: 'Add book' }))

    await waitFor(() => expect(screen.getByLabelText('ISBN')).toHaveAccessibleDescription(/correct format/))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it.each([
    [403, /Only library administrators/],
    [401, /session has expired/],
    [500, /Something went wrong on our side/],
  ])('explains a %s answer in words', async (status, pattern) => {
    vi.mocked(createBook).mockRejectedValue(new ApiError(status, 'raw'))
    const user = userEvent.setup()
    renderPage()
    await fill(user)

    await user.click(screen.getByRole('button', { name: 'Add book' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(pattern)
  })

  it('disables the button while the book is being sent', async () => {
    vi.mocked(createBook).mockReturnValue(new Promise(() => {}))
    const user = userEvent.setup()
    renderPage()
    await fill(user)

    await user.click(screen.getByRole('button', { name: 'Add book' }))

    const button = await screen.findByRole('button', { name: 'Adding…' })
    expect(button).toHaveAttribute('aria-disabled', 'true')
    expect(button).toHaveFocus() // the button keeps focus while sending
  })

  it('keeps the status region in the page before any message, with only text inside', async () => {
    vi.mocked(createBook).mockResolvedValue({ id: 'bk-1' })
    const user = userEvent.setup()
    renderPage()
    expect(screen.getByRole('status')).toBeEmptyDOMElement()

    await fill(user)
    await user.click(screen.getByRole('button', { name: 'Add book' }))

    const status = await screen.findByText(/was added/)
    expect(status.closest('[role="status"]')).not.toBeNull()
    expect(screen.getByRole('status')).not.toContainElement(screen.getByRole('button', { name: 'Add another copy' }))
  })

  it('moves focus to Add another copy after a success', async () => {
    vi.mocked(createBook).mockResolvedValue({ id: 'bk-1' })
    const user = userEvent.setup()
    renderPage()
    await fill(user)

    await user.click(screen.getByRole('button', { name: 'Add book' }))

    await waitFor(() => expect(screen.getByRole('button', { name: 'Add another copy' })).toHaveFocus())
  })

  it('removes the success line when adding another copy fails', async () => {
    vi.mocked(createBook)
      .mockResolvedValueOnce({ id: 'bk-1' })
      .mockRejectedValueOnce(new ApiError(500, 'boom'))
    const user = userEvent.setup()
    renderPage()
    await fill(user)
    await user.click(screen.getByRole('button', { name: 'Add book' }))

    await user.click(await screen.findByRole('button', { name: 'Add another copy' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong on our side')
    expect(screen.queryByText(/was added/)).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add another copy' })).not.toBeInTheDocument()
  })

  it('focuses the first field the server complained about', async () => {
    vi.mocked(createBook).mockRejectedValue(
      new ApiError(400, 'Some fields are invalid.', { title: 'Title must be between 1 and 254 characters' }),
    )
    const user = userEvent.setup()
    renderPage()
    await fill(user)

    await user.click(screen.getByRole('button', { name: 'Add book' }))

    await waitFor(() => expect(screen.getByLabelText('Title')).toHaveFocus())
    expect(screen.getByLabelText('Title')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Title')).toHaveAccessibleDescription(/between 1 and 254/)
  })

  it('sends only one request when the form is submitted twice in the same moment', async () => {
    vi.mocked(createBook).mockReturnValue(new Promise(() => {}))
    const user = userEvent.setup()
    renderPage()
    await fill(user)

    // Two Enter presses before React has re-rendered.
    const form = screen.getByLabelText('Author').closest('form')
    fireEvent.submit(form)
    fireEvent.submit(form)

    await waitFor(() => expect(createBook).toHaveBeenCalledTimes(1))
  })
})
