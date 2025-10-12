import { beforeEach, describe, expect, it, vi } from 'vitest'

const productMock = vi.fn()
const productApiCtorMock = vi.fn(() => ({ product: productMock }))
const createBackendApiConfigMock = vi.fn()

vi.mock('..', () => ({
  ProductApi: productApiCtorMock,
  ProductDomainLanguageEnum: { Fr: 'fr', En: 'en' } as const,
}))

vi.mock('./createBackendApiConfig', () => ({
  createBackendApiConfig: createBackendApiConfigMock,
}))

describe('useProductService', () => {
  beforeEach(() => {
    vi.resetModules()
    productMock.mockReset()
    productApiCtorMock.mockReset()
    createBackendApiConfigMock.mockReset()
    createBackendApiConfigMock.mockReturnValue({ basePath: 'https://api.test' })
  })

  it('fetches products using the backend client with the correct domain language', async () => {
    productMock.mockResolvedValueOnce({ gtin: 123 })

    const { useProductService } = await import('./product.services')
    const service = useProductService('fr')

    const response = await service.getProduct(123, { include: ['base', 'identity'] })

    expect(createBackendApiConfigMock).toHaveBeenCalledTimes(1)
    expect(productApiCtorMock).toHaveBeenCalledTimes(1)
    expect(productApiCtorMock).toHaveBeenCalledWith({ basePath: 'https://api.test' })
    expect(productMock).toHaveBeenCalledTimes(1)
    expect(productMock).toHaveBeenCalledWith({
      gtin: 123,
      include: ['base', 'identity'],
      domainLanguage: 'fr',
    })
    expect(response).toEqual({ gtin: 123 })
  })

  it('reuses the same API instance across calls', async () => {
    productMock.mockResolvedValue({ gtin: 456 })

    const { useProductService } = await import('./product.services')
    const service = useProductService('en')

    await service.getProduct(456)
    await service.getProduct(789)

    expect(createBackendApiConfigMock).toHaveBeenCalledTimes(1)
    expect(productApiCtorMock).toHaveBeenCalledTimes(1)
    expect(productMock).toHaveBeenCalledTimes(2)
    expect(productMock).toHaveBeenNthCalledWith(1, {
      gtin: 456,
      include: undefined,
      domainLanguage: 'en',
    })
    expect(productMock).toHaveBeenNthCalledWith(2, {
      gtin: 789,
      include: undefined,
      domainLanguage: 'en',
    })
  })
})
