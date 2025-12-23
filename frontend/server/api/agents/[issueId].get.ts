import {
  type AgentIssueDto,
  useAgentService,
} from '~~/shared/api-client/services/agents.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import {
  extractBackendErrorDetails,
  logBackendError,
} from '../../utils/log-backend-error'

export default defineEventHandler(async (event): Promise<AgentIssueDto> => {
  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)
  const issueId = event.context.params?.issueId

  if (!issueId) {
    throw createError({ statusCode: 400, statusMessage: 'Missing issueId' })
  }

  const agentService = useAgentService(domainLanguage)

  try {
    return await agentService.getIssue(issueId, domainLanguage)
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    logBackendError({
      namespace: 'agents:issue',
      details: backendError,
    })
    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
