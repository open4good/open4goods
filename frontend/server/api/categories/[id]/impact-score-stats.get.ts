import { useCategoriesService } from '~~/shared/api-client/services/categories.services'
import type { ImpactScoreStatsDto } from '~~/shared/api-client'
import { ImpactScoreStatsDtoToJSON } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../../utils/cache-headers'

/**
 * Measured ImpactScore figures of a vertical (counts, coverage per criterion,
 * distribution of the scores) for its method page.
 */
export default defineEventHandler(
  async (event): Promise<ImpactScoreStatsDto> => {
    setDomainLanguageCacheHeaders(event, 'public, max-age=900, s-maxage=900')

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
      const stats = await categoriesService.getImpactScoreStats(categoryId)
      return ImpactScoreStatsDtoToJSON(stats)
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error(
        'Error fetching ImpactScore stats:',
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
