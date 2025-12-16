import { cachedEventHandler } from 'nitropack/runtime/internal/cache'
import { getQuery } from 'h3'
import type { H3Event } from 'h3'
import type { CategoryNavigationDto } from '~~/shared/api-client'
import { useCategoriesService } from '~~/shared/api-client/services/categories.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

type NavigationCacheContext = {
  domainLanguage: string
  googleCategoryId?: number
  path?: string
}

declare module 'h3' {
  interface H3EventContext {
    categoryNavigationCacheContext?: NavigationCacheContext
  }
}

const resolveNavigationCacheContext = (
  event: H3Event
): NavigationCacheContext => {
  if (event.context.categoryNavigationCacheContext) {
    return event.context.categoryNavigationCacheContext
  }

  const query = getQuery(event)

  const googleCategoryId = (() => {
    const rawValue = query.googleCategoryId
    if (typeof rawValue === 'string' && rawValue.length > 0) {
      const parsed = Number.parseInt(rawValue, 10)
      if (Number.isFinite(parsed)) {
        return parsed
      }

      throw createError({
        statusCode: 400,
        statusMessage: 'googleCategoryId must be a valid integer when provided',
      })
    }

    return undefined
  })()

  const path =
    typeof query.path === 'string' && query.path.length > 0
      ? query.path
      : undefined

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const context: NavigationCacheContext = {
    domainLanguage,
    googleCategoryId,
    path,
  }

  event.context.categoryNavigationCacheContext = context

  return context
}

const handler = async (event: H3Event): Promise<CategoryNavigationDto> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=1800, s-maxage=1800')

  const { domainLanguage, googleCategoryId, path } =
    resolveNavigationCacheContext(event)
  const categoriesService = useCategoriesService(domainLanguage)

  try {
    return await categoriesService.getNavigation({ googleCategoryId, path })
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error(
      'Error fetching category navigation:',
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
  name: 'category-navigation',
  maxAge: 1800,
  getKey: event => {
    const { domainLanguage, googleCategoryId, path } =
      resolveNavigationCacheContext(event)

    return JSON.stringify({
      domainLanguage,
      googleCategoryId: googleCategoryId ?? null,
      path: path ?? null,
    })
  },
})
