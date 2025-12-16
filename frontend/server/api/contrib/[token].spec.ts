import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

type ContribRouteHandler = (typeof import('./[token]'))['default']

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
  getRouterParam: getRouterParamMock,
  createError: createErrorMock,
}))

vi.mock('~~/shared/api-client/services/affiliation.services', () => ({
  useAffiliationService: useAffiliationServiceMock,
}))

vi.mock('~~/shared/utils/domain-language', () => ({
  resolveDomainLanguage: resolveDomainLanguageMock,
}))

vi.mock('../../utils/log-backend-error', () => ({
  extractBackendErrorDetails: extractBackendErrorDetailsMock,
}))

describe('server/api/contrib/[token]', () => {
  let handler: ContribRouteHandler
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

    handler = (await import('./[token]')).default
  })

  afterEach(() => {
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
      userAgent: undefined,
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
            'user-agent': 'Mozilla/5.0',
          },
        },
      },
      context: { params: { token: 'token-xyz' } },
    } as unknown as Parameters<ContribRouteHandler>[0]

    const response = await handler(event)

    expect(resolveRedirectMock).toHaveBeenCalledWith({
      token: 'token-xyz',
      method: 'POST',
      userAgent: 'Mozilla/5.0',
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
          },
        },
      },
      context: { params: { token: 'token-manual' } },
    } as unknown as Parameters<ContribRouteHandler>[0]

    const response = await handler(event)

    expect(resolveRedirectMock).toHaveBeenCalledWith({
      token: 'token-manual',
      method: 'GET',
      userAgent: undefined,
    })
    expect(response.statusCode).toBe(301)
    expect(response.location).toBe('https://example.test/manual')
  })

  it('throws a 405 error when using an unsupported HTTP method', async () => {
    const event = {
      node: {
        req: {
          method: 'PUT',
          headers: { host: 'nudger.example' },
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
          headers: { host: 'nudger.example' },
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
          headers: { host: 'nudger.example' },
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
})
