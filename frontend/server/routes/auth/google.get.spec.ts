import { beforeEach, describe, expect, it, vi } from 'vitest'

import { GOOGLE_SSO_PENDING_COOKIE } from '../../utils/google-sso'

type GoogleGetHandler = (typeof import('./google.get'))['default']

const runtimeConfig = vi.hoisted(() => ({
  public: {
    googleSsoEnabled: true,
    tokenCookieName: 'access_token',
    refreshCookieName: 'refresh_token',
  },
  googleOAuthClientId: 'client-123',
  googleOAuthRedirectUri: 'http://127.0.0.1:4100/auth/google/callback',
  apiUrl: 'http://localhost:8082',
}))

const getQueryMock = vi.hoisted(() => vi.fn())
const setCookieMock = vi.hoisted(() => vi.fn())
const sendRedirectMock = vi.hoisted(() => vi.fn())

vi.mock('h3', async importOriginal => ({
  ...(await importOriginal<typeof import('h3')>()),
  getQuery: getQueryMock,
  setCookie: setCookieMock,
  sendRedirect: sendRedirectMock,
}))

vi.mock('#imports', async importOriginal => ({
  ...(await importOriginal<typeof import('h3')>()),
  defineEventHandler: (fn: GoogleGetHandler) => fn,
  getQuery: getQueryMock,
  setCookie: setCookieMock,
  sendRedirect: sendRedirectMock,
  useRuntimeConfig: () => runtimeConfig,
}))

vi.mock('nuxt/app', () => ({ useRuntimeConfig: () => runtimeConfig }))
vi.mock('#app/nuxt', () => ({ useRuntimeConfig: () => runtimeConfig }))

describe('GET /auth/google', () => {
  let handler: GoogleGetHandler

  beforeEach(async () => {
    vi.resetModules()
    getQueryMock.mockReset().mockReturnValue({})
    setCookieMock.mockReset()
    sendRedirectMock.mockReset()
    runtimeConfig.public.googleSsoEnabled = true
    runtimeConfig.googleOAuthClientId = 'client-123'
    runtimeConfig.googleOAuthRedirectUri =
      'http://127.0.0.1:4100/auth/google/callback'
    vi.stubGlobal('defineEventHandler', (fn: GoogleGetHandler) => fn)
    vi.stubGlobal('useRuntimeConfig', () => runtimeConfig)
    vi.stubGlobal('getQuery', getQueryMock)
    vi.stubGlobal('setCookie', setCookieMock)
    vi.stubGlobal('sendRedirect', sendRedirectMock)
    handler = (await import('./google.get')).default
  })

  const fakeEvent = {} as Parameters<GoogleGetHandler>[0]

  it('returns 404 when the SSO flag is disabled', () => {
    runtimeConfig.public.googleSsoEnabled = false

    expect.assertions(3)
    try {
      handler(fakeEvent)
    } catch (err) {
      expect(err).toMatchObject({ statusCode: 404 })
    }
    expect(setCookieMock).not.toHaveBeenCalled()
    expect(sendRedirectMock).not.toHaveBeenCalled()
  })

  it('returns 404 when the OAuth client is not configured', () => {
    runtimeConfig.googleOAuthClientId = ''

    expect.assertions(2)
    try {
      handler(fakeEvent)
    } catch (err) {
      expect(err).toMatchObject({ statusCode: 404 })
    }
    expect(sendRedirectMock).not.toHaveBeenCalled()
  })

  it('stores the pending state and redirects to Google with state, nonce and PKCE challenge', async () => {
    await handler(fakeEvent)

    expect(setCookieMock).toHaveBeenCalledTimes(1)
    const [, cookieName, cookieValue, cookieOptions] =
      setCookieMock.mock.calls[0]
    expect(cookieName).toBe(GOOGLE_SSO_PENDING_COOKIE)
    expect(cookieOptions).toMatchObject({ httpOnly: true, path: '/' })

    const pending = JSON.parse(cookieValue)
    expect(pending.redirect).toBe('/')

    expect(sendRedirectMock).toHaveBeenCalledTimes(1)
    const [, redirectUrl, statusCode] = sendRedirectMock.mock.calls[0]
    expect(statusCode).toBe(302)
    const url = new URL(redirectUrl)
    expect(url.origin + url.pathname).toBe(
      'https://accounts.google.com/o/oauth2/v2/auth'
    )
    expect(url.searchParams.get('client_id')).toBe('client-123')
    expect(url.searchParams.get('state')).toBe(pending.state)
    expect(url.searchParams.get('nonce')).toBe(pending.nonce)
    expect(url.searchParams.get('code_challenge_method')).toBe('S256')
  })

  it('carries a validated same-origin redirect target through the pending state', async () => {
    getQueryMock.mockReturnValue({ redirect: '/account' })

    await handler(fakeEvent)

    const cookieValue = setCookieMock.mock.calls[0][2]
    expect(JSON.parse(cookieValue).redirect).toBe('/account')
  })

  it('falls back to / when the redirect target is an open redirect', async () => {
    getQueryMock.mockReturnValue({ redirect: 'https://evil.example' })

    await handler(fakeEvent)

    const cookieValue = setCookieMock.mock.calls[0][2]
    expect(JSON.parse(cookieValue).redirect).toBe('/')
  })
})
