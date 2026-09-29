import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

type GoogleLoginHandler = (typeof import('./google.get'))['default']

const runtimeConfig = vi.hoisted(() => ({
  googleOidc: {
    clientId: 'client-id',
    clientSecret: 'client-secret',
    redirectUri: 'http://localhost:3000/auth/google/callback',
  },
}))

const setCookieMock = vi.hoisted(() => vi.fn())

vi.mock('#imports', () => ({
  useRuntimeConfig: () => runtimeConfig,
}))

vi.mock('nuxt/app', () => ({
  useRuntimeConfig: () => runtimeConfig,
}))

vi.mock('#app/nuxt', () => ({
  useRuntimeConfig: () => runtimeConfig,
}))

describe('Google OAuth start route', () => {
  let handler: GoogleLoginHandler
  let queryValue: Record<string, unknown>

  beforeEach(async () => {
    vi.resetModules()
    queryValue = {}
    setCookieMock.mockReset()
    vi.stubGlobal('defineEventHandler', (callback: GoogleLoginHandler) => callback)
    vi.stubGlobal('useRuntimeConfig', () => runtimeConfig)
    vi.stubGlobal('getQuery', () => queryValue)
    vi.stubGlobal('setCookie', setCookieMock)
    vi.stubGlobal('sendRedirect', (_event: unknown, location: string) => ({ location }))
    vi.stubGlobal('createError', (details: { statusCode: number; statusMessage: string }) => new Error(`${details.statusCode}: ${details.statusMessage}`))
    handler = (await import('./google.get')).default
  })

  afterEach(() => {
    vi.resetAllMocks()
    vi.unstubAllGlobals()
  })

  const storedRedirect = () => setCookieMock.mock.calls.find(call => call[1] === 'oidc_redirect')?.[2]

  it('keeps a same-origin relative redirect', async () => {
    queryValue = { redirect: '/editor' }
    await handler({} as Parameters<GoogleLoginHandler>[0])
    expect(storedRedirect()).toBe('/editor')
  })

  it('falls back to / for a protocol-relative redirect', async () => {
    queryValue = { redirect: '//evil.example.com' }
    await handler({} as Parameters<GoogleLoginHandler>[0])
    expect(storedRedirect()).toBe('/')
  })

  it('falls back to / for a backslash-based redirect', async () => {
    queryValue = { redirect: '/\\evil.example.com' }
    await handler({} as Parameters<GoogleLoginHandler>[0])
    expect(storedRedirect()).toBe('/')
  })

  it('falls back to / for an absolute URL redirect', async () => {
    queryValue = { redirect: 'https://evil.example.com' }
    await handler({} as Parameters<GoogleLoginHandler>[0])
    expect(storedRedirect()).toBe('/')
  })
})
