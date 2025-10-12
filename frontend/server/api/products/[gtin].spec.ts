import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { SpyInstance } from 'vitest'

type ProductRouteHandler = typeof import('./[gtin]')['default']

const getRouterParamMock = vi.hoisted(() =>
  vi.fn<(event: unknown, name: string) => string | undefined>(),
)
const useProductServiceMock = vi.hoisted(() =>
  vi.fn(() => ({ getProduct: vi.fn() })),
)
const resolveDomainLanguageMock = vi.hoisted(() =>
  vi.fn(() => ({ domainLanguage: 'en' as const })),
)
const extractBackendErrorDetailsMock = vi.hoisted(() =>
  vi.fn(async () => ({
    statusCode: 500,
    statusMessage: 'Backend failure',
    statusText: 'Internal Server Error',
    bodyText: undefined,
    isResponseError: false,
    logMessage: 'HTTP 500 - Backend failure',
  })),
)
const setResponseHeaderMock = vi.hoisted(() => vi.fn())
const createErrorMock = vi.hoisted(() =>
  vi.fn((input: { statusCode: number; statusMessage: string; cause?: unknown }) => ({
    ...input,
    isCreateError: true,
  })),
)

vi.mock('h3', () => ({
  defineEventHandler: (fn: ProductRouteHandler) => fn,
  getRouterParam: getRouterParamMock,
  createError: createErrorMock,
  setResponseHeader: setResponseHeaderMock,
}))

vi.mock('~~/shared/api-client/services/product.services', () => ({
  useProductService: useProductServiceMock,
}))

vi.mock('~~/shared/utils/domain-language', () => ({
  resolveDomainLanguage: resolveDomainLanguageMock,
}))

vi.mock('../../utils/log-backend-error', () => ({
  extractBackendErrorDetails: extractBackendErrorDetailsMock,
}))

describe('server/api/products/[gtin]', () => {
  let handler: ProductRouteHandler
  let errorSpy: SpyInstance

  beforeEach(async () => {
    vi.resetModules()
    setResponseHeaderMock.mockReset()
    createErrorMock.mockReset()
    getRouterParamMock.mockImplementation((event, name) => {
      const params = (event as { context?: { params?: Record<string, string> } }).context?.params
      return params?.[name]
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
    useProductServiceMock.mockReturnValue({
      getProduct: vi.fn().mockResolvedValue({ gtin: 123, slug: 'product' }),
    })
    errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)

    vi.stubGlobal('defineEventHandler', (fn: ProductRouteHandler) => fn)
    vi.stubGlobal('setResponseHeader', setResponseHeaderMock)
    vi.stubGlobal('getRouterParam', getRouterParamMock)
    vi.stubGlobal('createError', createErrorMock)

    handler = (await import('./[gtin]')).default
  })

  afterEach(() => {
    errorSpy.mockRestore()
    vi.clearAllMocks()
    vi.unstubAllGlobals()
  })

  it('fetches the product using the service and forwards the domain language', async () => {
    const event = {
      node: {
        req: {
          headers: {
            host: 'nudger.test',
          },
        },
      },
      context: { params: { gtin: '123456' } },
    } as unknown as Parameters<ProductRouteHandler>[0]

    const response = await handler(event)

    expect(resolveDomainLanguageMock).toHaveBeenCalledWith('nudger.test')
    expect(useProductServiceMock).toHaveBeenCalledWith('en')

    const serviceInstance = useProductServiceMock.mock.results[0]?.value
    expect(serviceInstance?.getProduct).toHaveBeenCalledWith(123456, {
      include: ['base', 'identity', 'names', 'scores'],
    })
    expect(setResponseHeaderMock).toHaveBeenCalledWith(
      event,
      'Cache-Control',
      'public, max-age=300, s-maxage=300',
    )
    expect(response).toEqual({ gtin: 123, slug: 'product' })
  })

  it('returns a 400 error when the GTIN parameter is missing', async () => {
    getRouterParamMock.mockReturnValueOnce(undefined)

    const event = {
      node: { req: { headers: { host: 'nudger.test' } } },
      context: { params: {} },
    } as unknown as Parameters<ProductRouteHandler>[0]

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 400 })
  })

  it('returns a 400 error when the GTIN parameter is invalid', async () => {
    const event = {
      node: { req: { headers: { host: 'nudger.test' } } },
      context: { params: { gtin: 'invalid' } },
    } as unknown as Parameters<ProductRouteHandler>[0]

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 400 })
  })

  it('logs backend errors and rethrows with backend metadata', async () => {
    const failingService = { getProduct: vi.fn().mockRejectedValue(new Error('backend boom')) }
    useProductServiceMock.mockReturnValueOnce(failingService)

    const event = {
      node: { req: { headers: { host: 'nudger.test' } } },
      context: { params: { gtin: '987654' } },
    } as unknown as Parameters<ProductRouteHandler>[0]

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 500 })
    expect(failingService.getProduct).toHaveBeenCalledWith(987654, expect.any(Object))
    expect(errorSpy).toHaveBeenCalledWith(
      'Error fetching product detail',
      'HTTP 500 - Backend failure',
      expect.objectContaining({ statusCode: 500 }),
    )
  })
})
