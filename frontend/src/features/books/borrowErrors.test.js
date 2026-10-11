import { describe, expect, it } from 'vitest'
import { borrowErrorMessage } from './borrowErrors.js'

describe('borrowErrorMessage', () => {
  it('explains the item limit with the configured number', () => {
    expect(borrowErrorMessage({ status: 409, message: 'BORROW_LIMIT_REACHED' }, 3)).toMatch(/limit of 3 books/)
  })

  it('explains a book that was taken meanwhile', () => {
    expect(borrowErrorMessage({ status: 409, message: 'BOOK_NOT_AVAILABLE' }, 3)).toMatch(/Someone else/)
  })

  it('does not show an unknown 409 body to the user', () => {
    expect(borrowErrorMessage({ status: 409, message: 'Data integrity violation' }, 3)).toBe(
      'This book cannot be borrowed right now.',
    )
  })

  it.each([
    [404, /could not be found/],
    [403, /not allowed/],
    [401, /session has expired/],
    [500, /Borrowing failed/],
  ])('maps status %s to a readable message', (status, pattern) => {
    expect(borrowErrorMessage({ status, message: 'raw' }, 3)).toMatch(pattern)
  })

  it('passes the network error text through (status 0)', () => {
    expect(borrowErrorMessage({ status: 0, message: 'Cannot reach the library service.' }, 3)).toBe(
      'Cannot reach the library service.',
    )
  })
})
