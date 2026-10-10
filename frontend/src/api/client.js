import { config } from '../config.js'

/** An HTTP error from the backend, with the status code kept for the UI to decide what to show. */
export class ApiError extends Error {
  /** @param fieldErrors  {field: message} from a 400 validation response, otherwise empty */
  constructor(status, message, fieldErrors = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

/** GET a JSON resource. The token is optional (public endpoints need none). */
export function apiGet(path, { token, params } = {}) {
  return request('GET', path, { token, params })
}

/** POST a JSON body and read the JSON answer. The token is optional (registration needs none). */
export function apiPost(path, body, { token } = {}) {
  return request('POST', path, { token, body })
}

/**
 * The single place where the app talks to the backend.
 * Adds the base URL and the bearer token, and turns non-2xx responses into ApiError.
 */
async function request(method, path, { token, params, body } = {}) {
  const url = new URL(path, config.apiBaseUrl)
  for (const [key, value] of Object.entries(params ?? {})) {
    if (value !== undefined && value !== null && value !== '') url.searchParams.set(key, value)
  }

  let response
  try {
    response = await fetch(url, {
      method,
      headers: {
        Accept: 'application/json',
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
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
    const { message, fieldErrors } = await readError(response)
    throw new ApiError(response.status, message, fieldErrors)
  }
  return response.status === 204 ? null : response.json()
}

/**
 * Backend errors come in three shapes:
 *  - plain text (404/409/500 today, e.g. "EMAIL_ALREADY_REGISTERED")
 *  - RFC 9457 JSON with `detail` (planned, US-08.1)
 *  - a 400 validation map {field: message}, which becomes `fieldErrors` so forms can show each one
 */
async function readError(response) {
  const text = await response.text()
  let body
  try {
    body = JSON.parse(text)
  } catch {
    return { message: text || response.statusText || `HTTP ${response.status}` }
  }
  if (body && typeof body === 'object') {
    const message = body.detail ?? body.message ?? body.title
    if (message) return { message }
    const values = Object.values(body)
    if (response.status === 400 && values.length > 0 && values.every((v) => typeof v === 'string')) {
      return { message: 'Some fields are invalid.', fieldErrors: body }
    }
  }
  return { message: text }
}
