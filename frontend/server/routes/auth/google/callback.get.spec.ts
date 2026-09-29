import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

type CallbackHandler = (typeof import('./callback.get'))['default']

const runtimeConfig = vi.hoisted(() => ({
  apiUrl: 'http://localhost:8082',
  machineToken: 'server-only-token',
  public: {
    tokenCookieName: 'access_token',
    refreshCookieName: 'refresh_token',
  },
  googleOidc: {
    clientId: 'client-id',
    clientSecret: 'client-secret',
    redirectUri: 'http://localhost:3000/auth/google/callback',
  },
}))

const cookies = vi.hoisted(() => new Map<string, string>())
const setCookieMock = vi.hoisted(() => vi.fn())
const deleteCookieMock = vi.hoisted(() => vi.fn())
const fetchMock = vi.hoisted(() => vi.fn())

describe('Google OAuth callback', () => {
  let handler: CallbackHandler

  beforeEach(async () => {
    vi.resetModules()
    cookies.clear()
    cookies.set('oidc_state', 'expected-state')
    cookies.set('oidc_nonce', 'expected-nonce')
    cookies.set('oidc_verifier', 'verifier')
    cookies.set('oidc_redirect', '/editor')
    setCookieMock.mockReset()
    deleteCookieMock.mockReset()
    fetchMock.mockReset()
    fetchMock
      .mockResolvedValueOnce({ id_token: `header.${Buffer.from(JSON.stringify({ nonce: 'expected-nonce' })).toString('base64url')}.signature` })
      .mockResolvedValueOnce({ accessToken: 'application-access', refreshToken: 'application-refresh' })
    vi.stubGlobal('defineEventHandler', (callback: CallbackHandler) => callback)
    vi.stubGlobal('useRuntimeConfig', () => runtimeConfig)
    vi.stubGlobal('getQuery', () => ({ code: 'provider-code', state: 'expected-state' }))
    vi.stubGlobal('getCookie', (_event: unknown, name: string) => cookies.get(name))
    vi.stubGlobal('setCookie', setCookieMock)
    vi.stubGlobal('deleteCookie', deleteCookieMock)
    vi.stubGlobal('sendRedirect', (_event: unknown, location: string) => ({ location }))
    vi.stubGlobal('createError', (details: { statusCode: number; statusMessage: string }) => new Error(`${details.statusCode}: ${details.statusMessage}`))
    vi.stubGlobal('$fetch', fetchMock)
    handler = (await import('./callback.get')).default
  })

  afterEach(() => {
    vi.resetAllMocks()
    vi.unstubAllGlobals()
  })

  it('keeps provider tokens server-side and consumes the one-time state after a successful callback', async () => {
    await expect(handler({} as Parameters<CallbackHandler>[0])).resolves.toEqual({ location: '/editor' })

    expect(fetchMock).toHaveBeenNthCalledWith(2, 'http://localhost:8082/auth/google', {
      method: 'POST',
      headers: { 'X-Shared-Token': expect.any(String) },
      body: expect.objectContaining({ idToken: expect.any(String) }),
    })
    expect(setCookieMock).toHaveBeenCalledWith(expect.anything(), 'access_token', 'application-access', expect.anything())
    expect(deleteCookieMock).toHaveBeenCalledWith(expect.anything(), 'oidc_state', expect.anything())

    cookies.delete('oidc_state')
    await expect(handler({} as Parameters<CallbackHandler>[0])).rejects.toThrow('Google sign-in was refused')
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('purges the one-time cookies even when the state check fails before any provider call', async () => {
    vi.stubGlobal('getQuery', () => ({ code: 'provider-code', state: 'unexpected-state' }))

    await expect(handler({} as Parameters<CallbackHandler>[0])).rejects.toThrow('Google sign-in was refused')

    expect(fetchMock).not.toHaveBeenCalled()
    expect(deleteCookieMock).toHaveBeenCalledWith(expect.anything(), 'oidc_state', expect.anything())
    expect(deleteCookieMock).toHaveBeenCalledWith(expect.anything(), 'oidc_nonce', expect.anything())
    expect(deleteCookieMock).toHaveBeenCalledWith(expect.anything(), 'oidc_verifier', expect.anything())
    expect(deleteCookieMock).toHaveBeenCalledWith(expect.anything(), 'oidc_redirect', expect.anything())
  })

  it('rejects a state value carrying a different byte length instead of throwing', async () => {
    cookies.set('oidc_state', 'état-attendu')
    vi.stubGlobal('getQuery', () => ({ code: 'provider-code', state: 'etat-attendu' }))

    await expect(handler({} as Parameters<CallbackHandler>[0])).rejects.toThrow('Google sign-in was refused')
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
