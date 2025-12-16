import type { GlobalSearchResponseDto } from '~~/shared/api-client'
import { useSearchService } from '~~/shared/api-client/services/search.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../utils/cache-headers'
import { extractBackendErrorDetails } from '../utils/log-backend-error'

const MIN_QUERY_LENGTH = 2

type SearchQueryParams = {
  query?: string | string[]
}

export default defineEventHandler(
  async (event): Promise<GlobalSearchResponseDto> => {
    setDomainLanguageCacheHeaders(event, 'private, max-age=0, no-cache')

    const { query } = getQuery<SearchQueryParams>(event)
    const normalizedQuery = Array.isArray(query)
      ? query[0]?.trim()
      : typeof query === 'string'
        ? query.trim()
        : ''

    if (!normalizedQuery || normalizedQuery.length < MIN_QUERY_LENGTH) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Query parameter must contain at least two characters.',
      })
    }

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)
    const searchService = useSearchService(domainLanguage)

    try {
      return await searchService.executeGlobalSearch(normalizedQuery)
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error(
        'Global search proxy failed',
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
