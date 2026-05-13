import { createError, defineEventHandler, getQuery, readBody } from 'h3'

import type {
  AggregationRequestDto,
  FilterRequestDto,
  GlobalSearchResponseDto,
  ProductSearchRequestDto,
  ProductSearchResponseDto,
  SortRequestDto,
} from '~~/shared/api-client'
import { ProductsIncludeEnum } from '~~/shared/api-client'
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
  pageNumber?: number
  pageSize?: number
  aggs?: AggregationRequestDto
}

type SearchPayload = ProductsSearchPayload | GlobalSearchPayload

const PRODUCT_INCLUDE_VALUES = Object.values(ProductsIncludeEnum)

const toFirstString = (value: unknown): string | undefined => {
  if (Array.isArray(value)) {
    return toFirstString(value[0])
  }

  return typeof value === 'string' && value.length > 0 ? value : undefined
}

const toInteger = (value: unknown): number | undefined => {
  const rawValue =
    typeof value === 'number' ? String(value) : toFirstString(value)

  if (!rawValue) {
    return undefined
  }

  const parsed = Number.parseInt(rawValue, 10)
  return Number.isFinite(parsed) ? parsed : undefined
}

const toIncludeValues = (value: unknown): ProductsIncludeEnum[] | undefined => {
  const rawValues = Array.isArray(value) ? value : [value]
  const includeValues = rawValues
    .flatMap(rawValue =>
      typeof rawValue === 'string'
        ? rawValue.split(',').map(item => item.trim())
        : []
    )
    .filter((item): item is ProductsIncludeEnum =>
      PRODUCT_INCLUDE_VALUES.includes(item as ProductsIncludeEnum)
    )

  return includeValues.length > 0 ? [...new Set(includeValues)] : undefined
}

const isGlobalSearchPayload = (
  payload: SearchPayload
): payload is GlobalSearchPayload =>
  typeof payload === 'object' &&
  payload !== null &&
  'query' in payload &&
  !(
    'verticalId' in payload ||
    'semanticSearch' in payload ||
    'include' in payload
  )

const hasProductSearchRouteQuery = (routeQuery: Record<string, unknown>) =>
  routeQuery.verticalId != null || routeQuery.include != null

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

    const routeQuery = getQuery(event)
    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const productService = useProductService(domainLanguage)

    if (
      isGlobalSearchPayload(payload) &&
      !hasProductSearchRouteQuery(routeQuery)
    ) {
      try {
        return await productService.searchGlobalProducts({
          query: payload.query ?? '',
          filters: payload.filters,
          sort: payload.sort,
          searchType: payload.searchType,
          pageNumber: payload.pageNumber,
          pageSize: payload.pageSize,
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
    const normalizedProductsPayload: ProductsSearchPayload = {
      ...productsPayload,
      verticalId:
        productsPayload.verticalId ?? toFirstString(routeQuery.verticalId),
      pageNumber:
        productsPayload.pageNumber ?? toInteger(routeQuery.pageNumber),
      pageSize: productsPayload.pageSize ?? toInteger(routeQuery.pageSize),
      query: productsPayload.query ?? toFirstString(routeQuery.query),
      include:
        toIncludeValues(productsPayload.include) ??
        toIncludeValues(routeQuery.include),
    }

    const searchBody = buildSearchBody(normalizedProductsPayload)

    try {
      const response = await productService.searchProducts({
        verticalId: normalizedProductsPayload.verticalId,
        pageNumber: normalizedProductsPayload.pageNumber,
        pageSize: normalizedProductsPayload.pageSize,
        query: normalizedProductsPayload.query,
        include: normalizedProductsPayload.include,
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
