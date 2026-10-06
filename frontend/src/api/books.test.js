import { describe, expect, it, vi } from 'vitest'
import { apiGet } from './client.js'
import { fetchBooks, normalizePage } from './books.js'

vi.mock('./client.js', () => ({ apiGet: vi.fn() }))

describe('fetchBooks', () => {
  it('asks for one page of books and returns it normalised', async () => {
    vi.mocked(apiGet).mockResolvedValue({ content: [{ id: '1' }], number: 0, size: 20, totalElements: 1, totalPages: 1 })

    const page = await fetchBooks({ token: 't', page: 0, size: 20, sort: 'title,asc' })

    expect(apiGet).toHaveBeenCalledWith('/apis/v1/books', { token: 't', params: { page: 0, size: 20, sort: 'title,asc' } })
    expect(page.items).toEqual([{ id: '1' }])
  })
})

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
