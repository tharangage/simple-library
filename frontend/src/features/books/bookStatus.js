/**
 * Every status the Books page knows how to show (story US-03.1).
 * Today the backend only sends `borrowed: true|false`, so only AVAILABLE and BORROWED appear.
 * The other statuses light up automatically once the backend adds the fields from US-03.2.
 */
export const BOOK_STATUS = Object.freeze({
  AVAILABLE: 'AVAILABLE',
  BORROWED: 'BORROWED',
  ON_HOLD: 'ON_HOLD',
  BORROWED_BY_ME: 'BORROWED_BY_ME',
  RESERVED_BY_ME: 'RESERVED_BY_ME',
  READY_FOR_ME: 'READY_FOR_ME',
})

export const STATUS_LABELS = Object.freeze({
  AVAILABLE: 'Available',
  BORROWED: 'Borrowed',
  ON_HOLD: 'On hold',
  BORROWED_BY_ME: 'Borrowed by you',
  RESERVED_BY_ME: 'Reserved by you',
  READY_FOR_ME: 'Ready for you',
})

/** Work out the one status to show for a book. "About me" statuses win over general ones. */
export function getBookStatus(book) {
  if (book.borrowedByMe) return BOOK_STATUS.BORROWED_BY_ME
  if (book.readyForMe) return BOOK_STATUS.READY_FOR_ME
  if (book.reservedByMe) return BOOK_STATUS.RESERVED_BY_ME
  if (book.status && STATUS_LABELS[book.status]) return book.status
  return book.borrowed ? BOOK_STATUS.BORROWED : BOOK_STATUS.AVAILABLE
}
