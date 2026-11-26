import { beforeEach, describe, expect, it, vi } from 'vitest'

type CardsHandler = (typeof import('./cards.post'))['default']

const searchProductCardsMock = vi.hoisted(() => vi.fn())
const useProductServiceMock = vi.hoisted(() =>
  vi.fn(() => ({ searchProductCards: searchProductCardsMock }))
)
const resolveDomainLanguageMock = vi.hoisted(() =>
  vi.fn(() => ({ domainLanguage: 'fr' as const }))
)
const extractBackendErrorDetailsMock = vi.hoisted(() => vi.fn())
const setDomainLanguageCacheHeadersMock = vi.hoisted(() => vi.fn())
const logFacetQualityIssuesMock = vi.hoisted(() => vi.fn())
const sanitizeFacetAggregationsMock = vi.hoisted(() =>
  vi.fn((aggregations: unknown) => aggregations)
)

vi.mock('h3', () => ({
  defineEventHandler: (fn: CardsHandler) => fn,
  readBody: (event: { context?: { body?: unknown } }) => event.context?.body,
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

vi.mock('../../utils/facet-quality', () => ({
  logFacetQualityIssues: logFacetQualityIssuesMock,
  sanitizeFacetAggregations: sanitizeFacetAggregationsMock,
}))

const buildEvent = (body: unknown) => ({
  context: { body },
  node: { req: { headers: { host: 'nudger.fr' } } },
})

describe('server/api/products/cards.post', () => {
  let handler: CardsHandler

  beforeEach(async () => {
    vi.resetModules()
    searchProductCardsMock.mockReset()
    useProductServiceMock.mockClear()
    extractBackendErrorDetailsMock.mockReset()
    setDomainLanguageCacheHeadersMock.mockClear()
    logFacetQualityIssuesMock.mockClear()
    sanitizeFacetAggregationsMock.mockClear()
    searchProductCardsMock.mockResolvedValue({
      products: { data: [{ gtin: 123 }] },
      aggregations: [],
    })
    handler = (await import('./cards.post')).default
  })

  it('forwards the listing payload to front-api product cards', async () => {
    const sort = { sorts: [{ field: 'price.minPrice.price', order: 'asc' }] }
    const filters = { filters: [] }
    const aggs = { aggs: [] }

    const response = await handler(
      buildEvent({
        verticalId: 'tv',
        pageNumber: 1,
        pageSize: 15,
        query: 'oled',
        sort,
        filters,
        aggs,
        include: 'base',
      }) as never
    )

    expect(useProductServiceMock).toHaveBeenCalledWith('fr')
    expect(searchProductCardsMock).toHaveBeenCalledWith({
      verticalId: 'tv',
      pageNumber: 1,
      pageSize: 15,
      query: 'oled',
      sort,
      aggs,
      filters,
    })
    expect(setDomainLanguageCacheHeadersMock).toHaveBeenCalledWith(
      expect.anything(),
      'private, no-store'
    )
    expect(sanitizeFacetAggregationsMock).toHaveBeenCalledWith([])
    expect(response.products?.data?.[0]?.gtin).toBe(123)
  })

  it('rejects a request without body', async () => {
    await expect(handler(buildEvent(null) as never)).rejects.toMatchObject({
      statusCode: 400,
    })
    expect(searchProductCardsMock).not.toHaveBeenCalled()
  })

  it('maps backend failures to an HTTP error', async () => {
    const failure = new Error('boom')
    searchProductCardsMock.mockRejectedValue(failure)
    extractBackendErrorDetailsMock.mockResolvedValue({
      statusCode: 404,
      statusMessage: 'Not Found',
      logMessage: 'not found',
    })
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})

    await expect(
      handler(buildEvent({ verticalId: 'unknown' }) as never)
    ).rejects.toMatchObject({ statusCode: 404, statusMessage: 'Not Found' })

    consoleError.mockRestore()
  })
})
