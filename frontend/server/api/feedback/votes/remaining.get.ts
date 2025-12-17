import type { FeedbackRemainingVotesDto } from '~~/shared/api-client'
import { useFeedbackService } from '~~/shared/api-client/services/feedback.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import {
  extractBackendErrorDetails,
  logBackendError,
} from '../../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../../utils/cache-headers'

export default defineEventHandler(
  async (event): Promise<FeedbackRemainingVotesDto> => {
    setDomainLanguageCacheHeaders(event, 'public, max-age=60, s-maxage=60')

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const feedbackService = useFeedbackService(domainLanguage)

    try {
      return await feedbackService.remainingVotes()
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      logBackendError({
        namespace: 'feedback:votes:remaining',
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
