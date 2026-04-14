import { beforeEach, describe, expect, it, vi } from 'vitest'

type ResolveHandler = (typeof import('./resolve.get'))['default']

const getProductByGtinMock = vi.hoisted(() => vi.fn())
const searchProductsMock = vi.hoisted(() => vi.fn())
const useProductServiceMock = vi.hoisted(() =>
  vi.fn(() => ({
    getProductByGtin: getProductByGtinMock,
    searchProducts: searchProductsMock,
  }))
)
const resolveDomainLanguageMock = vi.hoisted(() =>
  vi.fn(() => ({ domainLanguage: 'en' as const }))
)
const extractBackendErrorDetailsMock = vi.hoisted(() => vi.fn())
const setDomainLanguageCacheHeadersMock = vi.hoisted(() => vi.fn())

vi.mock('h3', () => ({
  defineEventHandler: (fn: ResolveHandler) => fn,
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
  normaliseProductDto: (product: unknown) => product,
}))

describe('server/api/products/resolve.get', () => {
  let handler: ResolveHandler

  beforeEach(async () => {
    vi.resetModules()
    getProductByGtinMock.mockReset()
    searchProductsMock.mockReset()
    extractBackendErrorDetailsMock.mockReset()

    handler = (await import('./resolve.get')).default
  })

  it('resolves a product by gtin', async () => {
    getProductByGtinMock.mockResolvedValue({
      gtin: 123,
      identity: { brand: 'Acme', model: 'Model X' },
    })

    const event = {
      node: { req: { headers: { host: 'nudger.fr' } } },
      context: { query: { gtin: '123' } },
    } as unknown as Parameters<ResolveHandler>[0]

    const response = await handler(event)

    expect(getProductByGtinMock).toHaveBeenCalledWith(123)
    expect(response.product?.gtin).toBe(123)
    expect(response.resolvedBy).toBe('gtin')
  })

  it('returns conservative null on ambiguous brand+model matches', async () => {
    searchProductsMock.mockResolvedValue({
      products: {
        data: [
          { identity: { brand: 'Acme', model: 'Model X' } },
          { identity: { brand: 'ACME', model: 'Model X' } },
        ],
      },
    })

    const event = {
      node: { req: { headers: { host: 'nudger.fr' } } },
      context: { query: { brand: 'Acme', model: 'Model X' } },
    } as unknown as Parameters<ResolveHandler>[0]

    const response = await handler(event)

    expect(response.product).toBeNull()
    expect(response.reason).toBe('ambiguous')
  })
})
