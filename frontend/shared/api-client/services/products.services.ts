import { ofetch } from 'ofetch'
import { ProductApi } from '..'
import type {
  AggregationRequestDto,
  FilterRequestDto,
  GlobalSearchRequestDto,
  GlobalSearchResponseDto,
  ProductDto,
  ProductFieldOptionsResponse,
  ProductSearchRequestDto,
  ProductSearchResponseDto,
  ReviewGenerationStatus,
  SearchSuggestResponseDto,
  SortRequestDto,
} from '..'
import type {
  FilterableFieldsForVerticalRequest,
  ProductIncludeEnum,
  ProductsRequest,
  SortableFieldsForVerticalRequest,
} from '../apis/ProductApi'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

export const useProductService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: ProductApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useProductService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new ProductApi(createBackendApiConfig())
    }

    return api
  }

  const getProductByGtin = async (
    gtin: string | number,
    include?: ProductIncludeEnum[]
  ): Promise<ProductDto> => {
    const parsedGtin =
      typeof gtin === 'number' ? gtin : Number.parseInt(gtin, 10)

    if (!Number.isFinite(parsedGtin)) {
      throw new TypeError('GTIN must be a number.')
    }

    try {
      return await resolveApi().product({
        gtin: parsedGtin,
        domainLanguage,
        include,
      })
    } catch (error) {
      console.error('Error fetching product:', error)
      throw error
    }
  }

  const getFilterableFieldsForVertical = async (
    verticalId: string
  ): Promise<ProductFieldOptionsResponse> => {
    if (!verticalId) {
      throw new TypeError(
        'verticalId is required to resolve filterable fields.'
      )
    }

    const request: FilterableFieldsForVerticalRequest = {
      verticalId,
      domainLanguage,
    }

    try {
      return await resolveApi().filterableFieldsForVertical(request)
    } catch (error) {
      console.error(
        'Error fetching filterable fields for vertical:',
        verticalId,
        error
      )
      throw error
    }
  }

  const getSortableFieldsForVertical = async (
    verticalId: string
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
      console.error(
        'Error fetching sortable fields for vertical:',
        verticalId,
        error
      )
      throw error
    }
  }

  const searchProducts = async (
    parameters: Omit<ProductsRequest, 'domainLanguage'> & {
      aggs?: AggregationRequestDto
      sort?: SortRequestDto
      filters?: FilterRequestDto
      body?: ProductSearchRequestDto
    }
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

  const searchGlobalProducts = async (
    payload: GlobalSearchRequestDto
  ): Promise<GlobalSearchResponseDto> => {
    const normalizedQuery = payload?.query?.trim()

    if (!normalizedQuery) {
      throw new TypeError('Query is required to execute a global search.')
    }

    const config = createBackendApiConfig()
    const basePath = config.basePath?.replace(/\/$/, '') ?? ''
    const endpoint = `${basePath}/products/search`

    try {
      return await ofetch<GlobalSearchResponseDto>(endpoint, {
        method: 'POST',
        headers: {
          ...(config.headers ?? {}),
        },
        query: {
          domainLanguage,
        },
        body: {
          query: normalizedQuery,
          filters: payload.filters,
          sort: payload.sort,
          searchType: payload.searchType,
        },
      })
    } catch (error) {
      console.error(
        'Error executing global search',
        { query: normalizedQuery, domainLanguage },
        error
      )
      throw error
    }
  }

  const fetchSearchSuggestions = async (
    query: string
  ): Promise<SearchSuggestResponseDto> => {
    const normalizedQuery = query?.trim()

    if (!normalizedQuery) {
      throw new TypeError('Query is required to fetch search suggestions.')
    }

    const config = createBackendApiConfig()
    const basePath = config.basePath?.replace(/\/$/, '') ?? ''
    const endpoint = `${basePath}/products/suggest`

    try {
      return await ofetch<SearchSuggestResponseDto>(endpoint, {
        headers: {
          ...(config.headers ?? {}),
        },
        query: {
          query: normalizedQuery,
          domainLanguage,
        },
      })
    } catch (error) {
      console.error(
        'Error fetching search suggestions',
        { query: normalizedQuery, domainLanguage },
        error
      )
      throw error
    }
  }

  const getReviewStatus = async (
    gtin: string | number
  ): Promise<ReviewGenerationStatus> => {
    const parsedGtin =
      typeof gtin === 'number' ? gtin : Number.parseInt(gtin, 10)

    if (!Number.isFinite(parsedGtin)) {
      throw new TypeError('GTIN must be a number.')
    }

    try {
      return await resolveApi().reviewStatus({
        gtin: parsedGtin,
        domainLanguage,
      })
    } catch (error) {
      console.error(
        'Error fetching review status for product',
        parsedGtin,
        error
      )
      throw error
    }
  }

  const triggerReviewGeneration = async (
    gtin: string | number,
    hcaptchaResponse?: string | null,
    options: { force?: boolean; authToken?: string | null } = {}
  ): Promise<number> => {
    const parsedGtin =
      typeof gtin === 'number' ? gtin : Number.parseInt(gtin, 10)

    if (!Number.isFinite(parsedGtin)) {
      throw new TypeError('GTIN must be a number.')
    }

    if (!hcaptchaResponse && !options.force) {
      throw new TypeError(
        'hCaptcha response is required to trigger review generation.'
      )
    }

    const config = createBackendApiConfig()
    const basePath = config.basePath?.replace(/\/$/, '') ?? ''
    const endpoint = `${basePath}/products/${encodeURIComponent(String(parsedGtin))}/review`

    try {
      return await ofetch<number>(endpoint, {
        method: 'POST',
        headers: {
          ...(config.headers ?? {}),
          ...(options.authToken
            ? { Authorization: `Bearer ${options.authToken}` }
            : {}),
        },
        query: {
          ...(hcaptchaResponse ? { hcaptchaResponse } : {}),
          ...(options.force ? { force: true } : {}),
          domainLanguage,
        },
      })
    } catch (error) {
      console.error(
        'Error triggering review generation for product',
        parsedGtin,
        error
      )
      throw error
    }
  }

  return {
    getProductByGtin,
    getFilterableFieldsForVertical,
    getSortableFieldsForVertical,
    searchProducts,
    searchGlobalProducts,
    fetchSearchSuggestions,
    getReviewStatus,
    triggerReviewGeneration,
  }
}
