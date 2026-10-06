import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client.js'
import { fetchBooks } from '../../api/books.js'
import { BooksPage } from './BooksPage.jsx'

// Replace the real login library and the real HTTP call with test doubles.
const signinRedirect = vi.fn()
vi.mock('react-oidc-context', () => ({
  useAuth: () => ({ user: { access_token: 'test-token' }, signinRedirect }),
}))
vi.mock('../../api/books.js', () => ({ fetchBooks: vi.fn() }))

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
      <MemoryRouter initialEntries={[url]}>
        <BooksPage />
        <CurrentUrl />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('BooksPage', () => {
  beforeEach(() => {
    vi.mocked(fetchBooks).mockReset()
    signinRedirect.mockReset()
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
    expect(fetchBooks).toHaveBeenCalledWith({ token: 'test-token', page: 0, size: 20, sort: 'title,asc' })
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
