import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

type SuggestRouteHandler = (typeof import('./suggest.get'))['default']

const fetchSearchSuggestionsMock = vi.hoisted(() => vi.fn())
const useSearchServiceMock = vi.hoisted(() =>
  vi.fn(() => ({ fetchSearchSuggestions: fetchSearchSuggestionsMock }))
)
const resolveDomainLanguageMock = vi.hoisted(() =>
  vi.fn(() => ({ domainLanguage: 'en' as const }))
)
const extractBackendErrorDetailsMock = vi.hoisted(() => vi.fn())
const setDomainLanguageCacheHeadersMock = vi.hoisted(() => vi.fn())
const getQueryMock = vi.hoisted(() => vi.fn())
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
  defineEventHandler: (fn: SuggestRouteHandler) => fn,
  getQuery: getQueryMock,
  createError: createErrorMock,
}))

vi.mock('~~/shared/api-client/services/search.services', () => ({
  useSearchService: useSearchServiceMock,
}))

vi.mock('~~/shared/utils/domain-language', () => ({
  resolveDomainLanguage: resolveDomainLanguageMock,
}))

vi.mock('../../utils/log-backend-error', () => ({
  extractBackendErrorDetails: extractBackendErrorDetailsMock,
}))

vi.mock('../../utils/cache-headers', () => ({
  setDomainLanguageCacheHeaders: setDomainLanguageCacheHeadersMock,
}))

describe('server/api/search/suggest.get', () => {
  let handler: SuggestRouteHandler
  let consoleErrorSpy: ReturnType<typeof vi.spyOn>

  beforeEach(async () => {
    vi.resetModules()

    fetchSearchSuggestionsMock.mockReset()
    useSearchServiceMock.mockReturnValue({
      fetchSearchSuggestions: fetchSearchSuggestionsMock,
    })
    resolveDomainLanguageMock.mockReturnValue({ domainLanguage: 'en' })
    setDomainLanguageCacheHeadersMock.mockImplementation(() => undefined)
    getQueryMock.mockReturnValue({ query: 'apple' })

    createErrorMock.mockImplementation(input => ({
      ...input,
      isCreateError: true,
    }))

    consoleErrorSpy = vi
      .spyOn(console, 'error')
      .mockImplementation(() => undefined)

    handler = (await import('./suggest.get')).default
  })

  afterEach(() => {
    consoleErrorSpy.mockRestore()
    vi.clearAllMocks()
  })

  it('returns an empty response when the backend returns a 5xx error', async () => {
    const backendFailure = new Error('Backend unavailable')
    fetchSearchSuggestionsMock.mockRejectedValue(backendFailure)
    extractBackendErrorDetailsMock.mockResolvedValue({
      statusCode: 503,
      statusMessage: 'Service Unavailable',
      statusText: 'Service Unavailable',
      bodyText: undefined,
      isResponseError: true,
      logMessage: 'HTTP 503 - Service Unavailable',
    })

    const event = {
      node: {
        req: {
          headers: { host: 'nudger.example' },
        },
        res: { statusCode: 0 },
      },
    } as unknown as Parameters<SuggestRouteHandler>[0]

    const response = await handler(event)

    expect(resolveDomainLanguageMock).toHaveBeenCalledWith('nudger.example')
    expect(fetchSearchSuggestionsMock).toHaveBeenCalledWith('apple')
    expect(extractBackendErrorDetailsMock).toHaveBeenCalledWith(backendFailure)
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'Search suggestion proxy failed',
      'HTTP 503 - Service Unavailable',
      expect.objectContaining({ statusCode: 503 })
    )
    expect(createErrorMock).not.toHaveBeenCalled()
    expect(response).toEqual({ categoryMatches: [], productMatches: [] })
    expect(event.node.res.statusCode).toBe(200)
  })

  it('throws for 4xx backend errors', async () => {
    const backendFailure = new Error('Invalid request')
    fetchSearchSuggestionsMock.mockRejectedValue(backendFailure)
    extractBackendErrorDetailsMock.mockResolvedValue({
      statusCode: 400,
      statusMessage: 'Bad Request',
      statusText: 'Bad Request',
      bodyText: 'Missing query',
      isResponseError: true,
      logMessage: 'HTTP 400 - Bad Request',
    })

    const event = {
      node: {
        req: {
          headers: { host: 'nudger.example' },
        },
        res: { statusCode: 0 },
      },
    } as unknown as Parameters<SuggestRouteHandler>[0]

    await expect(handler(event)).rejects.toEqual({
      statusCode: 400,
      statusMessage: 'Bad Request',
      cause: backendFailure,
      isCreateError: true,
    })

    expect(resolveDomainLanguageMock).toHaveBeenCalledWith('nudger.example')
    expect(extractBackendErrorDetailsMock).toHaveBeenCalledWith(backendFailure)
    expect(consoleErrorSpy).toHaveBeenCalled()
    expect(event.node.res.statusCode).toBe(0)
  })
})
