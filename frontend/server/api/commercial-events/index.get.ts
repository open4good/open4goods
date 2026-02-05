import { cachedEventHandler } from 'nitropack/runtime/internal/cache'
import { createError } from 'h3'
import type { H3Event } from 'h3'
import type { CommercialEvent } from '~~/shared/api-client'
import { useCommercialEventsService } from '~~/shared/api-client/services/commercial-events.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

type CommercialEventsCacheContext = {
  domainLanguage: string
}

declare module 'h3' {
  interface H3EventContext {
    commercialEventsCacheContext?: CommercialEventsCacheContext
  }
}

const resolveCommercialEventsCacheContext = (
  event: H3Event
): CommercialEventsCacheContext => {
  if (event.context.commercialEventsCacheContext) {
    return event.context.commercialEventsCacheContext
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const context: CommercialEventsCacheContext = {
    domainLanguage,
  }

  event.context.commercialEventsCacheContext = context

  return context
}

const handler = async (event: H3Event): Promise<CommercialEvent[]> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=86400, s-maxage=86400')

  const { domainLanguage } = resolveCommercialEventsCacheContext(event)
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
}

export default cachedEventHandler(handler, {
  name: 'commercial-events',
  maxAge: 86400,
  getKey: event => {
    const { domainLanguage } = resolveCommercialEventsCacheContext(event)

    return `${domainLanguage}:all`
  },
})
