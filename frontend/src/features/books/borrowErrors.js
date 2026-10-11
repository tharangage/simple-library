/**
 * Turns a failed borrow call into a sentence for the user.
 * The backend answers 409 with a plain code as the body (see BookBorrowerService); anything unknown
 * falls back to the status or the error text, never a raw stack or JSON.
 * @param {{ status?: number, message?: string }} error
 * @param {number} maxItems  the limit, for the "reached the limit" message
 */
export function borrowErrorMessage(error, maxItems) {
  switch (error.status) {
    case 409:
      if (error.message === 'BORROW_LIMIT_REACHED') {
        return `You have reached the limit of ${maxItems} books. Return a book at the library desk to borrow another.`
      }
      if (error.message === 'BOOK_NOT_AVAILABLE') {
        return 'Someone else has just borrowed this book. The list has been refreshed.'
      }
      return 'This book cannot be borrowed right now.'
    case 404:
      return 'This book or your borrower record could not be found. Please reload the page.'
    case 403:
      return 'You are not allowed to borrow this book.'
    case 401:
      return 'Your session has expired. Please log in again.'
    case 0:
      return error.message
    default:
      return 'Borrowing failed. Please try again.'
  }
}
