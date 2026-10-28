import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useRuntimeConfig } from '#imports'

type ContribRouteHandler =
  (typeof import('../../utils/affiliation-redirect'))['resolveAffiliationRedirect']

const resolveRedirectMock = vi.hoisted(() => vi.fn())
const useAffiliationServiceMock = vi.hoisted(() =>
  vi.fn(() => ({ resolveRedirect: resolveRedirectMock }))
)
const resolveDomainLanguageMock = vi.hoisted(() =>
  vi.fn(() => ({ domainLanguage: 'en' as const }))
)
const extractBackendErrorDetailsMock = vi.hoisted(() => vi.fn())
const getRouterParamMock = vi.hoisted(() =>
  vi.fn<(event: unknown, name: string) => string | undefined>()
)
const sendMock = vi.hoisted(() => vi.fn())
const setResponseHeaderMock = vi.hoisted(() => vi.fn())
const setResponseStatusMock = vi.hoisted(() => vi.fn())
const getCookieMock = vi.hoisted(() =>
  vi.fn((event: unknown, name: string) => {
    const header = (
      event as { node?: { req?: { headers?: { cookie?: string } } } }
    ).node?.req?.headers?.cookie
    return header
      ?.split(';')
      .map(part => part.trim().split('='))
      .find(([key]) => key === name)?.[1]
  })
)
const createErrorMock = vi.hoisted(() =>
  vi.fn(
    (input: {
      statusCode: number
      statusMessage: string
      cause?: unknown
    }) => ({
      ...input,
      isCreateError: true,
    })
  )
)

vi.mock('h3', () => ({
  defineEventHandler: (fn: ContribRouteHandler) => fn,
  getCookie: getCookieMock,
  getRouterParam: getRouterParamMock,
  createError: createErrorMock,
  send: sendMock,
  setResponseHeader: setResponseHeaderMock,
  setResponseStatus: setResponseStatusMock,
}))

vi.mock('~~/shared/api-client/services/affiliation.services', () => ({
  useAffiliationService: useAffiliationServiceMock,
}))

vi.mock('~~/shared/utils/domain-language', async importOriginal => ({
  ...(await importOriginal<typeof import('~~/shared/utils/domain-language')>()),
  resolveDomainLanguage: resolveDomainLanguageMock,
}))

vi.mock('../../utils/log-backend-error', () => ({
  extractBackendErrorDetails: extractBackendErrorDetailsMock,
}))

const SITE_REFERER = 'https://nudger.fr/fr/produit/123'

const BROWSER_UA =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'

type ContribRuntimeConfig = {
  affiliationBlockedUserAgents: string
  affiliationRequireNavigationSignal: unknown
}

describe('resolveAffiliationRedirect', () => {
  let handler: ContribRouteHandler
  let runtimeConfig: ContribRuntimeConfig
  let consoleErrorSpy: ReturnType<typeof vi.spyOn>

  beforeEach(async () => {
    vi.resetModules()

    resolveRedirectMock.mockReset()
    useAffiliationServiceMock.mockReturnValue({
      resolveRedirect: resolveRedirectMock,
    })
    resolveDomainLanguageMock.mockReturnValue({ domainLanguage: 'en' })
    extractBackendErrorDetailsMock.mockResolvedValue({
      statusCode: 500,
      statusMessage: 'Backend failure',
      statusText: 'Internal Server Error',
      bodyText: undefined,
      isResponseError: false,
      logMessage: 'HTTP 500 - Backend failure',
    })
    getRouterParamMock.mockImplementation((event, name) => {
      const context = (
        event as { context?: { params?: Record<string, string | undefined> } }
      ).context
      return context?.params?.[name]
    })
    createErrorMock.mockImplementation(input => ({
      ...input,
      isCreateError: true,
    }))

    consoleErrorSpy = vi
      .spyOn(console, 'error')
      .mockImplementation(() => undefined)

    // The server util reads the real (auto-imported) Nuxt runtime config.
    runtimeConfig = useRuntimeConfig() as unknown as ContribRuntimeConfig
    runtimeConfig.affiliationBlockedUserAgents = ''
    runtimeConfig.affiliationRequireNavigationSignal = true
    vi.spyOn(console, 'warn').mockImplementation(() => undefined)

    handler = (await import('../../utils/affiliation-redirect'))
      .resolveAffiliationRedirect
  })

  afterEach(() => {
    runtimeConfig.affiliationBlockedUserAgents = ''
    runtimeConfig.affiliationRequireNavigationSignal = true
    consoleErrorSpy.mockRestore()
    vi.clearAllMocks()
  })

  it('resolves redirects for GET requests', async () => {
    resolveRedirectMock.mockResolvedValue({
      statusCode: 301,
      location: 'https://example.test',
    })

    const event = {
      node: {
        req: {
          method: 'GET',
          headers: {
            host: 'nudger.example',
            'user-agent': BROWSER_UA,
            referer: SITE_REFERER,
          },
        },
      },
      context: { params: { token: 'token-abc' } },
    } as unknown as Parameters<ContribRouteHandler>[0]

    const response = await handler(event)

    expect(resolveDomainLanguageMock).toHaveBeenCalledWith('nudger.example')
    expect(useAffiliationServiceMock).toHaveBeenCalledWith('en')
    expect(resolveRedirectMock).toHaveBeenCalledWith({
      token: 'token-abc',
      method: 'GET',
      userAgent: BROWSER_UA,
    })
    expect(response).toEqual({
      statusCode: 301,
      location: 'https://example.test',
    })
  })

  it('resolves redirects for POST requests and forwards the user agent header', async () => {
    resolveRedirectMock.mockResolvedValue({
      statusCode: 301,
      location: 'https://example.test/post',
    })

    const event = {
      node: {
        req: {
          method: 'POST',
          headers: {
            host: 'nudger.example',
            'user-agent': BROWSER_UA,
            referer: SITE_REFERER,
          },
        },
      },
      context: { params: { token: 'token-xyz' } },
    } as unknown as Parameters<ContribRouteHandler>[0]

    const response = await handler(event)

    expect(resolveRedirectMock).toHaveBeenCalledWith({
      token: 'token-xyz',
      method: 'POST',
      userAgent: BROWSER_UA,
    })
    expect(response).toEqual({
      statusCode: 301,
      location: 'https://example.test/post',
    })
  })

  it('propagates the Location header from manual redirect responses', async () => {
    resolveRedirectMock.mockResolvedValue({
      statusCode: 301,
      location: 'https://example.test/manual',
    })

    const event = {
      node: {
        req: {
          method: 'GET',
          headers: {
            host: 'nudger.example',
            'user-agent': BROWSER_UA,
            referer: SITE_REFERER,
          },
        },
      },
      context: { params: { token: 'token-manual' } },
    } as unknown as Parameters<ContribRouteHandler>[0]

    const response = await handler(event)

    expect(resolveRedirectMock).toHaveBeenCalledWith({
      token: 'token-manual',
      method: 'GET',
      userAgent: BROWSER_UA,
    })
    expect(response.statusCode).toBe(301)
    expect(response.location).toBe('https://example.test/manual')
  })

  it('throws a 405 error when using an unsupported HTTP method', async () => {
    const event = {
      node: {
        req: {
          method: 'PUT',
          headers: {
            host: 'nudger.example',
            'user-agent': BROWSER_UA,
            referer: SITE_REFERER,
          },
        },
      },
      context: { params: { token: 'token-abc' } },
    } as unknown as Parameters<ContribRouteHandler>[0]

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 405 })
    expect(resolveRedirectMock).not.toHaveBeenCalled()
  })

  it('throws a 400 error when the token is missing', async () => {
    getRouterParamMock.mockReturnValueOnce(undefined)

    const event = {
      node: {
        req: {
          method: 'GET',
          headers: {
            host: 'nudger.example',
            'user-agent': BROWSER_UA,
            referer: SITE_REFERER,
          },
        },
      },
      context: { params: { token: undefined } },
    } as unknown as Parameters<ContribRouteHandler>[0]

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 400 })
  })

  it('logs backend failures and rethrows with backend metadata', async () => {
    const backendFailure = new Error('backend boom')
    resolveRedirectMock.mockRejectedValueOnce(backendFailure)

    const event = {
      node: {
        req: {
          method: 'GET',
          headers: {
            host: 'nudger.example',
            'user-agent': BROWSER_UA,
            referer: SITE_REFERER,
          },
        },
      },
      context: { params: { token: 'token-abc' } },
    } as unknown as Parameters<ContribRouteHandler>[0]

    await expect(handler(event)).rejects.toMatchObject({
      statusCode: 500,
      statusMessage: 'Backend failure',
      isCreateError: true,
    })

    expect(extractBackendErrorDetailsMock).toHaveBeenCalledWith(backendFailure)
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'Error resolving affiliation redirect',
      'HTTP 500 - Backend failure',
      expect.objectContaining({ statusCode: 500 })
    )
    expect(createErrorMock).toHaveBeenCalledWith({
      statusCode: 500,
      statusMessage: 'Backend failure',
      cause: backendFailure,
    })
  })

  const eventWith = (userAgent: string | undefined) =>
    ({
      node: {
        req: {
          method: 'GET',
          headers: {
            host: 'nudger.example',
            ...(userAgent === undefined ? {} : { 'user-agent': userAgent }),
          },
        },
      },
      context: { params: { token: 'token-abc-very-long-secret' } },
    }) as unknown as Parameters<ContribRouteHandler>[0]

  it.each([
    [
      'Googlebot',
      'Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)',
    ],
    [
      'headless Chrome',
      'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) HeadlessChrome/120.0.0.0 Safari/537.36',
    ],
    ['empty user agent', undefined],
    ['blank user agent', '   '],
  ])('refuses %s with a 403 before calling the service', async (_name, ua) => {
    await expect(handler(eventWith(ua))).rejects.toMatchObject({
      statusCode: 403,
    })

    expect(useAffiliationServiceMock).not.toHaveBeenCalled()
    expect(resolveRedirectMock).not.toHaveBeenCalled()
    expect(console.warn).toHaveBeenCalledTimes(1)
    expect(vi.mocked(console.warn).mock.calls[0]?.[0] as string).not.toContain(
      'token-abc-very-long-secret'
    )
  })

  it('refuses user agents matching the configured patterns', async () => {
    runtimeConfig.affiliationBlockedUserAgents = ' acmefetcher , other '
    const event = eventWith(`${BROWSER_UA} AcmeFetcher/9`)
    event.node.req.headers.referer = SITE_REFERER

    await expect(handler(event)).rejects.toMatchObject({
      statusCode: 403,
      statusMessage: 'Forbidden',
    })
    expect(resolveRedirectMock).not.toHaveBeenCalled()
  })

  // User agent replayed by the GOU-1168 proxy network (no referer, no cookie).
  const ROBOT_CHROME_UA =
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36'

  const navigationEvent = (headers: Record<string, string> = {}) =>
    ({
      node: {
        req: {
          method: 'GET',
          headers: {
            host: 'nudger.fr',
            'user-agent': ROBOT_CHROME_UA,
            ...headers,
          },
        },
      },
      context: { params: { token: 'token-abc-very-long-secret' } },
    }) as unknown as Parameters<ContribRouteHandler>[0]

  describe('navigation signal (GOU-1184)', () => {
    beforeEach(() => {
      resolveRedirectMock.mockResolvedValue({
        statusCode: 301,
        location: 'https://network.example/click',
      })
    })

    it('K1: refuses a browser user agent without referer nor cookie, before calling the service', async () => {
      await expect(handler(navigationEvent())).rejects.toMatchObject({
        statusCode: 403,
        statusMessage: 'Ouvrez ce lien depuis la fiche produit sur nudger.fr',
      })

      expect(useAffiliationServiceMock).not.toHaveBeenCalled()
      expect(resolveRedirectMock).not.toHaveBeenCalled()
    })

    it.each([
      'https://evil.example/',
      'https://nudger.fr.evil.example/produit',
      'not a url',
    ])('refuses a referer outside the site (%s)', async referer => {
      await expect(handler(navigationEvent({ referer }))).rejects.toMatchObject(
        { statusCode: 403 }
      )
      expect(resolveRedirectMock).not.toHaveBeenCalled()
    })

    it('refuses an unexpected navigation cookie value', async () => {
      await expect(
        handler(navigationEvent({ cookie: 'nudger_nav=0' }))
      ).rejects.toMatchObject({ statusCode: 403 })
      expect(resolveRedirectMock).not.toHaveBeenCalled()
    })

    it.each([
      'https://nudger.fr/fr/produit/123',
      'https://beta.nudger.fr/produit',
      'https://nudger.com/en/product',
    ])('K2: redirects with a site referer (%s)', async referer => {
      await expect(
        handler(navigationEvent({ referer }))
      ).resolves.toMatchObject({ statusCode: 301 })
      expect(resolveRedirectMock).toHaveBeenCalledTimes(1)
    })

    it('K3: redirects with the navigation cookie', async () => {
      await expect(
        handler(navigationEvent({ cookie: 'other=x; nudger_nav=1' }))
      ).resolves.toMatchObject({ statusCode: 301 })
      expect(resolveRedirectMock).toHaveBeenCalledTimes(1)
    })

    it.each([false, 'false', '0'])(
      'K5: switch %s restores the GOU-1175 behaviour',
      async value => {
        runtimeConfig.affiliationRequireNavigationSignal = value

        await expect(handler(navigationEvent())).resolves.toMatchObject({
          statusCode: 301,
        })
        expect(resolveRedirectMock).toHaveBeenCalledTimes(1)
      }
    )

    it('keeps the GOU-1175 robot rules when a navigation signal is present', async () => {
      await expect(
        handler(
          navigationEvent({
            referer: SITE_REFERER,
            'user-agent': 'Mozilla/5.0 (compatible; Googlebot/2.1)',
          })
        )
      ).rejects.toMatchObject({ statusCode: 403, statusMessage: 'Forbidden' })
      expect(resolveRedirectMock).not.toHaveBeenCalled()
    })

    it('logs refusals aggregated per interval, without the full token', async () => {
      const nowSpy = vi.spyOn(Date, 'now').mockReturnValue(1_000_000)

      for (let i = 0; i < 3; i++) {
        await expect(handler(navigationEvent())).rejects.toMatchObject({
          statusCode: 403,
        })
      }

      expect(console.warn).toHaveBeenCalledTimes(1)
      expect(vi.mocked(console.warn).mock.calls[0]?.[0]).toContain(
        '1 request(s)'
      )

      nowSpy.mockReturnValue(1_060_000)
      await expect(handler(navigationEvent())).rejects.toMatchObject({
        statusCode: 403,
      })

      expect(console.warn).toHaveBeenCalledTimes(2)
      const message = vi.mocked(console.warn).mock.calls[1]?.[0] as string
      expect(message).toContain('3 request(s)')
      expect(message).not.toContain('token-abc-very-long-secret')
      nowSpy.mockRestore()
    })
  })

  it('sends a short text/plain error without HTML', async () => {
    const { sendContribPlainError } =
      await import('../../utils/affiliation-redirect')
    const event = {} as Parameters<ContribRouteHandler>[0]

    await sendContribPlainError(event, {
      statusCode: 400,
      statusMessage:
        '{"type":"about:blank","title":"Bad Request","status":400}',
    })

    expect(setResponseStatusMock).toHaveBeenCalledWith(event, 400)
    expect(setResponseHeaderMock).toHaveBeenCalledWith(
      event,
      'Cache-Control',
      'no-store'
    )
    expect(setResponseHeaderMock).toHaveBeenCalledWith(
      event,
      'X-Robots-Tag',
      'noindex, nofollow'
    )
    expect(sendMock).toHaveBeenCalledWith(
      event,
      'Bad Request',
      'text/plain; charset=utf-8'
    )
  })
})
