import { cachedEventHandler } from 'nitropack/runtime/internal/cache'
import type { H3Event } from 'h3'
import { useAssistantConfigsService } from '~~/shared/api-client/services/assistant-configs.services'
import type { AssistantConfigDto } from '~~/shared/api-client'
import { AssistantConfigDtoToJSON } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

type AssistantConfigsCacheContext = {
  domainLanguage: string
}

declare module 'h3' {
  interface H3EventContext {
    assistantConfigsCacheContext?: AssistantConfigsCacheContext
  }
}

const resolveAssistantConfigsCacheContext = (
  event: H3Event
): AssistantConfigsCacheContext => {
  if (event.context.assistantConfigsCacheContext) {
    return event.context.assistantConfigsCacheContext
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const context: AssistantConfigsCacheContext = {
    domainLanguage,
  }

  event.context.assistantConfigsCacheContext = context

  return context
}

/**
 * Assistant configs API endpoint
 * Handles GET requests for assistant configs with caching
 */
const handler = async (event: H3Event): Promise<AssistantConfigDto[]> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=900, s-maxage=900')

  const { domainLanguage } = resolveAssistantConfigsCacheContext(event)
  const assistantsService = useAssistantConfigsService(domainLanguage)

  try {
    const configs = await assistantsService.getAssistantConfigs()
    return configs.map(config => AssistantConfigDtoToJSON(config))
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error(
      'Error fetching assistant configs:',
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
  name: 'assistant-configs-list',
  maxAge: 900,
  getKey: event => {
    const { domainLanguage } = resolveAssistantConfigsCacheContext(event)

    return `${domainLanguage}:all`
  },
})
