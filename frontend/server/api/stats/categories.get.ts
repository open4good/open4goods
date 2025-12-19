import { cachedEventHandler } from 'nitropack/runtime/internal/cache'
import type { H3Event } from 'h3'
import type { CategoriesStatsDto } from '~~/shared/api-client'
import { useStatsService } from '~~/shared/api-client/services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'

type CategoriesStatsCacheContext = {
  domainLanguage: string
}

declare module 'h3' {
  interface H3EventContext {
    categoriesStatsCacheContext?: CategoriesStatsCacheContext
  }
}

const resolveCategoriesStatsCacheContext = (
  event: H3Event
): CategoriesStatsCacheContext => {
  if (event.context.categoriesStatsCacheContext) {
    return event.context.categoriesStatsCacheContext
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const context: CategoriesStatsCacheContext = { domainLanguage }

  event.context.categoriesStatsCacheContext = context

  return context
}

const handler = async (event: H3Event): Promise<CategoriesStatsDto> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=300, s-maxage=300')

  const { domainLanguage } = resolveCategoriesStatsCacheContext(event)
  const statsService = useStatsService(domainLanguage)

  try {
    return await statsService.getCategoriesStats()
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)

    console.error(
      'Error fetching categories stats:',
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
  name: 'categories-stats',
  maxAge: 300,
  getKey: event => {
    const { domainLanguage } = resolveCategoriesStatsCacheContext(event)

    return domainLanguage
  },
})
