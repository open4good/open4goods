import { ProductApi } from '..'
import type { ProductDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

export const useProductService = (domainLanguage: DomainLanguage) => {
  const isVitest = typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: ProductApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error('useProductService() is only available on the server runtime.')
    }

    if (!api) {
      api = new ProductApi(createBackendApiConfig())
    }

    return api
  }

  const getProductByGtin = async (gtin: string | number): Promise<ProductDto> => {
    const parsedGtin = typeof gtin === 'number' ? gtin : Number.parseInt(gtin, 10)

    if (!Number.isFinite(parsedGtin)) {
      throw new TypeError('GTIN must be a number.')
    }

    try {
      return await resolveApi().product({
        gtin: parsedGtin,
        domainLanguage,
      })
    } catch (error) {
      console.error('Error fetching product:', error)
      throw error
    }
  }

  return { getProductByGtin }
}
