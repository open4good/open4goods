import type {
  AggregationRequestDto,
  FilterRequestDto,
  GlobalSearchResponseDto,
  ProductSearchRequestDto,
  ProductSearchResponseDto,
  SortRequestDto,
  ProductsIncludeEnum,
} from '~~/shared/api-client'
import { useProductService } from '~~/shared/api-client/services/products.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { normaliseProductDto } from '../../utils/normalise-product-sourcing'
import {
  logFacetQualityIssues,
  sanitizeFacetAggregations,
} from '../../utils/facet-quality'

interface ProductsSearchPayload {
  verticalId?: string
  pageNumber?: number
  pageSize?: number
  sort?: SortRequestDto
  aggs?: AggregationRequestDto
  filters?: FilterRequestDto
  semanticSearch?: boolean
  query?: string
  include?: ProductsIncludeEnum[]
  searchType?: string
}

interface GlobalSearchPayload {
  query?: string
  filters?: FilterRequestDto
  sort?: SortRequestDto
  searchType?: string
}

type SearchPayload = ProductsSearchPayload | GlobalSearchPayload

const isGlobalSearchPayload = (
  payload: SearchPayload
): payload is GlobalSearchPayload =>
  typeof payload === 'object' &&
  payload !== null &&
  'query' in payload &&
  !(
    'verticalId' in payload ||
    'pageNumber' in payload ||
    'pageSize' in payload ||
    'aggs' in payload ||
    'semanticSearch' in payload ||
    'include' in payload
  )

export default defineEventHandler(
  async (
    event
  ): Promise<ProductSearchResponseDto | GlobalSearchResponseDto> => {
    setDomainLanguageCacheHeaders(event, 'private, no-store')

    const payload = await readBody<SearchPayload | null>(event)

    if (!payload) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Request body is required.',
      })
    }

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const productService = useProductService(domainLanguage)

    if (isGlobalSearchPayload(payload)) {
      try {
        return await productService.searchGlobalProducts({
          query: payload.query ?? '',
          filters: payload.filters,
          sort: payload.sort,
          searchType: payload.searchType,
        })
      } catch (error) {
        const backendError = await extractBackendErrorDetails(error)
        console.error(
          'Error executing global search:',
          backendError.logMessage,
          backendError
        )

        throw createError({
          statusCode: backendError.statusCode,
          statusMessage: backendError.statusMessage,
          cause: error,
        })
      }
    }

    const buildSearchBody = (
      input: ProductsSearchPayload
    ): ProductSearchRequestDto | undefined => {
      const body: ProductSearchRequestDto = {}
      let hasContent = false

      if (input.sort) {
        body.sort = input.sort
        hasContent = true
      }

      if (input.aggs) {
        body.aggs = input.aggs
        hasContent = true
      }

      if (input.filters) {
        body.filters = input.filters
        hasContent = true
      }

      if (typeof input.semanticSearch === 'boolean') {
        body.semanticSearch = input.semanticSearch
        hasContent = true
      }

      return hasContent ? body : undefined
    }

    // After the type guard, we know this is a ProductsSearchPayload
    const productsPayload = payload as ProductsSearchPayload

    const searchBody = buildSearchBody(productsPayload)

    try {
      const response = await productService.searchProducts({
        verticalId: productsPayload.verticalId,
        pageNumber: productsPayload.pageNumber,
        pageSize: productsPayload.pageSize,
        query: productsPayload.query,
        include: productsPayload.include,
        ...(searchBody ? { body: searchBody } : {}),
      })
      response.products?.data?.forEach(product => {
        normaliseProductDto(product)
      })
      logFacetQualityIssues(response.aggregations)
      response.aggregations = sanitizeFacetAggregations(response.aggregations)

      return response
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error(
        'Error searching products:',
        backendError.logMessage,
        backendError
      )

      throw createError({
        statusCode: backendError.statusCode,
        statusMessage: backendError.statusMessage,
        cause: error,
      })
    }
  }
)
