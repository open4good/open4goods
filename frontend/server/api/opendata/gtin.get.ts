import type { OpenDataDatasetDto } from '~~/shared/api-client'
import { useOpenDataService } from '~~/shared/api-client/services/opendata.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails, logBackendError } from '../../utils/log-backend-error'

export default defineEventHandler(async (event): Promise<OpenDataDatasetDto> => {
  setResponseHeader(event, 'Cache-Control', 'public, max-age=300, s-maxage=300')

  const rawHost = event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const openDataService = useOpenDataService(domainLanguage)

  try {
    return await openDataService.fetchGtinDataset()
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    logBackendError({
      namespace: 'opendata:gtin',
      details: backendError,
    })

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})

