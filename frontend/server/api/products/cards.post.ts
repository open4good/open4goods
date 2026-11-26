import { createError, defineEventHandler, readBody } from 'h3'

import type {
  AggregationRequestDto,
  FilterRequestDto,
  ProductCardSearchResponseDto,
  SortRequestDto,
} from '~~/shared/api-client'
import { useProductService } from '~~/shared/api-client/services/products.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import {
  logFacetQualityIssues,
  sanitizeFacetAggregations,
} from '../../utils/facet-quality'

/**
 * Listing search payload: the same fields as `/api/products/search`, without
 * `include` since a card has a fixed shape.
 */
export interface ProductCardsPayload {
  verticalId?: string
  pageNumber?: number
  pageSize?: number
  query?: string
  sort?: SortRequestDto
  aggs?: AggregationRequestDto
  filters?: FilterRequestDto
}

/**
 * Lightweight listing search (GOU-2710): proxies `POST /products/cards` of
 * front-api, which returns `ProductCardDto` items (about 4 KB each instead of
 * about 50 KB for the full `ProductDto`). Category grids, guides, the nudge
 * tool and product alternatives use it; the product page keeps the full DTO.
 */
export default defineEventHandler(
  async (event): Promise<ProductCardSearchResponseDto> => {
    setDomainLanguageCacheHeaders(event, 'private, no-store')

    const payload = await readBody<ProductCardsPayload | null>(event)

    if (!payload) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Request body is required.',
      })
    }

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    try {
      const response = await useProductService(
        domainLanguage
      ).searchProductCards({
        verticalId: payload.verticalId,
        pageNumber: payload.pageNumber,
        pageSize: payload.pageSize,
        query: payload.query,
        sort: payload.sort,
        aggs: payload.aggs,
        filters: payload.filters,
      })
      logFacetQualityIssues(response.aggregations)
      response.aggregations = sanitizeFacetAggregations(response.aggregations)

      return response
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error(
        'Error searching product cards:',
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
