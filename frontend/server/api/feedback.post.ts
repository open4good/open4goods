import {
  FeedbackSubmissionRequestDtoTypeEnum,
  type FeedbackSubmissionResponseDto,
} from '~~/shared/api-client'
import { useFeedbackService } from '~~/shared/api-client/services/feedback.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import {
  extractBackendErrorDetails,
  logBackendError,
} from '../utils/log-backend-error'

type FeedbackType = `${FeedbackSubmissionRequestDtoTypeEnum}`

interface FeedbackFormPayload {
  type?: FeedbackType
  title?: string
  message?: string
  author?: string
  url?: string
  hCaptchaResponse?: string
}

const SUPPORTED_TYPES = new Set<FeedbackType>([
  FeedbackSubmissionRequestDtoTypeEnum.Idea,
  FeedbackSubmissionRequestDtoTypeEnum.Bug,
])

const sanitize = (value: string | undefined | null): string =>
  value?.trim() ?? ''

const normalizeType = (
  value: FeedbackType | undefined
): FeedbackSubmissionRequestDtoTypeEnum | null => {
  if (!value) {
    return null
  }

  const upperCased = value.toUpperCase() as FeedbackType

  if (!SUPPORTED_TYPES.has(upperCased)) {
    return null
  }

  return upperCased as FeedbackSubmissionRequestDtoTypeEnum
}

const MIN_TITLE_LENGTH = 4
const MAX_TITLE_LENGTH = 140
const MIN_MESSAGE_LENGTH = 20
const MAX_MESSAGE_LENGTH = 4000

export default defineEventHandler(
  async (event): Promise<FeedbackSubmissionResponseDto> => {
    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const body = await readBody<FeedbackFormPayload>(event)

    const type = normalizeType(body.type)
    const title = sanitize(body.title)
    const message = sanitize(body.message)
    const author = sanitize(body.author)
    const url = sanitize(body.url)
    const hCaptchaResponse = sanitize(body.hCaptchaResponse)

    if (!type) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Feedback category is required.',
      })
    }

    if (!title || title.length < MIN_TITLE_LENGTH) {
      throw createError({
        statusCode: 400,
        statusMessage:
          'Please provide a concise title (at least 4 characters).',
      })
    }

    if (title.length > MAX_TITLE_LENGTH) {
      throw createError({
        statusCode: 400,
        statusMessage: 'The provided title is too long.',
      })
    }

    if (!message || message.length < MIN_MESSAGE_LENGTH) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Please describe your idea or issue in more detail.',
      })
    }

    if (message.length > MAX_MESSAGE_LENGTH) {
      throw createError({
        statusCode: 400,
        statusMessage: 'The provided description is too long.',
      })
    }

    if (!hCaptchaResponse) {
      throw createError({
        statusCode: 400,
        statusMessage: 'The hCaptcha token is missing.',
      })
    }

    const feedbackService = useFeedbackService(domainLanguage)

    try {
      return await feedbackService.submitFeedback({
        type,
        title,
        message,
        author: author || undefined,
        url: url || undefined,
        hCaptchaResponse,
      })
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      logBackendError({
        namespace: 'feedback:submit',
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
