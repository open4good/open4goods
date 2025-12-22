import {
  type AgentTemplateDto,
  useAgentService,
} from '~~/shared/api-client/services/agents.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import {
  extractBackendErrorDetails,
  logBackendError,
} from '../../utils/log-backend-error'

export default defineEventHandler(
  async (event): Promise<AgentTemplateDto[]> => {
    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    setDomainLanguageCacheHeaders(event, 'public, max-age=300, s-maxage=600')

    const agentService = useAgentService(domainLanguage)

    try {
      return await agentService.listTemplates(domainLanguage)
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      logBackendError({
        namespace: 'agents:templates',
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
