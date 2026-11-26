import type { OpenDataDatasetDto } from '~~/shared/api-client'
import { useOpenDataService } from '~~/shared/api-client/services/opendata.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import {
  extractBackendErrorDetails,
  logBackendError,
} from '../../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../../utils/cache-headers'

export default defineEventHandler(
  async (event): Promise<OpenDataDatasetDto> => {
    setDomainLanguageCacheHeaders(event, 'public, max-age=300, s-maxage=300')

    const vertical = getRouterParam(event, 'vertical') ?? ''
    if (!/^[a-z0-9][a-z0-9-]*$/.test(vertical)) {
      throw createError({ statusCode: 404, statusMessage: 'Not Found' })
    }

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const openDataService = useOpenDataService(domainLanguage)

    try {
      return await openDataService.fetchImpactDataset(vertical)
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      logBackendError({
        namespace: 'opendata:impact',
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
