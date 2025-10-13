import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

type ProductRouteHandler = typeof import('./[gtin]')['default']

const getProductByGtinMock = vi.hoisted(() => vi.fn())
const useProductServiceMock = vi.hoisted(() =>
  vi.fn(() => ({ getProductByGtin: getProductByGtinMock }))
)
const resolveDomainLanguageMock = vi.hoisted(() =>
  vi.fn(() => ({ domainLanguage: 'en' as const }))
)
const extractBackendErrorDetailsMock = vi.hoisted(() => vi.fn())
const setDomainLanguageCacheHeadersMock = vi.hoisted(() => vi.fn())
const getRouterParamMock = vi.hoisted(() =>
  vi.fn<(event: unknown, name: string) => string | undefined>()
)
const createErrorMock = vi.hoisted(() =>
  vi.fn(
    (input: { statusCode: number; statusMessage: string; cause?: unknown }) => ({
      ...input,
      isCreateError: true,
    })
  )
)

vi.mock('h3', () => ({
  defineEventHandler: (fn: ProductRouteHandler) => fn,
  getRouterParam: getRouterParamMock,
  createError: createErrorMock,
}))

vi.mock('~~/shared/api-client/services/products.services', () => ({
  useProductService: useProductServiceMock,
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

describe('server/api/products/[gtin]', () => {
  let handler: ProductRouteHandler
  let consoleErrorSpy: ReturnType<typeof vi.spyOn>

  beforeEach(async () => {
    vi.resetModules()

    getProductByGtinMock.mockReset()
    useProductServiceMock.mockReturnValue({
      getProductByGtin: getProductByGtinMock,
    })
    resolveDomainLanguageMock.mockReturnValue({ domainLanguage: 'fr' })
    extractBackendErrorDetailsMock.mockResolvedValue({
      statusCode: 502,
      statusMessage: 'Bad Gateway',
      statusText: 'Bad Gateway',
      bodyText: undefined,
      isResponseError: false,
      logMessage: 'HTTP 502 - Bad Gateway',
    })
    getRouterParamMock.mockImplementation((event, name) => {
      const context = (event as {
        context?: { params?: Record<string, string | undefined> }
      }).context
      return context?.params?.[name]
    })
    createErrorMock.mockImplementation((input) => ({
      ...input,
      isCreateError: true,
    }))

    consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)

    handler = (await import('./[gtin]')).default
  })

  afterEach(() => {
    consoleErrorSpy.mockRestore()
    vi.clearAllMocks()
  })

  it('fetches product data and forwards the domain language', async () => {
    const productResponse = { gtin: 1234567890123, slug: 'test-product' }
    getProductByGtinMock.mockResolvedValue(productResponse)

    const event = {
      node: {
        req: {
          headers: { host: 'nudger.example' },
        },
      },
      context: { params: { gtin: '1234567890123' } },
    } as unknown as Parameters<ProductRouteHandler>[0]

    const response = await handler(event)

    expect(setDomainLanguageCacheHeadersMock).toHaveBeenCalledWith(
      event,
      'public, max-age=300, s-maxage=300'
    )
    expect(resolveDomainLanguageMock).toHaveBeenCalledWith('nudger.example')
    expect(useProductServiceMock).toHaveBeenCalledWith('fr')
    expect(getProductByGtinMock).toHaveBeenCalledWith(1234567890123)
    expect(response).toEqual(productResponse)
  })

  it('throws a 400 error when the GTIN parameter is missing', async () => {
    getRouterParamMock.mockReturnValueOnce(undefined)

    const event = {
      node: {
        req: {
          headers: { host: 'nudger.example' },
        },
      },
      context: { params: { gtin: undefined } },
    } as unknown as Parameters<ProductRouteHandler>[0]

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 400 })
    expect(useProductServiceMock).not.toHaveBeenCalled()
  })

  it('translates backend errors into HTTP responses', async () => {
    const backendFailure = new Error('backend boom')
    getProductByGtinMock.mockRejectedValueOnce(backendFailure)

    const event = {
      node: {
        req: {
          headers: {
            host: 'nudger.example',
          },
        },
      },
      context: { params: { gtin: '1234567890123' } },
    } as unknown as Parameters<ProductRouteHandler>[0]

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 502 })

    expect(extractBackendErrorDetailsMock).toHaveBeenCalledWith(backendFailure)
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'Error fetching product:',
      'HTTP 502 - Bad Gateway',
      expect.any(Object)
    )
  })
})
