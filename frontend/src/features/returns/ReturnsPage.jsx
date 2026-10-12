import { useRef, useState } from 'react'
import { useSearchParams } from 'react-router'
import { ConfirmDialog } from '../../components/ConfirmDialog.jsx'
import { Pagination } from '../../components/Pagination.jsx'
import { useBorrowedBooks } from './useBorrowedBooks.js'
import { useReturnBook } from './useReturnBook.js'

export const PAGE_SIZE = 5

/** Page and search text live in the URL (/admin/returns?q=dune&page=2), like the Books page. */
function readParams(searchParams) {
  const page = Number.parseInt(searchParams.get('page') ?? '1', 10)
  return { page: Number.isInteger(page) && page >= 1 ? page : 1, q: searchParams.get('q') ?? '' }
}

const formatDate = (value) => (value ? new Date(value).toLocaleDateString() : '')
const borrowerName = (borrowedBook) =>
  [borrowedBook.borrowerFirstname, borrowedBook.borrowerLastname].filter(Boolean).join(' ') || borrowedBook.borrowerEmail

/**
 * Library desk (admin): find a current borrowedBook and mark it as returned.
 * Focus starts in the search box so a barcode scanner that types the book id and presses Enter works.
 */
export function ReturnsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { page, q } = readParams(searchParams)
  const { data, isPending, isError, error, refetch, isFetching, isPlaceholderData } = useBorrowedBooks({
    page: page - 1, // the URL is 1-based for people; Spring Data is 0-based
    size: PAGE_SIZE,
    q,
  })
  const returnBook = useReturnBook()
  const [bookToReturn, setBookToReturn] = useState(null)
  const searchRef = useRef(null)

  const search = (event) => {
    event.preventDefault()
    const text = new FormData(event.currentTarget).get('q').trim()
    setSearchParams(text ? { q: text } : {})
  }

  const goToPage = (newPage) => {
    const next = new URLSearchParams(searchParams)
    next.set('page', String(newPage))
    setSearchParams(next)
  }

  const confirmReturn = () => {
    const borrowedBook = bookToReturn
    setBookToReturn(null)
    // The returned row disappears, so keyboard users continue in the search box (ready for the next scan).
    returnBook.mutate(borrowedBook, { onSuccess: () => searchRef.current?.focus() })
  }

  return (
    <section className="returns">
      <header className="page-header">
        <h1>Returns</h1>
        <p className="muted">Books that are out. Mark one as returned when it is handed back at the desk.</p>
      </header>

      {/* key: a new search from the URL (Back button) resets the box to match */}
      <form role="search" className="search-form" onSubmit={search} key={q}>
        <label htmlFor="borrowedBook-search">Search by book id, ISBN, title or borrower email</label>
        <div className="search-form__row">
          <input id="borrowedBook-search" name="q" type="search" defaultValue={q} ref={searchRef} autoFocus />
          <button type="submit" className="button--primary">
            Search
          </button>
        </div>
      </form>

      {isPending && (
        <p role="status" className="message">
          Loading borrowed books…
        </p>
      )}

      {isError && (
        <div role="alert" className="message message--error">
          {error.status === 403
            ? 'Only library administrators can see borrowed books.'
            : `Could not load borrowed books: ${error.message}`}{' '}
          <button type="button" onClick={() => refetch()}>
            Try again
          </button>
        </div>
      )}

      {data && data.totalElements === 0 && (
        <p className="message">{q ? `No borrowed books match “${q}”.` : 'No books are out right now.'}</p>
      )}

      {data && data.totalElements > 0 && data.items.length === 0 && (
        <p className="message">
          There are no borrowed books on page {page}.{' '}
          <button type="button" className="link-button" onClick={() => goToPage(1)}>
            Go to the first page
          </button>
        </p>
      )}

      {data && data.items.length > 0 && (
        <>
          <table className="book-table" aria-busy={isFetching && isPlaceholderData}>
            <thead>
              <tr>
                <th scope="col">Title</th>
                <th scope="col">Borrower</th>
                <th scope="col">Borrowed on</th>
                <th scope="col" className="book-table__action">
                  <span className="visually-hidden">Action</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((borrowedBook) => (
                <tr key={borrowedBook.bookId}>
                  <td data-label="Title" className="book-table__title">
                    {borrowedBook.title}
                    <div className="muted mono">{borrowedBook.isbn}</div>
                  </td>
                  <td data-label="Borrower">
                    {borrowerName(borrowedBook)}
                    <div className="muted">{borrowedBook.borrowerEmail}</div>
                  </td>
                  <td data-label="Borrowed on">{formatDate(borrowedBook.borrowedDate)}</td>
                  <td className="book-table__action">
                    <button
                      type="button"
                      disabled={returnBook.isPending}
                      onClick={() => setBookToReturn(borrowedBook)}
                      aria-label={`Mark “${borrowedBook.title}” as returned`}
                    >
                      Mark returned
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pagination
            page={page}
            totalPages={data.totalPages}
            onPageChange={goToPage}
            disabled={isFetching && isPlaceholderData}
          />
        </>
      )}

      <ConfirmDialog
        open={bookToReturn !== null}
        title="Mark this book as returned?"
        confirmLabel="Mark returned"
        onConfirm={confirmReturn}
        onCancel={() => setBookToReturn(null)}
      >
        {bookToReturn && (
          <p>
            “{bookToReturn.title}” was borrowed by {borrowerName(bookToReturn)}. It becomes available again.
          </p>
        )}
      </ConfirmDialog>
    </section>
  )
}
