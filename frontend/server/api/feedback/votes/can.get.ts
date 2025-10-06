import type { FeedbackVoteEligibilityDto } from '~~/shared/api-client'
import { useFeedbackService } from '~~/shared/api-client/services/feedback.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails, logBackendError } from '../../../utils/log-backend-error'

export default defineEventHandler(async (event): Promise<FeedbackVoteEligibilityDto> => {
  setResponseHeader(event, 'Cache-Control', 'public, max-age=60, s-maxage=60')

  const rawHost = event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const feedbackService = useFeedbackService(domainLanguage)

  try {
    return await feedbackService.canVote()
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    logBackendError({
      namespace: 'feedback:votes:can',
      details: backendError,
    })

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
