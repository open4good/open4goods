import type { CategoryNavigationDto } from '~~/shared/api-client'
import { useCategoriesService } from '~~/shared/api-client/services/categories.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

export default defineEventHandler(async (event): Promise<CategoryNavigationDto> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=1800, s-maxage=1800')

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

  const path = typeof query.path === 'string' && query.path.length > 0 ? query.path : undefined

  const rawHost = event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const categoriesService = useCategoriesService(domainLanguage)

  try {
    return await categoriesService.getNavigation({ googleCategoryId, path })
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error('Error fetching category navigation:', backendError.logMessage, backendError)

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
