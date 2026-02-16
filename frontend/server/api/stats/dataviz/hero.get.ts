import type { H3Event } from 'h3'
import type { DatavizHeroStatsDto } from '~~/shared/api-client'
import { StatsApi } from '~~/shared/api-client'
import { createBackendApiConfig } from '~~/shared/api-client/services/createBackendApiConfig'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import { setDomainLanguageCacheHeaders } from '../../../utils/cache-headers'
import { extractBackendErrorDetails } from '../../../utils/log-backend-error'

export default defineEventHandler(
  async (event: H3Event): Promise<DatavizHeroStatsDto> => {
    setDomainLanguageCacheHeaders(event, 'public, max-age=300, s-maxage=300')

    const query = getQuery(event)
    const verticalId = String(query.verticalId ?? '')

    if (!verticalId) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Missing verticalId query parameter',
      })
    }

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)
    const statsApi = new StatsApi(createBackendApiConfig())

    try {
      return await statsApi.datavizHero({
        verticalId,
        domainLanguage: domainLanguage as 'fr' | 'en',
      })
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)

      console.error(
        'Error fetching dataviz hero stats:',
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
