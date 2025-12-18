import type { ShareResolutionRequestDto } from '~~/shared/api-client'
import { useShareResolutionService } from '~~/shared/api-client/services/share-resolution.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'

const isSupportedDomainLanguage = (
  language?: string | null
): language is 'en' | 'fr' => language === 'en' || language === 'fr'

export default defineEventHandler(async event => {
  setDomainLanguageCacheHeaders(event, 'private, max-age=0, no-store')

  const body = await readBody<
    ShareResolutionRequestDto & {
      domainLanguage?: string | null
    }
  >(event)

  if (!body?.url) {
    throw createError({
      statusCode: 400,
      statusMessage: 'URL is required to resolve a shared product',
    })
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const domainLanguage = isSupportedDomainLanguage(body.domainLanguage)
    ? body.domainLanguage
    : resolveDomainLanguage(rawHost).domainLanguage

  try {
    const shareResolutionService = useShareResolutionService(domainLanguage)

    return await shareResolutionService.createResolution({
      url: body.url,
      title: body.title,
      text: body.text,
    })
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error(
      'Failed to create share resolution',
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
