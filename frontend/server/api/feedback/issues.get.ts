import { ListIssuesTypeEnum, type FeedbackIssueDto } from '~~/shared/api-client'
import { useFeedbackService } from '~~/shared/api-client/services/feedback.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import {
  extractBackendErrorDetails,
  logBackendError,
} from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

type SupportedIssueType = `${ListIssuesTypeEnum}`

const SUPPORTED_TYPES = new Set<SupportedIssueType>([
  ListIssuesTypeEnum.Idea,
  ListIssuesTypeEnum.Bug,
])

const normalizeIssueType = (value: unknown): ListIssuesTypeEnum | undefined => {
  if (typeof value !== 'string' || value.length === 0) {
    return undefined
  }

  const upperCased = value.toUpperCase()

  if (SUPPORTED_TYPES.has(upperCased as SupportedIssueType)) {
    return upperCased as ListIssuesTypeEnum
  }

  return undefined
}

export default defineEventHandler(
  async (event): Promise<FeedbackIssueDto[]> => {
    setDomainLanguageCacheHeaders(event, 'public, max-age=120, s-maxage=120')

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)
    const query = getQuery(event)

    const typeParam = Array.isArray(query.type) ? query.type[0] : query.type
    const normalizedType = normalizeIssueType(typeParam)

    if (typeParam && !normalizedType) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Unsupported feedback issue type.',
      })
    }

    const feedbackService = useFeedbackService(domainLanguage)

    try {
      return await feedbackService.listIssues(normalizedType)
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      logBackendError({
        namespace: 'feedback:issues',
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
