/**
 * Turns a failed return call into a sentence for the admin.
 * The backend answers 409 when the book is not (or no longer) borrowed by that borrower.
 * @param {{ status?: number, message?: string }} error
 */
export function returnErrorMessage(error) {
  switch (error.status) {
    case 409:
      return 'This book was already returned, or is borrowed by someone else. The list has been refreshed.'
    case 404:
      return 'This book or borrower could not be found. The list has been refreshed.'
    case 403:
      return 'Only library administrators can return books.'
    case 401:
      return 'Your session has expired. Please log in again.'
    case 0:
      return error.message
    default:
      return 'Returning the book failed. Please try again.'
  }
}
