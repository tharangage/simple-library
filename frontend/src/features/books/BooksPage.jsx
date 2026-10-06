import { useAuth } from 'react-oidc-context'
import { useSearchParams } from 'react-router'
import { Pagination } from '../../components/Pagination.jsx'
import { getBookStatus } from './bookStatus.js'
import { StatusBadge } from './StatusBadge.jsx'
import { useBooks } from './useBooks.js'

export const PAGE_SIZE = 20

// Columns the user may sort by. Anything else in the URL is ignored, because the backend
// answers 400 for unknown sort fields.
const SORTABLE_COLUMNS = { title: 'Title', author: 'Author', isbn: 'ISBN' }
const DEFAULT_SORT = { field: 'title', direction: 'asc' }

/**
 * Page and sort live in the URL (e.g. /books?page=2&sort=author,desc), not in component state.
 * That way reload, the Back button and shared links all show the same page.
 */
function readListParams(searchParams) {
  const page = Number.parseInt(searchParams.get('page') ?? '1', 10)
  const [field, direction] = (searchParams.get('sort') ?? '').split(',')
  const validSort = field in SORTABLE_COLUMNS && ['asc', 'desc'].includes(direction)
  return {
    page: Number.isInteger(page) && page > 0 ? page : 1,
    sort: validSort ? { field, direction } : DEFAULT_SORT,
  }
}

export function BooksPage() {
  const auth = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const { page, sort } = readListParams(searchParams)

  const { data, isPending, isError, error, refetch, isFetching, isPlaceholderData } = useBooks({
    page: page - 1, // the URL is 1-based for people; Spring Data is 0-based
    size: PAGE_SIZE,
    sort: `${sort.field},${sort.direction}`,
  })

  const goToPage = (newPage) => {
    const next = new URLSearchParams(searchParams)
    next.set('page', String(newPage))
    setSearchParams(next)
  }

  const sortBy = (field) => {
    const direction = sort.field === field && sort.direction === 'asc' ? 'desc' : 'asc'
    // A new sort order starts again from the first page.
    setSearchParams({ sort: `${field},${direction}` })
  }

  return (
    <section className="books">
      <header className="page-header">
        <h1>Books</h1>
        {data && data.totalElements > 0 && (
          <p className="muted" aria-live="polite">
            {describeRange(data)}
          </p>
        )}
      </header>

      {isPending && (
        <p role="status" className="message">
          Loading books…
        </p>
      )}

      {isError && <BooksError error={error} onRetry={() => refetch()} onLogin={() => auth.signinRedirect()} />}

      {data && data.totalElements === 0 && (
        <p className="message">No books yet. Books appear here once the library registers them.</p>
      )}

      {data && data.totalElements > 0 && data.items.length === 0 && (
        <p className="message">
          There are no books on page {page}.{' '}
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
                {Object.entries(SORTABLE_COLUMNS).map(([field, label]) => (
                  <SortableHeader key={field} field={field} label={label} sort={sort} onSort={sortBy} />
                ))}
                <th scope="col">Status</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((book) => (
                <tr key={book.id}>
                  <td data-label="Title" className="book-table__title">
                    {book.title}
                  </td>
                  <td data-label="Author">{book.author}</td>
                  <td data-label="ISBN" className="mono">
                    {book.isbn}
                  </td>
                  <td data-label="Status">
                    <StatusBadge status={getBookStatus(book)} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pagination
            page={page}
            totalPages={data.totalPages}
            onPageChange={goToPage}
            disabled={isFetching}
          />
        </>
      )}
    </section>
  )
}

function SortableHeader({ field, label, sort, onSort }) {
  const active = sort.field === field
  const ariaSort = active ? (sort.direction === 'asc' ? 'ascending' : 'descending') : 'none'
  return (
    <th scope="col" aria-sort={ariaSort}>
      <button type="button" className="sort-button" onClick={() => onSort(field)}>
        {label}
        <span aria-hidden="true">{active ? (sort.direction === 'asc' ? ' ▲' : ' ▼') : ''}</span>
      </button>
    </th>
  )
}

function BooksError({ error, onRetry, onLogin }) {
  if (error.status === 401) {
    return (
      <div role="alert" className="message message--error">
        Your session has expired.{' '}
        <button type="button" onClick={onLogin}>
          Log in again
        </button>
      </div>
    )
  }
  return (
    <div role="alert" className="message message--error">
      Could not load books: {error.message}{' '}
      <button type="button" onClick={onRetry}>
        Try again
      </button>
    </div>
  )
}

function describeRange({ page, size, items, totalElements }) {
  const first = page * size + 1
  const last = first + items.length - 1
  const noun = totalElements === 1 ? 'book' : 'books'
  return items.length ? `Showing ${first}–${last} of ${totalElements} ${noun}` : `${totalElements} ${noun}`
}
