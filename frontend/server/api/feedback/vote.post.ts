import type { FeedbackVoteResponseDto } from '~~/shared/api-client'
import { useFeedbackService } from '~~/shared/api-client/services/feedback.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import {
  extractBackendErrorDetails,
  logBackendError,
} from '../../utils/log-backend-error'

interface VotePayload {
  issueId?: string
}

const sanitize = (value: string | undefined | null): string =>
  value?.trim() ?? ''

export default defineEventHandler(
  async (event): Promise<FeedbackVoteResponseDto> => {
    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const body = await readBody<VotePayload>(event)
    const issueId = sanitize(body.issueId)

    if (!issueId) {
      throw createError({
        statusCode: 400,
        statusMessage: 'The issueId field is required to register a vote.',
      })
    }

    const feedbackService = useFeedbackService(domainLanguage)

    try {
      return await feedbackService.voteOnIssue(issueId)
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      logBackendError({
        namespace: 'feedback:votes:vote',
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
