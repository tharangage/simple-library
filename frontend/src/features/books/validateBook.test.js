import { describe, expect, it } from 'vitest'
import { validateBook } from './validateBook.js'

const valid = { isbn: '978-955-675-510-7', title: 'Dune', author: 'Frank Herbert' }

describe('validateBook', () => {
  it('accepts a valid book', () => {
    expect(validateBook(valid)).toEqual({})
  })

  it.each(['978-955-675-510-7', '9789556755107', '0-8044-2957-X', '080442957x', '0134685997'])(
    'accepts the ISBN %s',
    (isbn) => {
      expect(validateBook({ ...valid, isbn })).toEqual({})
    },
  )

  it('requires all three fields', () => {
    expect(validateBook({ isbn: ' ', title: '', author: '  ' })).toEqual({
      isbn: 'ISBN cannot be blank',
      title: 'Title cannot be blank',
      author: 'Author cannot be blank',
    })
  })

  it('rejects an ISBN of the wrong length', () => {
    expect(validateBook({ ...valid, isbn: '12345' }).isbn).toBe('ISBN must be between 10 and 17 characters')
    expect(validateBook({ ...valid, isbn: '----------0134685997' }).isbn).toBe('ISBN must be between 10 and 17 characters')
  })

  it('rejects an ISBN in the wrong format', () => {
    expect(validateBook({ ...valid, isbn: 'abcdefghijk' }).isbn).toBe('ISBN must be in correct format')
    expect(validateBook({ ...valid, isbn: '123456789012' }).isbn).toBe('ISBN must be in correct format')
  })

  it('limits the length of title and author', () => {
    expect(validateBook({ ...valid, title: 'x'.repeat(255) }).title).toMatch(/at most|between 1 and 254/)
    expect(validateBook({ ...valid, author: 'x'.repeat(129) }).author).toMatch(/between 1 and 128/)
    expect(validateBook({ ...valid, title: 'x'.repeat(254), author: 'x'.repeat(128) })).toEqual({})
  })
})
