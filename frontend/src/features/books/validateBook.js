/**
 * Client-side checks for the "Add book" form. They mirror the backend rules (BookCreateRequest), so the user
 * sees problems before sending; the backend still validates everything again.
 * Returns {field: message} for invalid fields only (empty object = valid).
 */

// ISBN-10 (last character may be X) or ISBN-13, digits optionally separated by hyphens. Same pattern as the backend.
const ISBN = /^(?:(?=(?:[^0-9]*[0-9]){9}[^0-9]*[0-9Xx]$)[\d-]+[\dXx]|(?=(?:[^0-9]*[0-9]){13}$)[\d-]+)$/

export function validateBook({ isbn, title, author }) {
  const errors = {}

  const code = isbn.trim()
  if (!code) errors.isbn = 'ISBN cannot be blank'
  else if (code.length < 10 || code.length > 17) errors.isbn = 'ISBN must be between 10 and 17 characters'
  else if (!ISBN.test(code)) errors.isbn = 'ISBN must be in correct format'

  const name = title.trim()
  if (!name) errors.title = 'Title cannot be blank'
  else if (name.length > 254) errors.title = 'Title must be between 1 and 254 characters'

  const writer = author.trim()
  if (!writer) errors.author = 'Author cannot be blank'
  else if (writer.length > 128) errors.author = 'Author must be between 1 and 128 characters'

  return errors
}
