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

export default defineEventHandler(
  async (event): Promise<AgentRequestResponseDto> => {
    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    // Read body
    const body = await readBody<Partial<AgentRequestDto>>(event)

    // Basic validation
    if (
      !body ||
      !body.promptUser ||
      !body.promptTemplateId ||
      !body.promptVariantId
    ) {
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
          promptVariantId: body.promptVariantId,
          promptUser: body.promptUser,
          promptVisibility:
            body.promptVisibility ?? AgentRequestDtoPromptVisibilityEnum.Public,
          userHandle: body.userHandle,
          attributeValues: body.attributeValues,
          captchaToken: body.captchaToken,
          tags: body.tags,
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
