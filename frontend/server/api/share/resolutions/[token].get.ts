import { useShareResolutionService } from '~~/shared/api-client/services/share-resolution.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../../../utils/cache-headers'
import { extractBackendErrorDetails } from '../../../utils/log-backend-error'

const isSupportedDomainLanguage = (
  language?: string | null
): language is 'en' | 'fr' => language === 'en' || language === 'fr'

type ShareResolutionParams = {
  token?: string
}

type ShareResolutionQuery = {
  domainLanguage?: string | null
}

export default defineEventHandler(async event => {
  setDomainLanguageCacheHeaders(event, 'private, max-age=0, no-store')

  const { token } = getRouterParams<ShareResolutionParams>(event)
  const { domainLanguage: queryLanguage } = getQuery<ShareResolutionQuery>(event)

  if (!token) {
    throw createError({ statusCode: 400, statusMessage: 'Missing token' })
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const domainLanguage = isSupportedDomainLanguage(queryLanguage)
    ? queryLanguage
    : resolveDomainLanguage(rawHost).domainLanguage

  try {
    const shareResolutionService = useShareResolutionService(domainLanguage)
    return await shareResolutionService.getResolution(token)
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error(
      'Failed to fetch share resolution status',
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
