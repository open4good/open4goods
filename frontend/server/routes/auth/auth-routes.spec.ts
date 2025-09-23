import type { H3Event } from 'h3'
import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'

import { DEFAULT_DOMAIN_LANGUAGE } from '~~/server/utils/domain-language'

vi.mock('ofetch', () => {
  class FakeFetchError extends Error {
    response?: {
      status?: number
      statusText?: string
      text?: () => Promise<string>
    }
    data?: unknown

    constructor(message: string, init: { response?: FakeFetchError['response']; data?: unknown } = {}) {
      super(message)
      this.name = 'FetchError'
      this.response = init.response
      this.data = init.data
    }
  }

  return {
    FetchError: FakeFetchError,
  }
})

const runtimeConfig = {
  apiUrl: 'https://api.backend.test',
  tokenCookieName: 'access_token',
  refreshCookieName: 'refresh_token',
}

let defineEventHandlerMock: ReturnType<typeof vi.fn>
let createErrorMock: ReturnType<typeof vi.fn>
let readBodyMock: ReturnType<typeof vi.fn>
let setCookieMock: ReturnType<typeof vi.fn>
let getCookieMock: ReturnType<typeof vi.fn>
let useRuntimeConfigMock: ReturnType<typeof vi.fn>
let fetchMock: ReturnType<typeof vi.fn>

const createEvent = (host?: string): H3Event =>
  ({
    node: {
      req: {
        headers: host ? { host } : {},
      },
      res: {},
    },
  } as unknown as H3Event)

beforeEach(() => {
  vi.resetModules()
  defineEventHandlerMock = vi.fn((handler: any) => handler)
  createErrorMock = vi.fn((options: any) => {
    const error = new Error(options.statusMessage ?? 'Error')
    ;(error as any).statusCode = options.statusCode
    ;(error as any).cause = options.cause
    return error
  })
  readBodyMock = vi.fn()
  setCookieMock = vi.fn()
  getCookieMock = vi.fn()
  useRuntimeConfigMock = vi.fn(() => runtimeConfig)
  fetchMock = vi.fn()

  vi.stubGlobal('defineEventHandler', defineEventHandlerMock)
  vi.stubGlobal('createError', createErrorMock)
  vi.stubGlobal('readBody', readBodyMock)
  vi.stubGlobal('setCookie', setCookieMock)
  vi.stubGlobal('getCookie', getCookieMock)
  vi.stubGlobal('useRuntimeConfig', useRuntimeConfigMock)
  vi.stubGlobal('$fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.clearAllMocks()
})

describe('auth routes domain language support', () => {
  it('login route appends domainLanguage and returns tokens for known hosts', async () => {
    readBodyMock.mockResolvedValue({ username: 'alice', password: 'secret' })
    const tokens = { accessToken: 'access-token', refreshToken: 'refresh-token' }
    fetchMock.mockResolvedValue(tokens)

    const { default: loginHandler } = await import('./login.post')
    const event = createEvent('nudger.fr')

    const result = await loginHandler(event)

    expect(result).toEqual(tokens)
    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toContain('domainLanguage=fr')
    expect(options).toMatchObject({
      method: 'POST',
      body: { username: 'alice', password: 'secret' },
    })
    expect(setCookieMock).toHaveBeenNthCalledWith(
      1,
      event,
      runtimeConfig.tokenCookieName,
      tokens.accessToken,
      expect.objectContaining({ sameSite: 'lax', httpOnly: true })
    )
    expect(setCookieMock).toHaveBeenNthCalledWith(
      2,
      event,
      runtimeConfig.refreshCookieName,
      tokens.refreshToken,
      expect.any(Object)
    )
  })

  it('login route surfaces backend failures clearly when credentials are rejected', async () => {
    readBodyMock.mockResolvedValue({ username: 'alice', password: 'secret' })
    const response = {
      status: 401,
      statusText: 'Unauthorized',
      text: vi.fn().mockResolvedValue('Bad credentials'),
    }
    const { FetchError } = await import('ofetch')
    fetchMock.mockRejectedValue(new FetchError('Rejected', { response }))

    const { default: loginHandler } = await import('./login.post')
    const event = createEvent('nudger.fr')

    const thrown = await loginHandler(event).catch(error => error)
    expect(thrown).toBeInstanceOf(Error)
    expect(thrown).toMatchObject({
      message: 'Unauthorized',
      cause: {
        statusCode: 401,
        statusMessage: 'Unauthorized',
        payload: 'Bad credentials',
      },
    })
  })

  it('refresh route appends domainLanguage and returns tokens for known hosts', async () => {
    getCookieMock.mockReturnValue('stored-refresh-token')
    const tokens = { accessToken: 'new-access', refreshToken: 'new-refresh' }
    fetchMock.mockResolvedValue(tokens)

    const { default: refreshHandler } = await import('./refresh.post')
    const event = createEvent('nudger.fr')

    const result = await refreshHandler(event)

    expect(result).toEqual(tokens)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toContain('domainLanguage=fr')
    expect(options).toMatchObject({
      method: 'POST',
      headers: { cookie: `${runtimeConfig.refreshCookieName}=stored-refresh-token` },
    })
    expect(setCookieMock).toHaveBeenNthCalledWith(
      1,
      event,
      runtimeConfig.tokenCookieName,
      tokens.accessToken,
      expect.any(Object)
    )
    expect(setCookieMock).toHaveBeenNthCalledWith(
      2,
      event,
      runtimeConfig.refreshCookieName,
      tokens.refreshToken,
      expect.any(Object)
    )
  })

  it('refresh route falls back to default domain language when host is unknown', async () => {
    getCookieMock.mockReturnValue('stored-refresh-token')
    const tokens = { accessToken: 'new-access', refreshToken: 'new-refresh' }
    fetchMock.mockResolvedValue(tokens)

    const { default: refreshHandler } = await import('./refresh.post')
    const event = createEvent('unknown.example')

    await refreshHandler(event)
    const [url] = fetchMock.mock.calls[0]
    expect(url).toContain(`domainLanguage=${DEFAULT_DOMAIN_LANGUAGE}`)
  })

  it('refresh route surfaces backend failures clearly when tokens are refused', async () => {
    getCookieMock.mockReturnValue('stored-refresh-token')
    const response = {
      status: 403,
      statusText: 'Forbidden',
      text: vi.fn().mockResolvedValue('Refresh rejected'),
    }
    const { FetchError } = await import('ofetch')
    fetchMock.mockRejectedValue(new FetchError('Rejected', { response }))

    const { default: refreshHandler } = await import('./refresh.post')
    const event = createEvent('nudger.fr')

    const thrown = await refreshHandler(event).catch(error => error)
    expect(thrown).toBeInstanceOf(Error)
    expect(thrown).toMatchObject({
      message: 'Forbidden',
      cause: {
        statusCode: 403,
        statusMessage: 'Forbidden',
        payload: 'Refresh rejected',
      },
    })
  })
})
