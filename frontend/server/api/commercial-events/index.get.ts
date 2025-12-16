import { createError, defineEventHandler } from 'h3'
import type { CommercialEvent } from '~~/shared/api-client'
import { useCommercialEventsService } from '~~/shared/api-client/services/commercial-events.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

export default defineEventHandler(async (event): Promise<CommercialEvent[]> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=600, s-maxage=600')

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const service = useCommercialEventsService(domainLanguage)

  try {
    return await service.listCommercialEvents()
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error(
      'Error fetching commercial events:',
      backendError.logMessage,
      backendError
    )

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
