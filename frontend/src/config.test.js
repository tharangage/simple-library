import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// config.js reads its values once, when it is first imported, so each test imports a fresh copy.
const loadConfig = async () => {
  vi.resetModules()
  return (await import('./config.js')).config
}

describe('config', () => {
  beforeEach(() => {
    delete window.APP_CONFIG
  })
  afterEach(() => {
    delete window.APP_CONFIG
    vi.unstubAllEnvs()
  })

  it('uses the build-time VITE_* values when there is no runtime config', async () => {
    const config = await loadConfig()

    expect(config.apiBaseUrl).toBe('http://api.test')
    expect(config.keycloakUrl).toBe('http://keycloak.test')
  })

  it('lets the runtime config (written by the Docker image) win over the build-time values', async () => {
    window.APP_CONFIG = { VITE_API_BASE_URL: 'https://api.example.org' }

    const config = await loadConfig()

    expect(config.apiBaseUrl).toBe('https://api.example.org')
    expect(config.keycloakUrl).toBe('http://keycloak.test')
  })

  it('ignores an empty runtime value and falls back to the build-time one', async () => {
    window.APP_CONFIG = { VITE_KEYCLOAK_REALM: '' }

    expect((await loadConfig()).keycloakRealm).toBe('library')
  })

  it('names the missing setting when neither source has it', async () => {
    vi.stubEnv('VITE_API_BASE_URL', '')

    await expect(loadConfig()).rejects.toThrow(/Missing VITE_API_BASE_URL/)
  })
})
