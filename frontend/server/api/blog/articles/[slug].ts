import { useBlogService } from '~~/shared/api-client/services/blog.services'
import type { BlogPostDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../../utils/cache-headers'

/**
 * Blog article by slug API endpoint
 * Handles GET requests for a single blog article
 */
export default defineEventHandler(async (event): Promise<BlogPostDto> => {
  // Cache the article for one hour
  setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')

  const slug = getRouterParam(event, 'slug')

  if (!slug) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Article slug is required',
    })
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const blogService = useBlogService(domainLanguage)

  try {
    // Use the service to fetch the article
    const response = await blogService.getArticleBySlug(slug)
    return response
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error(
      'Error fetching blog article:',
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
