import type { H3Event } from 'h3'
import type { MetriksReportDto } from '~~/shared/api-client'
import { useMetriksService } from '~~/shared/api-client/services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'

const resolveRequest = (event: H3Event) => {
  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)
  const query = getQuery(event)
  const limit = Number(query.limit ?? 12)
  const includePayload = query.includePayload === 'true'

  return { domainLanguage, limit, includePayload }
}

export default defineEventHandler(async (event): Promise<MetriksReportDto> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=300, s-maxage=300')

  const { domainLanguage, limit, includePayload } = resolveRequest(event)
  const metriksService = useMetriksService(domainLanguage)

  try {
    return await metriksService.getReport(limit, includePayload)
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)

    console.error('Error fetching metriks report:', backendError.logMessage)

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
