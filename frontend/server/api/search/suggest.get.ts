import { createError, defineEventHandler, getQuery } from 'h3'
import type { SearchSuggestResponseDto } from '~~/shared/api-client'
import { useSearchService } from '~~/shared/api-client/services/search.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'

const MIN_QUERY_LENGTH = 2

type SuggestQueryParams = {
  query?: string | string[]
}

const emptyResponse: SearchSuggestResponseDto = {
  categoryMatches: [],
  productMatches: [],
}

export default defineEventHandler(
  async (event): Promise<SearchSuggestResponseDto> => {
    setDomainLanguageCacheHeaders(event, 'private, max-age=0, no-cache')

    const { query } = getQuery<SuggestQueryParams>(event)
    const normalizedQuery = Array.isArray(query)
      ? query[0]?.trim()
      : typeof query === 'string'
        ? query.trim()
        : ''

    if (!normalizedQuery || normalizedQuery.length < MIN_QUERY_LENGTH) {
      return emptyResponse
    }

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)
    const searchService = useSearchService(domainLanguage)

    try {
      return await searchService.fetchSearchSuggestions(normalizedQuery)
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error(
        'Search suggestion proxy failed',
        backendError.logMessage,
        backendError
      )

      const isClientError =
        backendError.isResponseError &&
        backendError.statusCode >= 400 &&
        backendError.statusCode < 500

      if (isClientError) {
        throw createError({
          statusCode: backendError.statusCode,
          statusMessage: backendError.statusMessage,
          cause: error,
        })
      }

      event.node.res.statusCode = 200

      return emptyResponse
    }
  }
)
