import { cachedEventHandler } from 'nitropack/runtime/internal/cache'
import { getQuery } from 'h3'
import type { H3Event } from 'h3'
import { useCategoriesService } from '~~/shared/api-client/services/categories.services'
import type { VerticalConfigDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

type CategoriesListCacheContext = {
  domainLanguage: string
  onlyEnabled: boolean
}

declare module 'h3' {
  interface H3EventContext {
    categoriesListCacheContext?: CategoriesListCacheContext
  }
}

const resolveCategoriesListCacheContext = (
  event: H3Event
): CategoriesListCacheContext => {
  if (event.context.categoriesListCacheContext) {
    return event.context.categoriesListCacheContext
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)
  const query = getQuery(event)
  const onlyEnabledParam = Array.isArray(query.onlyEnabled)
    ? query.onlyEnabled[0]
    : query.onlyEnabled

  const onlyEnabled = onlyEnabledParam === 'true' || onlyEnabledParam === true

  const context: CategoriesListCacheContext = {
    domainLanguage,
    onlyEnabled,
  }

  event.context.categoriesListCacheContext = context

  return context
}

/**
 * Categories API endpoint
 * Handles GET requests for categories with caching
 */
const handler = async (event: H3Event): Promise<VerticalConfigDto[]> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')

  const { domainLanguage, onlyEnabled } =
    resolveCategoriesListCacheContext(event)
  const categoriesService = useCategoriesService(domainLanguage)

  try {
    return await categoriesService.getCategories(onlyEnabled)
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
}

export default cachedEventHandler(handler, {
  name: 'categories-list',
  maxAge: 3600,
  getKey: event => {
    const { domainLanguage, onlyEnabled } =
      resolveCategoriesListCacheContext(event)

    return `${domainLanguage}:${onlyEnabled}`
  },
})
