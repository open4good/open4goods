import { ProductApi } from '..'
import type {
  AggregationRequestDto,
  FilterRequestDto,
  ProductDto,
  ProductFieldOptionsResponse,
  ProductSearchRequestDto,
  ProductSearchResponseDto,
  SortRequestDto,
} from '..'
import type {
  FilterableFieldsForVerticalRequest,
  ProductsRequest,
  SortableFieldsForVerticalRequest,
} from '../apis/ProductApi'
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

  const getFilterableFieldsForVertical = async (
    verticalId: string,
  ): Promise<ProductFieldOptionsResponse> => {
    if (!verticalId) {
      throw new TypeError('verticalId is required to resolve filterable fields.')
    }

    const request: FilterableFieldsForVerticalRequest = {
      verticalId,
      domainLanguage,
    }

    try {
      return await resolveApi().filterableFieldsForVertical(request)
    } catch (error) {
      console.error('Error fetching filterable fields for vertical:', verticalId, error)
      throw error
    }
  }

  const getSortableFieldsForVertical = async (
    verticalId: string,
  ): Promise<ProductFieldOptionsResponse> => {
    if (!verticalId) {
      throw new TypeError('verticalId is required to resolve sortable fields.')
    }

    const request: SortableFieldsForVerticalRequest = {
      verticalId,
      domainLanguage,
    }

    try {
      return await resolveApi().sortableFieldsForVertical(request)
    } catch (error) {
      console.error('Error fetching sortable fields for vertical:', verticalId, error)
      throw error
    }
  }

  const searchProducts = async (
    parameters: Omit<ProductsRequest, 'domainLanguage'> & {
      aggs?: AggregationRequestDto
      sort?: SortRequestDto
      filters?: FilterRequestDto
      body?: ProductSearchRequestDto
    },
  ): Promise<ProductSearchResponseDto> => {
    const {
      aggs,
      sort,
      filters,
      body: explicitBody,
      productSearchRequestDto: providedBody,
      ...rest
    } = parameters

    const body: ProductSearchRequestDto | undefined =
      explicitBody ??
      providedBody ??
      (sort || aggs || filters
        ? {
            ...(sort ? { sort } : {}),
            ...(aggs ? { aggs } : {}),
            ...(filters ? { filters } : {}),
          }
        : undefined)

    const request: ProductsRequest = {
      domainLanguage,
      ...rest,
      ...(body ? { productSearchRequestDto: body } : {}),
    }

    try {
      return await resolveApi().products(request)
    } catch (error) {
      console.error('Error searching products with parameters:', request, error)
      throw error
    }
  }

  return {
    getProductByGtin,
    getFilterableFieldsForVertical,
    getSortableFieldsForVertical,
    searchProducts,
  }
}
