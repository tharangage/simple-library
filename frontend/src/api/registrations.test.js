import { afterEach, describe, expect, it, vi } from 'vitest'
import { registerUser } from './registrations.js'

describe('registerUser', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('posts only the five registration fields (never the confirm-password field)', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('{"id":"1"}', { status: 201 }))
    vi.stubGlobal('fetch', fetchMock)

    await registerUser({
      firstName: 'Ann',
      lastName: 'Lee',
      email: 'ann@example.com',
      mobile: '+14155550100',
      password: 'secret-pass',
      confirmPassword: 'secret-pass',
    })

    const [url, options] = fetchMock.mock.calls[0]
    expect(url.toString()).toBe('http://api.test/apis/v1/registrations')
    expect(JSON.parse(options.body)).toEqual({
      firstName: 'Ann',
      lastName: 'Lee',
      email: 'ann@example.com',
      mobile: '+14155550100',
      password: 'secret-pass',
    })
  })
})
