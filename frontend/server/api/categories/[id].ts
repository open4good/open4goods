import { useCategoriesService } from '~~/shared/api-client/services/categories.services'
import type { VerticalConfigFullDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

export default defineEventHandler(
  async (event): Promise<VerticalConfigFullDto> => {
    setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')

    const categoryIdParam = getRouterParam(event, 'id')
    if (!categoryIdParam) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Category id is required',
      })
    }

    const categoryId = decodeURIComponent(categoryIdParam)

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const categoriesService = useCategoriesService(domainLanguage)

    try {
      return await categoriesService.getCategoryById(categoryId)
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error(
        'Error fetching category detail:',
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
)
