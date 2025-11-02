import { getQuery } from 'h3'
import type { GlobalSearchResponseDto } from '~~/shared/api-client'
import { useSearchService } from '~~/shared/api-client/services/search.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../utils/cache-headers'

export default defineEventHandler(async (event): Promise<GlobalSearchResponseDto> => {
  setDomainLanguageCacheHeaders(event, 'private, no-store')

  const query = getQuery(event)
  const rawQuery = Array.isArray(query.q) ? query.q[0] : query.q
  const normalizedQuery = rawQuery?.toString().trim()

  if (!normalizedQuery) {
    throw createError({ statusCode: 400, statusMessage: 'Query parameter "q" is required.' })
  }

  const rawHost = event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const searchService = useSearchService(domainLanguage)

  try {
    return await searchService.globalSearch(normalizedQuery)
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error('Error executing global search:', backendError.logMessage, backendError)

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
