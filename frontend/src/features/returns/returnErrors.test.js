import { describe, expect, it } from 'vitest'
import { returnErrorMessage } from './returnErrors.js'

describe('returnErrorMessage', () => {
  it.each([
    [409, /already returned/],
    [404, /could not be found/],
    [403, /Only library administrators/],
    [401, /session has expired/],
    [500, /Returning the book failed/],
  ])('explains status %s', (status, expected) => {
    expect(returnErrorMessage({ status, message: 'raw backend text' })).toMatch(expected)
  })

  it('passes on the message when the backend cannot be reached', () => {
    expect(returnErrorMessage({ status: 0, message: 'Cannot reach the library service.' })).toBe(
      'Cannot reach the library service.',
    )
  })
})
