import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FetchError } from 'ofetch'

import {
  GOOGLE_SSO_PENDING_COOKIE,
  serializePendingState,
  type GoogleSsoPendingState,
} from '../../../utils/google-sso'

type CallbackHandler = (typeof import('./callback.get'))['default']

const runtimeConfig = vi.hoisted(() => ({
  public: {
    googleSsoEnabled: true,
    tokenCookieName: 'access_token',
    refreshCookieName: 'refresh_token',
  },
  googleOAuthClientId: 'client-123',
  googleOAuthRedirectUri: 'http://127.0.0.1:4100/auth/google/callback',
  googleOAuthClientSecret: 'client-secret-xyz',
  apiUrl: 'http://localhost:8082',
}))

const getQueryMock = vi.hoisted(() => vi.fn())
const getCookieMock = vi.hoisted(() => vi.fn())
const deleteCookieMock = vi.hoisted(() => vi.fn())
const setCookieMock = vi.hoisted(() => vi.fn())
const sendRedirectMock = vi.hoisted(() => vi.fn())
const fetchMock = vi.hoisted(() => vi.fn())

vi.mock('h3', async importOriginal => ({
  ...(await importOriginal<typeof import('h3')>()),
  getQuery: getQueryMock,
  getCookie: getCookieMock,
  deleteCookie: deleteCookieMock,
  setCookie: setCookieMock,
  sendRedirect: sendRedirectMock,
}))

vi.mock('#imports', async importOriginal => ({
  ...(await importOriginal<typeof import('h3')>()),
  defineEventHandler: (fn: CallbackHandler) => fn,
  getQuery: getQueryMock,
  getCookie: getCookieMock,
  deleteCookie: deleteCookieMock,
  setCookie: setCookieMock,
  sendRedirect: sendRedirectMock,
  useRuntimeConfig: () => runtimeConfig,
  $fetch: fetchMock,
}))

vi.mock('nuxt/app', () => ({ useRuntimeConfig: () => runtimeConfig }))
vi.mock('#app/nuxt', () => ({ useRuntimeConfig: () => runtimeConfig }))

const VALID_PENDING: GoogleSsoPendingState = {
  state: 'state-abc',
  nonce: 'nonce-xyz',
  codeVerifier: 'verifier-123',
  redirect: '/account',
}

describe('GET /auth/google/callback', () => {
  let handler: CallbackHandler

  beforeEach(async () => {
    vi.resetModules()
    getQueryMock.mockReset().mockReturnValue({ state: 'state-abc', code: 'auth-code' })
    getCookieMock.mockReset().mockReturnValue(serializePendingState(VALID_PENDING))
    deleteCookieMock.mockReset()
    setCookieMock.mockReset()
    sendRedirectMock.mockReset()
    fetchMock.mockReset()
    runtimeConfig.public.googleSsoEnabled = true
    runtimeConfig.googleOAuthClientId = 'client-123'
    runtimeConfig.googleOAuthRedirectUri =
      'http://127.0.0.1:4100/auth/google/callback'
    runtimeConfig.googleOAuthClientSecret = 'client-secret-xyz'

    vi.stubGlobal('defineEventHandler', (fn: CallbackHandler) => fn)
    vi.stubGlobal('useRuntimeConfig', () => runtimeConfig)
    vi.stubGlobal('getQuery', getQueryMock)
    vi.stubGlobal('getCookie', getCookieMock)
    vi.stubGlobal('deleteCookie', deleteCookieMock)
    vi.stubGlobal('setCookie', setCookieMock)
    vi.stubGlobal('sendRedirect', sendRedirectMock)
    vi.stubGlobal('$fetch', fetchMock)

    handler = (await import('./callback.get')).default
  })

  const fakeEvent = {} as Parameters<CallbackHandler>[0]

  it('returns 404 when the SSO flag is disabled', async () => {
    runtimeConfig.public.googleSsoEnabled = false

    await expect(handler(fakeEvent)).rejects.toMatchObject({
      statusCode: 404,
    })
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('returns 500 when the client secret is not configured', async () => {
    runtimeConfig.googleOAuthClientSecret = ''
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    await expect(handler(fakeEvent)).rejects.toMatchObject({
      statusCode: 500,
    })
    expect(fetchMock).not.toHaveBeenCalled()
    expect(consoleErrorSpy.mock.calls.flat().join(' ')).not.toContain(
      'client-secret-xyz'
    )

    consoleErrorSpy.mockRestore()
  })

  it('rejects when the pending cookie is missing or expired', async () => {
    getCookieMock.mockReturnValue(undefined)

    await expect(handler(fakeEvent)).rejects.toMatchObject({
      statusCode: 401,
    })
    expect(deleteCookieMock).toHaveBeenCalledWith(
      fakeEvent,
      GOOGLE_SSO_PENDING_COOKIE,
      { path: '/' }
    )
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('rejects when the state query parameter does not match the pending cookie', async () => {
    getQueryMock.mockReturnValue({ state: 'wrong-state', code: 'auth-code' })

    await expect(handler(fakeEvent)).rejects.toMatchObject({
      statusCode: 401,
    })
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('rejects when Google reports an authorization error or no code', async () => {
    getQueryMock.mockReturnValue({ state: 'state-abc', error: 'access_denied' })

    await expect(handler(fakeEvent)).rejects.toMatchObject({
      statusCode: 401,
    })
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('rejects when the authorization code exchange fails', async () => {
    fetchMock.mockRejectedValueOnce(new Error('invalid_grant'))

    await expect(handler(fakeEvent)).rejects.toThrow()
    expect(setCookieMock).not.toHaveBeenCalled()
  })

  it('does not leak the client secret when the token endpoint rejects the request', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    const fetchError = new FetchError('Request failed')
    Object.assign(fetchError, {
      response: { status: 400 },
      data: { error: 'invalid_grant' },
      options: {
        body: new URLSearchParams({
          client_secret: 'client-secret-xyz',
        }).toString(),
      },
    })
    fetchMock.mockRejectedValueOnce(fetchError)

    let caught: unknown
    try {
      await handler(fakeEvent)
    } catch (err) {
      caught = err
    }

    expect(caught).toMatchObject({ statusCode: 401 })
    expect(JSON.stringify(caught)).not.toContain('client-secret-xyz')
    expect(consoleErrorSpy.mock.calls.flat().map(arg => JSON.stringify(arg)).join(' ')).not.toContain(
      'client-secret-xyz'
    )

    consoleErrorSpy.mockRestore()
  })

  it('exchanges the code, verifies the identity with front-api and sets session cookies', async () => {
    fetchMock
      .mockResolvedValueOnce({ id_token: 'google-id-token' })
      .mockResolvedValueOnce({
        accessToken: 'access-123',
        refreshToken: 'refresh-456',
      })

    await handler(fakeEvent)

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      'https://oauth2.googleapis.com/token',
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('client_secret=client-secret-xyz'),
      })
    )
    const firstCallBody = fetchMock.mock.calls[0][1].body as string
    expect(firstCallBody).toContain('code_verifier=verifier-123')
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      'http://localhost:8082/auth/google',
      expect.objectContaining({
        method: 'POST',
        body: { idToken: 'google-id-token', nonce: 'nonce-xyz' },
      })
    )

    expect(setCookieMock).toHaveBeenCalledWith(
      fakeEvent,
      'access_token',
      'access-123',
      expect.objectContaining({ httpOnly: true })
    )
    expect(setCookieMock).toHaveBeenCalledWith(
      fakeEvent,
      'refresh_token',
      'refresh-456',
      expect.objectContaining({ httpOnly: true })
    )
    expect(sendRedirectMock).toHaveBeenCalledWith(
      fakeEvent,
      '/account',
      302
    )
  })

  it('falls back to / when the pending redirect is an open redirect', async () => {
    getCookieMock.mockReturnValue(
      serializePendingState({ ...VALID_PENDING, redirect: 'https://evil.example' })
    )
    fetchMock
      .mockResolvedValueOnce({ id_token: 'google-id-token' })
      .mockResolvedValueOnce({
        accessToken: 'access-123',
        refreshToken: 'refresh-456',
      })

    await handler(fakeEvent)

    expect(sendRedirectMock).toHaveBeenCalledWith(fakeEvent, '/', 302)
  })
})
