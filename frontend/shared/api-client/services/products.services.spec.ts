import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'

import type { ProductDto } from '../..'

const productMock = vi.hoisted(() => vi.fn<Promise<ProductDto>, [unknown]>())
const productApiMock = vi.hoisted(() =>
  vi.fn(() => ({
    product: productMock,
  }))
)
const createBackendApiConfigMock = vi.hoisted(() => vi.fn())

vi.mock('..', async (importOriginal) => {
  const actual = await importOriginal<typeof import('..')>()

  return {
    ...actual,
    ProductApi: productApiMock,
  }
})

vi.mock('./createBackendApiConfig', () => ({
  createBackendApiConfig: createBackendApiConfigMock,
}))

describe('useProductsService', () => {
  let originalVitestFlag: string | undefined

  beforeEach(() => {
    vi.resetModules()
    productMock.mockReset()
    productApiMock.mockClear()
    createBackendApiConfigMock.mockReturnValue({})
    productMock.mockResolvedValue({
      id: 'product-1',
      gtin: 123456,
      name: 'Sample product',
    })
    originalVitestFlag = process.env.VITEST
  })

  afterEach(() => {
    if (typeof originalVitestFlag === 'undefined') {
      delete process.env.VITEST
    } else {
      process.env.VITEST = originalVitestFlag
    }
    vi.unstubAllGlobals()
  })

  it('preserves the original GTIN value when calling the backend', async () => {
    process.env.VITEST = 'true'
    const { useProductsService } = await import('./products.services')

    const service = useProductsService('en')

    await service.getProductByGtin('001234567890')

    expect(productMock).toHaveBeenCalledTimes(1)
    expect(productMock).toHaveBeenCalledWith({
      gtin: '001234567890',
      domainLanguage: 'en',
    })
  })
})

