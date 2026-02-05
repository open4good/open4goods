import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProductIncludeEnum } from '~~/shared/api-client/apis/ProductApi'

type ProductPageRouteHandler = (typeof import('./[gtin]'))['default']

const getProductByGtinMock = vi.hoisted(() => vi.fn())
const searchProductsMock = vi.hoisted(() => vi.fn())
const useProductServiceMock = vi.hoisted(() =>
  vi.fn(() => ({
    getProductByGtin: getProductByGtinMock,
    searchProducts: searchProductsMock,
  }))
)
const getCategoriesMock = vi.hoisted(() => vi.fn())
const getCategoryByIdMock = vi.hoisted(() => vi.fn())
const useCategoriesServiceMock = vi.hoisted(() =>
  vi.fn(() => ({
    getCategories: getCategoriesMock,
    getCategoryById: getCategoryByIdMock,
  }))
)
const listCommercialEventsMock = vi.hoisted(() => vi.fn())
const useCommercialEventsServiceMock = vi.hoisted(() =>
  vi.fn(() => ({
    listCommercialEvents: listCommercialEventsMock,
  }))
)
const resolveDomainLanguageMock = vi.hoisted(() =>
  vi.fn(() => ({ domainLanguage: 'en' as const }))
)
const setDomainLanguageCacheHeadersMock = vi.hoisted(() => vi.fn())
const extractBackendErrorDetailsMock = vi.hoisted(() => vi.fn())
const normaliseProductDtoMock = vi.hoisted(() => vi.fn())
const getRouterParamMock = vi.hoisted(() =>
  vi.fn<(event: unknown, name: string) => string | undefined>()
)
const getQueryMock = vi.hoisted(() => vi.fn<(event: unknown) => Record<string, unknown>>())
const createErrorMock = vi.hoisted(() =>
  vi.fn((input: { statusCode: number; statusMessage: string; cause?: unknown }) => ({
    ...input,
    isCreateError: true,
  }))
)

vi.mock('h3', () => ({
  defineEventHandler: (fn: ProductPageRouteHandler) => fn,
  getRouterParam: getRouterParamMock,
  getQuery: getQueryMock,
  createError: createErrorMock,
}))

vi.mock('~~/shared/api-client/services/products.services', () => ({
  useProductService: useProductServiceMock,
}))

vi.mock('~~/shared/api-client/services/categories.services', () => ({
  useCategoriesService: useCategoriesServiceMock,
}))

vi.mock('~~/shared/api-client/services/commercial-events.services', () => ({
  useCommercialEventsService: useCommercialEventsServiceMock,
}))

vi.mock('~~/shared/utils/domain-language', () => ({
  resolveDomainLanguage: resolveDomainLanguageMock,
}))

vi.mock('../../utils/cache-headers', () => ({
  setDomainLanguageCacheHeaders: setDomainLanguageCacheHeadersMock,
}))

vi.mock('../../utils/log-backend-error', () => ({
  extractBackendErrorDetails: extractBackendErrorDetailsMock,
}))

vi.mock('../../utils/normalise-product-sourcing', () => ({
  normaliseProductDto: normaliseProductDtoMock,
}))

describe('server/api/product-page/[gtin]', () => {
  let handler: ProductPageRouteHandler

  beforeEach(async () => {
    vi.resetModules()
    getProductByGtinMock.mockReset()
    searchProductsMock.mockReset()
    getCategoriesMock.mockReset()
    getCategoryByIdMock.mockReset()
    listCommercialEventsMock.mockReset()
    setDomainLanguageCacheHeadersMock.mockReset()
    resolveDomainLanguageMock.mockReturnValue({ domainLanguage: 'fr' })
    normaliseProductDtoMock.mockImplementation(input => input)
    extractBackendErrorDetailsMock.mockResolvedValue({
      statusCode: 502,
      statusMessage: 'Bad Gateway',
      logMessage: 'HTTP 502 - Bad Gateway',
    })
    getRouterParamMock.mockImplementation((event, name) => {
      const context = (event as { context?: { params?: Record<string, string> } }).context
      return context?.params?.[name]
    })
    getQueryMock.mockImplementation(
      event => (event as { context?: { query?: Record<string, unknown> } }).context?.query ?? {}
    )

    handler = (await import('./[gtin]')).default
  })

  it('returns aggregated product page payload from a single route call', async () => {
    getProductByGtinMock.mockResolvedValue({ gtin: 123, slug: 'p' })
    getCategoriesMock.mockResolvedValue([{ id: 'c1', verticalHomeUrl: '/tv' }])
    getCategoryByIdMock.mockResolvedValue({
      id: 'c1',
      impactScoreConfig: { criteriasPonderation: { water: 0.3 } },
    })
    searchProductsMock.mockResolvedValue({
      aggregations: [{ name: 'score_ECOSCORE', buckets: [] }],
    })
    listCommercialEventsMock.mockResolvedValue([{ id: 'event-1' }])

    const event = {
      node: {
        req: { headers: { host: 'nudger.example' } },
      },
      context: {
        params: { gtin: '123' },
        query: { include: 'base,attributes', categorySlug: 'tv' },
      },
    } as unknown as Parameters<ProductPageRouteHandler>[0]

    const response = await handler(event)

    expect(getProductByGtinMock).toHaveBeenCalledWith(123, [
      ProductIncludeEnum.Base,
      ProductIncludeEnum.Attributes,
    ])
    expect(getCategoriesMock).toHaveBeenCalledOnce()
    expect(getCategoryByIdMock).toHaveBeenCalledWith('c1')
    expect(searchProductsMock).toHaveBeenCalledOnce()
    expect(response.categoryDetail?.id).toBe('c1')
    expect(response.commercialEvents).toEqual([{ id: 'event-1' }])
    expect(response.aggregations.score_ECOSCORE).toBeDefined()
  })
})
