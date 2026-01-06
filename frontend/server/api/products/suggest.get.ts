import { createError, defineEventHandler, getQuery } from 'h3'
import type { SearchSuggestResponseDto } from '~~/shared/api-client'
import { useProductService } from '~~/shared/api-client/services/products.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'

const MIN_QUERY_LENGTH = 2

type SearchQueryParams = {
  query?: string | string[]
}

export default defineEventHandler(
  async (event): Promise<SearchSuggestResponseDto> => {
    setDomainLanguageCacheHeaders(event, 'private, max-age=0, no-cache')

    const { query } = getQuery<SearchQueryParams>(event)
    const normalizedQuery = Array.isArray(query)
      ? query[0]?.trim()
      : typeof query === 'string'
        ? query.trim()
        : ''

    if (!normalizedQuery || normalizedQuery.length < MIN_QUERY_LENGTH) {
      event.node.res.statusCode = 200
      return {
        categoryMatches: [],
        productMatches: [],
      }
    }

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)
    const productService = useProductService(domainLanguage)

    try {
      return await productService.fetchSearchSuggestions(normalizedQuery)
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error(
        'Search suggest proxy failed',
        backendError.logMessage,
        backendError
      )

      if (
        backendError.isResponseError &&
        backendError.statusCode >= 400 &&
        backendError.statusCode < 500
      ) {
        throw createError({
          statusCode: backendError.statusCode,
          statusMessage: backendError.statusMessage,
          cause: error,
        })
      }

      event.node.res.statusCode = 200
      return {
        categoryMatches: [],
        productMatches: [],
      }
    }
  }
)
