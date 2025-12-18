import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ProductDto } from '~~/shared/api-client'

type ShareResolutionHandler = (typeof import('./resolution.get'))['default']

const createErrorMock = vi.hoisted(() =>
  vi.fn((input: { statusCode: number; statusMessage: string }) => ({
    ...input,
    isCreateError: true,
  }))
)
const setDomainLanguageCacheHeadersMock = vi.hoisted(() => vi.fn())
const extractBackendErrorDetailsMock = vi.hoisted(() =>
  vi.fn().mockResolvedValue({
    statusCode: 502,
    statusMessage: 'Bad Gateway',
  })
)
const useProductServiceMock = vi.hoisted(() =>
  vi.fn(() => ({
    getProductByGtin: getProductByGtinMock,
    searchProducts: searchProductsMock,
  }))
)
const resolveDomainLanguageMock = vi.hoisted(() =>
  vi.fn(() => ({ domainLanguage: 'en' as const }))
)
const getProductByGtinMock = vi.hoisted(() => vi.fn<[], Promise<ProductDto | undefined>>())
const searchProductsMock = vi.hoisted(() =>
  vi.fn<[], Promise<{ products?: { data?: ProductDto[] } }>>()
)

let currentQuery: Record<string, unknown> = {}

vi.mock('h3', () => ({
  defineEventHandler: (fn: ShareResolutionHandler) => fn,
  getQuery: () => currentQuery,
  createError: createErrorMock,
}))

vi.mock('../../utils/cache-headers', () => ({
  setDomainLanguageCacheHeaders: setDomainLanguageCacheHeadersMock,
}))

vi.mock('../../utils/log-backend-error', () => ({
  extractBackendErrorDetails: extractBackendErrorDetailsMock,
}))

vi.mock('~~/shared/utils/domain-language', () => ({
  resolveDomainLanguage: resolveDomainLanguageMock,
}))

vi.mock('~~/shared/api-client/services/products.services', () => ({
  useProductService: useProductServiceMock,
}))

describe('server/api/share/resolution', () => {
  let handler: ShareResolutionHandler

  beforeEach(async () => {
    vi.resetModules()
    currentQuery = {}
    setDomainLanguageCacheHeadersMock.mockReset()
    extractBackendErrorDetailsMock.mockClear()
    createErrorMock.mockClear()
    getProductByGtinMock.mockReset()
    searchProductsMock.mockReset()
    resolveDomainLanguageMock.mockReturnValue({ domainLanguage: 'en' })

    handler = (await import('./resolution.get')).default
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('returns a resolved product when GTIN is provided', async () => {
    const product: ProductDto = { gtin: '12345678' } as ProductDto
    currentQuery = { gtin: '12345678' }
    getProductByGtinMock.mockResolvedValue(product)

    const result = await handler({} as never)

    expect(setDomainLanguageCacheHeadersMock).toHaveBeenCalledWith(
      expect.anything(),
      'private, no-store'
    )
    expect(result.status).toBe('resolved')
    expect(result.products).toEqual([product])
    expect(result.primary).toEqual(product)
  })

  it('searches with query when no GTIN is present', async () => {
    const products: ProductDto[] = [
      { gtin: '1', identity: { bestName: 'First' } } as ProductDto,
      { gtin: '2', identity: { bestName: 'Second' } } as ProductDto,
    ]
    currentQuery = { q: 'eco fridge' }
    searchProductsMock.mockResolvedValue({ products: { data: products } })

    const result = await handler({} as never)

    expect(searchProductsMock).toHaveBeenCalledWith(
      expect.objectContaining({
        pageNumber: 0,
        pageSize: 6,
        body: { query: 'eco fridge' },
      })
    )
    expect(result.status).toBe('resolved')
    expect(result.products).toHaveLength(2)
  })

  it('returns timeout status when resolution exceeds SLA', async () => {
    vi.useFakeTimers()
    currentQuery = { q: 'slow search' }
    searchProductsMock.mockImplementation(
      () =>
        new Promise(resolve => {
          setTimeout(() => resolve({ products: { data: [] } }), 5000)
        })
    )

    const resolutionPromise = handler({} as never)
    vi.advanceTimersByTime(5000)
    const result = await resolutionPromise

    expect(result.status).toBe('timeout')
  })

  it('throws createError on backend failure', async () => {
    currentQuery = { q: 'boom' }
    searchProductsMock.mockRejectedValue(new Error('backend down'))

    await expect(handler({} as never)).rejects.toMatchObject({
      isCreateError: true,
      statusCode: 502,
    })
  })
})
