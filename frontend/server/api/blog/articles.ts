import { getQuery } from 'h3'

import { useBlogService } from '~~/shared/api-client/services/blog.services'
import type { PageDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

/**
 * Blog articles API endpoint
 * Handles GET requests for blog articles with caching
 */
export default defineEventHandler(async (event): Promise<PageDto> => {
  // Set cache headers for 1 hour
  setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const blogService = useBlogService(domainLanguage)
  const query = getQuery(event)
  const pageNumberParam = Array.isArray(query.pageNumber)
    ? query.pageNumber[0]
    : query.pageNumber
  const pageSizeParam = Array.isArray(query.pageSize)
    ? query.pageSize[0]
    : query.pageSize
  const tagParam = Array.isArray(query.tag) ? query.tag[0] : query.tag

  const pageNumber = pageNumberParam
    ? Number.parseInt(pageNumberParam, 10)
    : undefined
  const pageSize = pageSizeParam
    ? Number.parseInt(pageSizeParam, 10)
    : undefined

  try {
    // Use the service to fetch articles
    const response = await blogService.getArticles({
      pageNumber: Number.isNaN(pageNumber) ? undefined : pageNumber,
      pageSize: Number.isNaN(pageSize) ? undefined : pageSize,
      tag: typeof tagParam === 'string' ? tagParam : undefined,
    })
    return response
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    // Log the error for debugging
    console.error(
      'Error fetching blog articles:',
      backendError.logMessage,
      backendError
    )

    // Forward backend status code and message when available
    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
