import {
  type AgentRequestResponseDto,
  type AgentRequestDto,
  AgentRequestDtoTypeEnum,
  AgentRequestDtoPromptVisibilityEnum,
  useAgentService,
} from '~~/shared/api-client/services/agents.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import {
  extractBackendErrorDetails,
  logBackendError,
} from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

export default defineEventHandler(
  async (event): Promise<AgentRequestResponseDto> => {
    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    // Read body
    const body = await readBody<Partial<AgentRequestDto>>(event)

    // Basic validation
    if (!body || !body.promptUser || !body.promptTemplateId) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Missing required fields',
      })
    }

    const agentService = useAgentService(domainLanguage)

    try {
      return await agentService.submitRequest(
        {
          type: body.type ?? AgentRequestDtoTypeEnum.Feature,
          promptTemplateId: body.promptTemplateId,
          promptUser: body.promptUser,
          promptVisibility:
            body.promptVisibility ?? AgentRequestDtoPromptVisibilityEnum.Public,
          userHandle: body.userHandle,
        },
        domainLanguage
      )
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      logBackendError({
        namespace: 'agents:submit',
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
