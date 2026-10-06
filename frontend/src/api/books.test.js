import { describe, expect, it } from 'vitest'
import { normalizePage } from './books.js'

describe('normalizePage', () => {
  it('reads the current Spring PageImpl shape', () => {
    const body = { content: [{ id: '1' }], number: 2, size: 20, totalElements: 41, totalPages: 3 }
    expect(normalizePage(body)).toEqual({ items: [{ id: '1' }], page: 2, size: 20, totalElements: 41, totalPages: 3 })
  })

  it('reads the planned PagedModel shape', () => {
    const body = { content: [], page: { number: 0, size: 20, totalElements: 0, totalPages: 0 } }
    expect(normalizePage(body)).toEqual({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
  })
})
