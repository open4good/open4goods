import { createError, defineEventHandler } from 'h3'

import { useAdministrationService } from '~~/shared/api-client/services/administration.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../../../utils/cache-headers'
import {
  extractBackendErrorDetails,
  logBackendError,
} from '../../../utils/log-backend-error'

export default defineEventHandler(async event => {
  setDomainLanguageCacheHeaders(event, 'private, no-store')

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)
  const administrationService = useAdministrationService(domainLanguage)

  try {
    await administrationService.resetCache()
    return { success: true }
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)

    logBackendError({
      namespace: 'administration:cache:reset',
      details: backendError,
    })

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
