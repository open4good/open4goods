import { useBlogService } from '~~/shared/api-client/services/blog.services'
import type { PageDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'

/**
 * Blog articles API endpoint
 * Handles GET requests for blog articles with caching
 */
export default defineEventHandler(async (event): Promise<PageDto> => {
  // Set cache headers for 1 hour
  setResponseHeader(
    event,
    'Cache-Control',
    'public, max-age=3600, s-maxage=3600'
  )

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const blogService = useBlogService(domainLanguage)

  try {
    // Use the service to fetch articles
    const response = await blogService.getArticles()
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
