import type {
  AggregationRequestDto,
  FilterRequestDto,
  ProductSearchResponseDto,
  SortRequestDto,
} from '~~/shared/api-client'
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
  include?: string[]
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

    try {
      return await productService.searchProducts({
        verticalId: payload.verticalId,
        pageNumber: payload.pageNumber,
        pageSize: payload.pageSize,
        sort: payload.sort,
        aggs: payload.aggs,
        filters: payload.filters,
        query: payload.query,
        include: payload.include,
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
