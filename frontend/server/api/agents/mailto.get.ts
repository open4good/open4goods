import { useAgentService } from '~~/shared/api-client/services/agents.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import {
  extractBackendErrorDetails,
  logBackendError,
} from '../../utils/log-backend-error'

export default defineEventHandler(async (event): Promise<string> => {
  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)
  const { agentId } = getQuery(event)

  if (!agentId || typeof agentId !== 'string') {
    throw createError({ statusCode: 400, message: 'Agent ID required' })
  }

  const agentService = useAgentService(domainLanguage)

  try {
    return await agentService.getMailto(agentId, domainLanguage)
  } catch (error) {
    // Return empty string on error allowing UI to handle fallback grace
    const backendError = await extractBackendErrorDetails(error)
    logBackendError({
      namespace: 'agents:mailto',
      details: backendError,
    })
    return ''
  }
})
