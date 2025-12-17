import type { OpenDataOverviewDto } from '~~/shared/api-client'
import { useOpenDataService } from '~~/shared/api-client/services/opendata.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import {
  extractBackendErrorDetails,
  logBackendError,
} from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

export default defineEventHandler(
  async (event): Promise<OpenDataOverviewDto> => {
    setDomainLanguageCacheHeaders(event, 'public, max-age=300, s-maxage=300')

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const openDataService = useOpenDataService(domainLanguage)

    try {
      return await openDataService.fetchOverview()
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      logBackendError({
        namespace: 'opendata:overview',
        details: backendError,
      })

      throw createError({
        statusCode: backendError.statusCode,
        statusMessage: backendError.statusMessage,
        cause: error,
      })
    }
  }
)
