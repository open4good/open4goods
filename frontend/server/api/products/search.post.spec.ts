import { beforeEach, describe, expect, it, vi } from 'vitest'

type SearchHandler = (typeof import('./search.post'))['default']

const searchProductsMock = vi.hoisted(() => vi.fn())
const searchGlobalProductsMock = vi.hoisted(() => vi.fn())
const useProductServiceMock = vi.hoisted(() =>
  vi.fn(() => ({
    searchProducts: searchProductsMock,
    searchGlobalProducts: searchGlobalProductsMock,
  }))
)
const resolveDomainLanguageMock = vi.hoisted(() =>
  vi.fn(() => ({ domainLanguage: 'en' as const }))
)
const extractBackendErrorDetailsMock = vi.hoisted(() => vi.fn())
const setDomainLanguageCacheHeadersMock = vi.hoisted(() => vi.fn())
const normaliseProductDtoMock = vi.hoisted(() => vi.fn())
const logFacetQualityIssuesMock = vi.hoisted(() => vi.fn())
const sanitizeFacetAggregationsMock = vi.hoisted(() =>
  vi.fn((aggregations: unknown) => aggregations)
)

vi.mock('h3', () => ({
  defineEventHandler: (fn: SearchHandler) => fn,
  readBody: (event: { context?: { body?: unknown } }) => event.context?.body,
  getQuery: (event: { context?: { query?: Record<string, unknown> } }) =>
    event.context?.query ?? {},
  createError: (input: { statusCode: number; statusMessage: string }) => input,
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

vi.mock('../../utils/normalise-product-sourcing', () => ({
  normaliseProductDto: normaliseProductDtoMock,
}))

vi.mock('../../utils/facet-quality', () => ({
  logFacetQualityIssues: logFacetQualityIssuesMock,
  sanitizeFacetAggregations: sanitizeFacetAggregationsMock,
}))

describe('server/api/products/search.post', () => {
  let handler: SearchHandler

  beforeEach(async () => {
    vi.resetModules()
    searchProductsMock.mockReset()
    searchGlobalProductsMock.mockReset()
    useProductServiceMock.mockClear()
    resolveDomainLanguageMock.mockClear()
    extractBackendErrorDetailsMock.mockReset()
    setDomainLanguageCacheHeadersMock.mockClear()
    normaliseProductDtoMock.mockClear()
    logFacetQualityIssuesMock.mockClear()
    sanitizeFacetAggregationsMock.mockClear()
    sanitizeFacetAggregationsMock.mockImplementation(
      (aggregations: unknown) => aggregations
    )
    searchProductsMock.mockResolvedValue({
      products: { data: [{ gtin: 123 }] },
      aggregations: { brands: {} },
    })

    handler = (await import('./search.post')).default
  })

  it('forwards supported query parameters when the body only contains search criteria', async () => {
    const event = {
      node: { req: { headers: { host: 'nudger.fr' } } },
      context: {
        query: {
          verticalId: 'smartphones',
          pageNumber: '2',
          pageSize: '24',
          include: 'base,identity,names,attributes,resources,scores,offers',
        },
        body: {
          filters: { filters: [] },
        },
      },
    } as unknown as Parameters<SearchHandler>[0]

    await handler(event)

    expect(searchProductsMock).toHaveBeenCalledWith({
      verticalId: 'smartphones',
      pageNumber: 2,
      pageSize: 24,
      query: undefined,
      include: [
        'base',
        'identity',
        'names',
        'attributes',
        'resources',
        'scores',
        'offers',
      ],
      body: {
        filters: { filters: [] },
      },
    })
  })

  it('keeps body values ahead of query values and filters unsupported includes', async () => {
    const event = {
      node: { req: { headers: { host: 'nudger.fr' } } },
      context: {
        query: {
          verticalId: 'ignored',
          pageNumber: '99',
          include: 'base,vertical,offers',
        },
        body: {
          verticalId: 'actual',
          pageNumber: 0,
          pageSize: 3,
          include: ['scores', 'vertical', 'offers'],
        },
      },
    } as unknown as Parameters<SearchHandler>[0]

    await handler(event)

    expect(searchProductsMock).toHaveBeenCalledWith({
      verticalId: 'actual',
      pageNumber: 0,
      pageSize: 3,
      query: undefined,
      include: ['scores', 'offers'],
    })
  })

  it('treats a body query as product search when verticalId is in the route query', async () => {
    const event = {
      node: { req: { headers: { host: 'nudger.fr' } } },
      context: {
        query: {
          verticalId: 'laptops',
          include: 'base,names',
        },
        body: {
          query: 'repairable',
        },
      },
    } as unknown as Parameters<SearchHandler>[0]

    await handler(event)

    expect(searchGlobalProductsMock).not.toHaveBeenCalled()
    expect(searchProductsMock).toHaveBeenCalledWith({
      verticalId: 'laptops',
      pageNumber: undefined,
      pageSize: undefined,
      query: 'repairable',
      include: ['base', 'names'],
    })
  })
})
