import type { H3Event } from 'h3'
import type { DatavizChartQueryResponseDto } from '~~/shared/api-client'
import { StatsApi } from '~~/shared/api-client'
import { createBackendApiConfig } from '~~/shared/api-client/services/createBackendApiConfig'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import { setDomainLanguageCacheHeaders } from '../../../utils/cache-headers'
import { extractBackendErrorDetails } from '../../../utils/log-backend-error'

export default defineEventHandler(
  async (event: H3Event): Promise<DatavizChartQueryResponseDto> => {
    setDomainLanguageCacheHeaders(event, 'public, max-age=120, s-maxage=120')

    const query = getQuery(event)
    const verticalId = String(query.verticalId ?? '')

    if (!verticalId) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Missing verticalId query parameter',
      })
    }

    const body = await readBody(event)
    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)
    const statsApi = new StatsApi(createBackendApiConfig())

    try {
      return await statsApi.chartQuery({
        verticalId,
        domainLanguage: domainLanguage as 'fr' | 'en',
        datavizChartQueryRequestDto: body,
      })
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)

      console.error(
        'Error executing chart query:',
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
