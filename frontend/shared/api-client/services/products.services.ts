import { ProductApi } from '..'
import type { ProductDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

/**
 * Products service for handling product-related API calls
 */
export const useProductsService = (domainLanguage: DomainLanguage) => {
  const isVitest = typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: ProductApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error('useProductsService() is only available on the server runtime.')
    }

    if (!api) {
      api = new ProductApi(createBackendApiConfig())
    }

    return api
  }

  /**
   * Fetch a product by its GTIN identifier
   * @param gtin - Numeric GTIN string
   */
  const getProductByGtin = async (gtin: string): Promise<ProductDto> => {
    try {
      return await resolveApi().product({
        // The generated client expects a number type but GTIN codes often include
        // significant leading zeroes. Passing the original string preserves the
        // exact identifier while still satisfying the runtime requirements.
        gtin: gtin as unknown as number,
        domainLanguage,
      })
    } catch (error) {
      console.error(`Error fetching product with GTIN ${gtin}:`, error)
      throw error
    }
  }

  return { getProductByGtin }
}
