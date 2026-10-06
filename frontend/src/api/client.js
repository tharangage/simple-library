import { config } from '../config.js'

/** An HTTP error from the backend, with the status code kept for the UI to decide what to show. */
export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

/**
 * The single place where the app talks to the backend.
 * Adds the base URL and the bearer token, and turns non-2xx responses into ApiError.
 */
export async function apiGet(path, { token, params } = {}) {
  const url = new URL(path, config.apiBaseUrl)
  for (const [key, value] of Object.entries(params ?? {})) {
    if (value !== undefined && value !== null && value !== '') url.searchParams.set(key, value)
  }

  let response
  try {
    response = await fetch(url, {
      headers: {
        Accept: 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    })
  } catch {
    // fetch only rejects when the browser got no usable response: server down, offline, or the
    // CORS check failed (the browser hides the real status, e.g. a 403 preflight, from JavaScript).
    throw new ApiError(
      0,
      'Cannot reach the library service. Check that the backend is running and allows this origin (CORS).',
    )
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response))
  }
  return response.json()
}

/** Today the backend sends errors as plain text; later as RFC 9457 JSON (story US-08.1). Accept both. */
async function readErrorMessage(response) {
  const text = await response.text()
  try {
    const body = JSON.parse(text)
    return body.detail ?? body.message ?? body.title ?? text
  } catch {
    return text || response.statusText || `HTTP ${response.status}`
  }
}
