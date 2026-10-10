import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiGet, apiPost } from './client.js'

const jsonResponse = (body, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

describe('apiGet', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('calls the API base URL with the bearer token and skips empty parameters', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ ok: true }))
    vi.stubGlobal('fetch', fetchMock)

    const body = await apiGet('/apis/v1/books', { token: 't0k', params: { page: 0, size: 20, sort: '', q: undefined } })

    expect(body).toEqual({ ok: true })
    const [url, options] = fetchMock.mock.calls[0]
    expect(url.toString()).toBe('http://api.test/apis/v1/books?page=0&size=20')
    expect(options.headers.Authorization).toBe('Bearer t0k')
  })

  it('sends no Authorization header without a token', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({}))
    vi.stubGlobal('fetch', fetchMock)

    await apiGet('/apis/v1/books')

    expect(fetchMock.mock.calls[0][1].headers).not.toHaveProperty('Authorization')
  })

  it('turns a plain-text error body into an ApiError (today’s backend format)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('Book not found', { status: 404 })))

    await expect(apiGet('/x')).rejects.toMatchObject({ name: 'ApiError', status: 404, message: 'Book not found' })
  })

  it('reads `detail` from an RFC 9457 problem response (planned format, US-08.1)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ title: 'Conflict', detail: 'Limit reached' }, 409)))

    await expect(apiGet('/x')).rejects.toMatchObject({ status: 409, message: 'Limit reached' })
  })

  it('falls back to the HTTP status when the error body is empty', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 403 })))

    await expect(apiGet('/x')).rejects.toMatchObject({ status: 403, message: 'HTTP 403' })
  })

  it('reports status 0 when no response arrives (server down or CORS blocked)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    const error = await apiGet('/x').catch((e) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(0)
    expect(error.message).toMatch(/CORS/)
  })
})

describe('apiPost', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('sends the body as JSON, without a token unless one is given', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: '1' }, 201))
    vi.stubGlobal('fetch', fetchMock)

    const body = await apiPost('/apis/v1/registrations', { email: 'a@b.co' })

    expect(body).toEqual({ id: '1' })
    const [url, options] = fetchMock.mock.calls[0]
    expect(url.toString()).toBe('http://api.test/apis/v1/registrations')
    expect(options.method).toBe('POST')
    expect(options.headers['Content-Type']).toBe('application/json')
    expect(options.headers).not.toHaveProperty('Authorization')
    expect(JSON.parse(options.body)).toEqual({ email: 'a@b.co' })
  })

  it('sends the bearer token when given', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({}))
    vi.stubGlobal('fetch', fetchMock)

    await apiPost('/x', {}, { token: 't0k' })

    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer t0k')
  })

  it('returns null for 204 No Content', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })))
    expect(await apiPost('/x', {})).toBeNull()
  })

  it('exposes a 400 validation map as fieldErrors', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ email: 'Email must be valid' }, 400)))

    const error = await apiPost('/x', {}).catch((e) => e)

    expect(error.status).toBe(400)
    expect(error.fieldErrors).toEqual({ email: 'Email must be valid' })
  })

  it('keeps the plain-text error code of a 409', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('EMAIL_ALREADY_REGISTERED', { status: 409 })))

    const error = await apiPost('/x', {}).catch((e) => e)

    expect(error).toMatchObject({ status: 409, message: 'EMAIL_ALREADY_REGISTERED', fieldErrors: {} })
  })
})
