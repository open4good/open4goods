import { useAssistantConfigsService } from '~~/shared/api-client/services/assistant-configs.services'
import type { NudgeToolConfigDto } from '~~/shared/api-client'
import { NudgeToolConfigDtoToJSON } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

export default defineEventHandler(
  async (event): Promise<NudgeToolConfigDto> => {
    setDomainLanguageCacheHeaders(event, 'public, max-age=900, s-maxage=900')

    const assistantIdParam = getRouterParam(event, 'id')
    if (!assistantIdParam) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Assistant id is required',
      })
    }

    const assistantId = decodeURIComponent(assistantIdParam)

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const assistantsService = useAssistantConfigsService(domainLanguage)

    try {
      const assistantConfig =
        await assistantsService.getAssistantConfigById(assistantId)
      return NudgeToolConfigDtoToJSON(assistantConfig)
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error(
        'Error fetching assistant config detail:',
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
)
