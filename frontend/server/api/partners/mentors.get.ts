import type { StaticPartnerDto } from '~~/shared/api-client'
import { usePartnerService } from '~~/shared/api-client/services/partners.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import {
  extractBackendErrorDetails,
  logBackendError,
} from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

export default defineEventHandler(
  async (event): Promise<StaticPartnerDto[]> => {
    setDomainLanguageCacheHeaders(event, 'public, max-age=900, s-maxage=900')

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const partnerService = usePartnerService(domainLanguage)

    try {
      return await partnerService.fetchMentorPartners()
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)

      logBackendError({
        namespace: 'partners:mentors',
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
