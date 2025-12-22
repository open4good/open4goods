import {
  type AgentActivityDto,
  useAgentService,
} from '~~/shared/api-client/services/agents.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import {
  extractBackendErrorDetails,
  logBackendError,
} from '../../utils/log-backend-error'

export default defineEventHandler(
  async (event): Promise<AgentActivityDto[]> => {
    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    // Cache short term as it updates frequently
    setDomainLanguageCacheHeaders(event, 'public, max-age=60, s-maxage=120')

    const agentService = useAgentService(domainLanguage)

    try {
      return await agentService.listActivity(domainLanguage)
    } catch (error) {
      // Log but return empty list to not break UI
      const backendError = await extractBackendErrorDetails(error)
      logBackendError({
        namespace: 'agents:activity',
        details: backendError,
      })
      return []
    }
  }
)
