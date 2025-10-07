import { getQuery } from 'h3'
import { useCategoriesService } from '~~/shared/api-client/services/categories.services'
import type { VerticalConfigDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'

/**
 * Categories API endpoint
 * Handles GET requests for categories with caching
 */
export default defineEventHandler(async (event): Promise<VerticalConfigDto[]> => {
  // Set cache headers for 1 hour
  setResponseHeader(
    event,
    'Cache-Control',
    'public, max-age=3600, s-maxage=3600'
  )

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const categoriesService = useCategoriesService(domainLanguage)
  const query = getQuery(event)
  const onlyEnabledParam = Array.isArray(query.onlyEnabled)
    ? query.onlyEnabled[0]
    : query.onlyEnabled

  const onlyEnabled = onlyEnabledParam === 'true' || onlyEnabledParam === true

  try {
    const response = await categoriesService.getCategories(onlyEnabled)
    return response
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error(
      'Error fetching categories:',
      backendError.logMessage,
      backendError
    )

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
