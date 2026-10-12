import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client.js'
import { fetchBooks } from '../../api/books.js'
import { borrowBook } from '../../api/borrowedBooks.js'
import { fetchMe } from '../../api/me.js'
import { ToastProvider } from '../../components/Toast.jsx'
import { BooksPage } from './BooksPage.jsx'

// Replace the real login library and the real HTTP call with test doubles.
const signinRedirect = vi.fn()
vi.mock('react-oidc-context', () => ({
  useAuth: () => ({ user: { access_token: 'test-token' }, signinRedirect }),
}))
vi.mock('../../api/books.js', () => ({ fetchBooks: vi.fn() }))
vi.mock('../../api/borrowedBooks.js', () => ({ borrowBook: vi.fn() }))
vi.mock('../../api/me.js', () => ({ fetchMe: vi.fn() }))

const page = (items, overrides = {}) => ({
  items,
  page: 0,
  size: 20,
  totalElements: items.length,
  totalPages: items.length ? 1 : 0,
  ...overrides,
})

const book = (id, title, borrowed = false) => ({ id, title, author: `Author ${id}`, isbn: `978-${id}`, borrowed })

/** Shows the router's current URL so tests can check that page/sort went into it. */
function CurrentUrl() {
  const location = useLocation()
  return <div data-testid="url">{location.pathname + location.search}</div>
}

function renderPage(url = '/books') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <MemoryRouter initialEntries={[url]}>
          <BooksPage />
          <CurrentUrl />
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('BooksPage', () => {
  beforeEach(() => {
    vi.mocked(fetchBooks).mockReset()
    vi.mocked(fetchMe).mockReset()
    vi.mocked(borrowBook).mockReset()
    signinRedirect.mockReset()
    // Default: a desk-only admin (no borrower section), so no Borrow buttons. Borrow tests override this.
    vi.mocked(fetchMe).mockResolvedValue(adminOnly)
  })

  it('lists books with their status', async () => {
    vi.mocked(fetchBooks).mockResolvedValue(page([book('1', 'Dune'), book('2', 'Emma', true)]))

    renderPage()

    const rows = await screen.findAllByRole('row')
    expect(within(rows[1]).getByText('Dune')).toBeInTheDocument()
    expect(within(rows[1]).getByText('Available')).toBeInTheDocument()
    expect(within(rows[2]).getByText('Borrowed')).toBeInTheDocument()
    expect(screen.getByText('Showing 1–2 of 2 books')).toBeInTheDocument()
  })

  it('asks the API for the first page sorted by title, sending the token', async () => {
    vi.mocked(fetchBooks).mockResolvedValue(page([]))

    renderPage()

    await screen.findByText(/No books yet/)
    expect(fetchBooks).toHaveBeenCalledWith({ token: 'test-token', page: 0, size: 5, sort: 'title,asc', q: '' })
  })

  it('shows a loading message first', () => {
    vi.mocked(fetchBooks).mockReturnValue(new Promise(() => {}))

    renderPage()

    expect(screen.getByRole('status')).toHaveTextContent('Loading books…')
  })

  it('moves to the next page and keeps it in the URL', async () => {
    vi.mocked(fetchBooks).mockImplementation(async ({ page: p }) =>
      page([book(String(p), `Book on page ${p}`)], { page: p, totalElements: 21, totalPages: 2 }),
    )
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /next/i }))

    expect(await screen.findByText('Book on page 1')).toBeInTheDocument()
    expect(screen.getByTestId('url')).toHaveTextContent('/books?page=2')
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument()
  })

  it('sorts by a column, toggles direction and starts from page 1', async () => {
    vi.mocked(fetchBooks).mockResolvedValue(page([book('1', 'Dune')]))
    const user = userEvent.setup()
    renderPage('/books?page=3&sort=title,asc')

    await user.click(await screen.findByRole('button', { name: 'Author' }))
    expect(screen.getByTestId('url')).toHaveTextContent('/books?sort=author%2Casc')
    expect(fetchBooks).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0, sort: 'author,asc' }))

    await user.click(screen.getByRole('button', { name: /Author/ }))
    expect(fetchBooks).toHaveBeenLastCalledWith(expect.objectContaining({ sort: 'author,desc' }))
    expect(screen.getByRole('columnheader', { name: /Author/ })).toHaveAttribute('aria-sort', 'descending')
  })

  it('searches on submit, starts from page 1, keeps the sort order and puts the text in the URL', async () => {
    vi.mocked(fetchBooks).mockResolvedValue(page([book('1', 'Dune')]))
    const user = userEvent.setup()
    renderPage('/books?page=3&sort=author,desc')

    await user.type(await screen.findByLabelText('Search by title, author or ISBN'), '  dune  {Enter}')

    expect(fetchBooks).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 0, sort: 'author,desc', q: 'dune' }),
    )
    expect(screen.getByTestId('url')).toHaveTextContent('/books?sort=author%2Cdesc&q=dune')
  })

  it('reads the search text from the URL and shows it in the box', async () => {
    vi.mocked(fetchBooks).mockResolvedValue(page([book('1', 'Dune')]))

    renderPage('/books?q=dune')

    expect(await screen.findByLabelText('Search by title, author or ISBN')).toHaveValue('dune')
    expect(fetchBooks).toHaveBeenCalledWith(expect.objectContaining({ q: 'dune' }))
  })

  it('keeps the search text when sorting and when changing page', async () => {
    vi.mocked(fetchBooks).mockImplementation(async ({ page: p }) =>
      page([book(String(p), `Book on page ${p}`)], { page: p, totalElements: 6, totalPages: 2 }),
    )
    const user = userEvent.setup()
    renderPage('/books?q=dune')

    await user.click(await screen.findByRole('button', { name: /next/i }))
    expect(screen.getByTestId('url')).toHaveTextContent('/books?q=dune&page=2')

    await user.click(screen.getByRole('button', { name: 'Author' }))
    expect(screen.getByTestId('url')).toHaveTextContent('/books?q=dune&sort=author%2Casc')
  })

  it('tells the user when no book matches, and Clear brings the full list back', async () => {
    vi.mocked(fetchBooks).mockImplementation(async ({ q }) => (q ? page([]) : page([book('1', 'Dune')])))
    const user = userEvent.setup()
    renderPage('/books?q=zzz&page=2')

    expect(await screen.findByText('No books match “zzz”.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Clear' }))

    expect(await screen.findByText('Dune')).toBeInTheDocument()
    expect(screen.getByTestId('url')).toHaveTextContent(/^\/books$/)
    expect(screen.queryByRole('button', { name: 'Clear' })).not.toBeInTheDocument()
  })

  it('searching for nothing removes the search text', async () => {
    vi.mocked(fetchBooks).mockResolvedValue(page([book('1', 'Dune')]))
    const user = userEvent.setup()
    renderPage('/books?q=dune')

    await user.clear(await screen.findByLabelText('Search by title, author or ISBN'))
    await user.type(screen.getByLabelText('Search by title, author or ISBN'), '   {Enter}')

    expect(screen.getByTestId('url')).toHaveTextContent(/^\/books$/)
  })

  it('ignores an unknown sort field in the URL', async () => {
    vi.mocked(fetchBooks).mockResolvedValue(page([]))

    renderPage('/books?sort=password,asc&page=abc')

    await screen.findByText(/No books yet/)
    expect(fetchBooks).toHaveBeenCalledWith(expect.objectContaining({ page: 0, sort: 'title,asc' }))
  })

  it('shows an error with a retry button', async () => {
    vi.mocked(fetchBooks)
      .mockRejectedValueOnce(new ApiError(500, 'Something went wrong'))
      .mockResolvedValueOnce(page([book('1', 'Dune')]))
    const user = userEvent.setup()
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not load books: Something went wrong')
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByText('Dune')).toBeInTheDocument()
  })

  it('offers to log in again when the token is rejected', async () => {
    vi.mocked(fetchBooks).mockRejectedValue(new ApiError(401, ''))
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Log in again' }))

    expect(signinRedirect).toHaveBeenCalled()
  })

  it('offers a way back when the page number is past the end', async () => {
    vi.mocked(fetchBooks).mockResolvedValue(page([], { page: 8, totalElements: 5, totalPages: 1 }))

    renderPage('/books?page=9')

    expect(await screen.findByText(/There are no books on page 9/)).toBeInTheDocument()
  })
})

const adminOnly = { userId: 'u-admin', borrower: null }
const borrower = (overrides = {}) => ({
  userId: 'u-1',
  borrower: { id: 'b-1', borrowedCount: 1, maxItems: 3, remainingSlots: 2, ...overrides },
})

describe('BooksPage borrowing', () => {
  beforeEach(() => {
    vi.mocked(fetchBooks).mockReset()
    vi.mocked(fetchMe).mockReset()
    vi.mocked(borrowBook).mockReset()
    vi.mocked(fetchBooks).mockResolvedValue(page([book('1', 'Dune'), book('2', 'Emma', true)]))
    vi.mocked(fetchMe).mockResolvedValue(borrower())
    vi.mocked(borrowBook).mockResolvedValue(null)
  })

  it('offers Borrow only on available books', async () => {
    renderPage()

    expect(await screen.findByRole('button', { name: 'Borrow Dune' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Borrow Emma' })).not.toBeInTheDocument()
  })

  it('shows an Add book link to an admin and none to a user', async () => {
    vi.mocked(fetchMe).mockResolvedValue({ userId: 'u-admin', roles: ['admin'], borrower: null })
    const { unmount } = renderPage()
    expect(await screen.findByRole('link', { name: 'Add book' })).toHaveAttribute('href', '/books/new')
    unmount()

    vi.mocked(fetchMe).mockResolvedValue({ ...borrower(), roles: ['user'] })
    renderPage()
    await screen.findByRole('button', { name: 'Borrow Dune' })
    expect(screen.queryByRole('link', { name: 'Add book' })).not.toBeInTheDocument()
  })

  it('shows no Borrow buttons to a user without a borrower record (admin only)', async () => {
    vi.mocked(fetchMe).mockResolvedValue(adminOnly)
    renderPage()

    await screen.findByText('Dune')
    expect(screen.queryByRole('button', { name: /^Borrow/ })).not.toBeInTheDocument()
  })

  it('asks for confirmation with the title and the places left, and Cancel changes nothing', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Borrow Dune' }))

    const dialog = screen.getByRole('dialog', { name: 'Borrow this book?' })
    expect(within(dialog).getByText('Dune')).toBeInTheDocument()
    expect(within(dialog).getByText('You have 2 of 3 places left.')).toBeInTheDocument()

    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(borrowBook).not.toHaveBeenCalled()
  })

  it('borrows after confirmation, shows a message and reloads the list and the counts', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Borrow Dune' }))
    const booksCalls = vi.mocked(fetchBooks).mock.calls.length
    const userCalls = vi.mocked(fetchMe).mock.calls.length

    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Borrow' }))

    expect(borrowBook).toHaveBeenCalledWith({ token: 'test-token', borrowerId: 'b-1', bookId: '1' })
    expect(await screen.findByText(/is yours/)).toHaveTextContent('Dune')
    expect(screen.getByRole('heading', { name: 'Books' })).toHaveFocus()
    await waitFor(() => expect(vi.mocked(fetchBooks).mock.calls.length).toBeGreaterThan(booksCalls))
    await waitFor(() => expect(vi.mocked(fetchMe).mock.calls.length).toBeGreaterThan(userCalls))
  })

  it('disables Borrow with an explanation when the user is at the limit', async () => {
    vi.mocked(fetchMe).mockResolvedValue(borrower({ borrowedCount: 3, remainingSlots: 0 }))
    const user = userEvent.setup()
    renderPage()

    const button = await screen.findByRole('button', { name: 'Borrow Dune' })
    expect(button).toHaveAttribute('aria-disabled', 'true')
    expect(button).toHaveAccessibleDescription('You have reached the limit of 3 books.')
    expect(button.parentElement).toHaveAttribute('title', 'You have reached the limit of 3 books.')

    await user.click(button)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('explains that someone else took the book and reloads the list', async () => {
    vi.mocked(borrowBook).mockRejectedValue(new ApiError(409, 'BOOK_NOT_AVAILABLE'))
    const user = userEvent.setup()
    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Borrow Dune' }))
    const booksCalls = vi.mocked(fetchBooks).mock.calls.length

    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Borrow' }))

    expect(await screen.findByText(/Someone else has just borrowed this book/)).toBeInTheDocument()
    await waitFor(() => expect(vi.mocked(fetchBooks).mock.calls.length).toBeGreaterThan(booksCalls))
  })

  it('explains the limit when the server refuses with BORROW_LIMIT_REACHED', async () => {
    vi.mocked(borrowBook).mockRejectedValue(new ApiError(409, 'BORROW_LIMIT_REACHED'))
    const user = userEvent.setup()
    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Borrow Dune' }))

    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Borrow' }))

    expect(await screen.findByText(/limit of 3 books/)).toBeInTheDocument()
  })
})
