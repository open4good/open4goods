import {
  ProductApi,
  ProductDomainLanguageEnum,
  type ProductDto,
  type ProductIncludeEnum,
} from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

export interface GetProductOptions {
  include?: ProductIncludeEnum[]
}

/**
 * Service responsible for retrieving product details from the backend API.
 */
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

  const resolveDomainLanguageEnum = () =>
    domainLanguage === 'fr'
      ? ProductDomainLanguageEnum.Fr
      : ProductDomainLanguageEnum.En

  const getProduct = async (
    gtin: number,
    options: GetProductOptions = {},
  ): Promise<ProductDto> => {
    const include = options.include && options.include.length > 0 ? options.include : undefined

    return await resolveApi().product({
      gtin,
      include,
      domainLanguage: resolveDomainLanguageEnum(),
    })
  }

  return { getProduct }
}
