import { cachedEventHandler } from 'nitropack/runtime/internal/cache'
import type { H3Event } from 'h3'
import { useCategoriesService } from '~~/shared/api-client/services/categories.services'
import type { VerticalConfigDto } from '~~/shared/api-client'
import { VerticalConfigDtoToJSON } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

type CategoriesListCacheContext = {
  domainLanguage: string
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

  const context: CategoriesListCacheContext = {
    domainLanguage,
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

  const { domainLanguage } = resolveCategoriesListCacheContext(event)
  const categoriesService = useCategoriesService(domainLanguage)

  try {
    const categories = await categoriesService.getCategories()
    return categories.map(category => VerticalConfigDtoToJSON(category))
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
    const { domainLanguage } = resolveCategoriesListCacheContext(event)

    return `${domainLanguage}:all`
  },
})
