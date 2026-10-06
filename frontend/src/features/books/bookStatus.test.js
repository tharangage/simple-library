import { describe, expect, it } from 'vitest'
import { BOOK_STATUS, getBookStatus } from './bookStatus.js'

describe('getBookStatus', () => {
  it('uses the borrowed flag the backend sends today', () => {
    expect(getBookStatus({ borrowed: false })).toBe(BOOK_STATUS.AVAILABLE)
    expect(getBookStatus({ borrowed: true })).toBe(BOOK_STATUS.BORROWED)
  })

  it('prefers the explicit status once the backend sends one', () => {
    expect(getBookStatus({ borrowed: false, status: 'ON_HOLD' })).toBe(BOOK_STATUS.ON_HOLD)
  })

  it('ignores a status it does not know', () => {
    expect(getBookStatus({ borrowed: true, status: 'LOST' })).toBe(BOOK_STATUS.BORROWED)
  })

  it('shows statuses about the current user first', () => {
    expect(getBookStatus({ borrowed: true, borrowedByMe: true })).toBe(BOOK_STATUS.BORROWED_BY_ME)
    expect(getBookStatus({ status: 'ON_HOLD', readyForMe: true })).toBe(BOOK_STATUS.READY_FOR_ME)
    expect(getBookStatus({ borrowed: true, reservedByMe: true })).toBe(BOOK_STATUS.RESERVED_BY_ME)
  })
})
