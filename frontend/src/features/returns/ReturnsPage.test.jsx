import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client.js'
import { fetchBorrowedBooks, returnBook } from '../../api/borrowedBooks.js'
import { ToastProvider } from '../../components/Toast.jsx'
import { ReturnsPage } from './ReturnsPage.jsx'

vi.mock('react-oidc-context', () => ({ useAuth: () => ({ user: { access_token: 'test-token' } }) }))
vi.mock('../../api/borrowedBooks.js', () => ({ fetchBorrowedBooks: vi.fn(), returnBook: vi.fn() }))

const page = (items, overrides = {}) => ({
  items,
  page: 0,
  size: 5,
  totalElements: items.length,
  totalPages: items.length ? 1 : 0,
  ...overrides,
})

const borrowedBook = (id, title, overrides = {}) => ({
  bookId: `book-${id}`,
  title,
  author: `Author ${id}`,
  isbn: `978-${id}`,
  borrowedDate: '2026-10-01T09:30:00',
  borrowerId: `borrower-${id}`,
  borrowerFirstname: 'Ann',
  borrowerLastname: 'Lee',
  borrowerEmail: `ann${id}@example.com`,
  ...overrides,
})

function CurrentUrl() {
  const location = useLocation()
  return <div data-testid="url">{location.pathname + location.search}</div>
}

function renderPage(url = '/admin/returns') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <MemoryRouter initialEntries={[url]}>
          <ReturnsPage />
          <CurrentUrl />
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('ReturnsPage', () => {
  beforeEach(() => {
    vi.mocked(fetchBorrowedBooks).mockReset()
    vi.mocked(returnBook).mockReset()
  })

  it('lists borrowed books with the borrower, and starts with focus in the search box', async () => {
    vi.mocked(fetchBorrowedBooks).mockResolvedValue(page([borrowedBook('1', 'Dune')]))

    renderPage()

    const row = (await screen.findAllByRole('row'))[1]
    expect(within(row).getByText('Dune')).toBeInTheDocument()
    expect(within(row).getByText('Ann Lee')).toBeInTheDocument()
    expect(within(row).getByText('ann1@example.com')).toBeInTheDocument()
    expect(screen.getByLabelText(/Search by book id/)).toHaveFocus()
    expect(fetchBorrowedBooks).toHaveBeenCalledWith({ token: 'test-token', page: 0, size: 5, q: '' })
  })

  it('says so when no books are out', async () => {
    vi.mocked(fetchBorrowedBooks).mockResolvedValue(page([]))

    renderPage()

    expect(await screen.findByText('No books are out right now.')).toBeInTheDocument()
  })

  it('searches on submit (Enter, as a barcode scanner sends it) and keeps the text in the URL', async () => {
    vi.mocked(fetchBorrowedBooks).mockResolvedValue(page([borrowedBook('1', 'Dune')]))
    const user = userEvent.setup()
    renderPage('/admin/returns?page=3')

    await user.type(await screen.findByLabelText(/Search by book id/), '  book-1  {Enter}')

    await waitFor(() =>
      expect(fetchBorrowedBooks).toHaveBeenLastCalledWith({ token: 'test-token', page: 0, size: 5, q: 'book-1' }),
    )
    expect(screen.getByTestId('url')).toHaveTextContent('/admin/returns?q=book-1')
  })

  it('tells the admin when a search finds nothing', async () => {
    vi.mocked(fetchBorrowedBooks).mockResolvedValue(page([]))

    renderPage('/admin/returns?q=zzz')

    expect(await screen.findByText('No borrowed books match “zzz”.')).toBeInTheDocument()
  })

  it('moves to the next page and keeps it in the URL', async () => {
    vi.mocked(fetchBorrowedBooks).mockImplementation(async ({ page: p }) =>
      page([borrowedBook(String(p), `Borrowed book on page ${p}`)], { page: p, totalElements: 6, totalPages: 2 }),
    )
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /next/i }))

    expect(await screen.findByText('Borrowed book on page 1')).toBeInTheDocument()
    expect(screen.getByTestId('url')).toHaveTextContent('/admin/returns?page=2')
  })

  it('asks for confirmation naming the book and borrower, then returns it with the existing endpoint', async () => {
    vi.mocked(fetchBorrowedBooks).mockResolvedValue(page([borrowedBook('1', 'Dune')]))
    vi.mocked(returnBook).mockResolvedValue(null)
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Mark “Dune” as returned' }))
    const dialog = screen.getByRole('dialog')
    expect(dialog).toHaveTextContent('“Dune” was borrowed by Ann Lee')
    expect(returnBook).not.toHaveBeenCalled()

    vi.mocked(fetchBorrowedBooks).mockResolvedValue(page([]))
    await user.click(within(dialog).getByRole('button', { name: 'Mark returned' }))

    await waitFor(() =>
      expect(returnBook).toHaveBeenCalledWith({ token: 'test-token', borrowerId: 'borrower-1', bookId: 'book-1' }),
    )
    expect(await screen.findByText('“Dune” is back on the shelf.')).toBeInTheDocument()
    expect(await screen.findByText('No books are out right now.')).toBeInTheDocument()
    expect(screen.getByLabelText(/Search by book id/)).toHaveFocus()
  })

  it('returns nothing when the admin cancels', async () => {
    vi.mocked(fetchBorrowedBooks).mockResolvedValue(page([borrowedBook('1', 'Dune')]))
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Mark “Dune” as returned' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(returnBook).not.toHaveBeenCalled()
  })

  it('explains a 409 (already returned) and reloads the list', async () => {
    vi.mocked(fetchBorrowedBooks).mockResolvedValue(page([borrowedBook('1', 'Dune')]))
    vi.mocked(returnBook).mockRejectedValue(new ApiError(409, 'Book was not borrowed by this borrower'))
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Mark “Dune” as returned' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Mark returned' }))

    expect(await screen.findByText(/already returned/)).toBeInTheDocument()
    await waitFor(() => expect(fetchBorrowedBooks).toHaveBeenCalledTimes(2))
  })

  it('shows an error with a retry button when loading fails', async () => {
    vi.mocked(fetchBorrowedBooks).mockRejectedValueOnce(new ApiError(500, 'boom'))
    vi.mocked(fetchBorrowedBooks).mockResolvedValue(page([borrowedBook('1', 'Dune')]))
    const user = userEvent.setup()
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not load borrowed books: boom')
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByText('Dune')).toBeInTheDocument()
  })
})
