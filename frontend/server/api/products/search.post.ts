import type {
  AggregationRequestDto,
  FilterRequestDto,
  ProductSearchResponseDto,
  SortRequestDto,
  ProductsIncludeEnum,
} from '~~/shared/api-client'
import type { Agg } from '~~/shared/api-client/models/Agg'
import type { Filter } from '~~/shared/api-client/models/Filter'
import { useProductService } from '~~/shared/api-client/services/products.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

interface ProductsSearchPayload {
  verticalId?: string
  pageNumber?: number
  pageSize?: number
  sort?: SortRequestDto
  aggs?: AggregationRequestDto
  filters?: FilterRequestDto
  query?: string
  include?: ProductsIncludeEnum[]
}

export default defineEventHandler(
  async (event): Promise<ProductSearchResponseDto> => {
    setDomainLanguageCacheHeaders(event, 'private, no-store')

    const payload = await readBody<ProductsSearchPayload | null>(event)

    if (!payload) {
      throw createError({ statusCode: 400, statusMessage: 'Request body is required.' })
    }

    const rawHost = event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const productService = useProductService(domainLanguage)

    const sanitizedSort = sanitizeSortRequest(payload.sort)
    const sanitizedAggregations = sanitizeAggregationRequest(payload.aggs)
    const sanitizedFilters = sanitizeFilterRequest(payload.filters)

    try {
      return await productService.searchProducts({
        verticalId: payload.verticalId,
        pageNumber: payload.pageNumber,
        pageSize: payload.pageSize,
        query: payload.query,
        include: payload.include,
        ...(sanitizedSort ? { sort: sanitizedSort } : {}),
        ...(sanitizedAggregations ? { aggs: sanitizedAggregations } : {}),
        ...(sanitizedFilters ? { filters: sanitizedFilters } : {}),
      })
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error('Error searching products:', backendError.logMessage, backendError)

      throw createError({
        statusCode: backendError.statusCode,
        statusMessage: backendError.statusMessage,
        cause: error,
      })
    }
  },
)

const sanitizeSortRequest = (sort?: SortRequestDto): SortRequestDto | undefined => {
  const entries = sort?.sorts
    ?.map((option) => {
      if (!option?.field) {
        return null
      }

      const sanitizedOption: NonNullable<SortRequestDto['sorts']>[number] = {
        field: option.field,
      }

      if (option.order) {
        sanitizedOption.order = option.order
      }

      return sanitizedOption
    })
    .filter(
      (value): value is NonNullable<SortRequestDto['sorts']>[number] => value !== null,
    )

  if (!entries?.length) {
    return undefined
  }

  return { sorts: entries }
}

const sanitizeAggregationRequest = (
  aggregations?: AggregationRequestDto,
): AggregationRequestDto | undefined => {
  const entries = aggregations?.aggs
    ?.map((agg) => {
      if (!agg?.field || !agg?.type) {
        return null
      }

      const sanitizedAgg: Agg = {
        field: agg.field,
        type: agg.type,
      }

      if (agg.name) {
        sanitizedAgg.name = agg.name
      }

      if (Number.isFinite(agg.min)) {
        sanitizedAgg.min = agg.min as number
      }

      if (Number.isFinite(agg.max)) {
        sanitizedAgg.max = agg.max as number
      }

      if (Number.isFinite(agg.buckets)) {
        sanitizedAgg.buckets = agg.buckets as number
      }

      if (Number.isFinite(agg.step)) {
        sanitizedAgg.step = agg.step as number
      }

      return sanitizedAgg
    })
    .filter((value): value is Agg => value !== null)

  if (!entries?.length) {
    return undefined
  }

  return { aggs: entries }
}

const sanitizeFilterRequest = (filters?: FilterRequestDto): FilterRequestDto | undefined => {
  const entries = filters?.filters
    ?.map((filter) => {
      if (!filter?.field || !filter?.operator) {
        return null
      }

      if (filter.operator === 'term') {
        if (!filter.terms?.length) {
          return null
        }

        const sanitized: Filter = {
          field: filter.field,
          operator: filter.operator,
          terms: [...filter.terms],
        }

        return sanitized
      }

      if (filter.operator === 'range') {
        const hasBounds = filter.min != null || filter.max != null

        if (!hasBounds) {
          return null
        }

        const sanitized: Filter = {
          field: filter.field,
          operator: filter.operator,
        }

        if (filter.min != null) {
          sanitized.min = filter.min
        }

        if (filter.max != null) {
          sanitized.max = filter.max
        }

        return sanitized
      }

      return null
    })
    .filter((value): value is Filter => value !== null)

  if (!entries?.length) {
    return undefined
  }

  return { filters: entries }
}
