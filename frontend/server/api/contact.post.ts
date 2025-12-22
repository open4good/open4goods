import type { ContactResponseDto } from '~~/shared/api-client'
import { useContactService } from '~~/shared/api-client/services/contact.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../utils/log-backend-error'

interface ContactFormPayload {
  name?: string
  email?: string
  message?: string
  hCaptchaResponse?: string
  templateId?: string
  subject?: string
  sourceRoute?: string
  sourceComponent?: string
  sourcePage?: string
}

const EMAIL_PATTERN =
  /^(?:[a-zA-Z0-9_'^&/+-])+(?:\.(?:[a-zA-Z0-9_'^&/+-])+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}$/u

const sanitizeInput = (value: string): string => value.trim()

export default defineEventHandler(
  async (event): Promise<ContactResponseDto> => {
    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const body = await readBody<ContactFormPayload>(event)

    const name = sanitizeInput(String(body.name ?? ''))
    const email = sanitizeInput(String(body.email ?? ''))
    const message = sanitizeInput(String(body.message ?? ''))
    const hCaptchaResponse = sanitizeInput(String(body.hCaptchaResponse ?? ''))
    const templateId = sanitizeInput(String(body.templateId ?? ''))
    const subject = sanitizeInput(String(body.subject ?? ''))
    const sourceRoute = sanitizeInput(String(body.sourceRoute ?? ''))
    const sourceComponent = sanitizeInput(String(body.sourceComponent ?? ''))
    const sourcePage = sanitizeInput(String(body.sourcePage ?? ''))

    if (!name) {
      throw createError({
        statusCode: 400,
        statusMessage: 'The name field is required.',
      })
    }

    if (name.length < 2 || name.length > 120) {
      throw createError({
        statusCode: 400,
        statusMessage: 'The provided name length is invalid.',
      })
    }

    if (!email || !EMAIL_PATTERN.test(email)) {
      throw createError({
        statusCode: 400,
        statusMessage: 'A valid email address is required.',
      })
    }

    if (!message || message.length < 10) {
      throw createError({
        statusCode: 400,
        statusMessage: 'The message must contain at least 10 characters.',
      })
    }

    if (message.length > 4000) {
      throw createError({
        statusCode: 400,
        statusMessage: 'The message is too long.',
      })
    }

    if (!hCaptchaResponse) {
      throw createError({
        statusCode: 400,
        statusMessage: 'The hCaptcha token is missing.',
      })
    }

    const contactService = useContactService(domainLanguage)

    try {
      return await contactService.submitMessage({
        name,
        email,
        message,
        hCaptchaResponse,
        templateId: templateId || undefined,
        subject: subject || undefined,
        sourceRoute: sourceRoute || undefined,
        sourceComponent: sourceComponent || undefined,
        sourcePage: sourcePage || undefined,
      })
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error(
        'Error forwarding contact message',
        backendError.logMessage,
        backendError
      )

      throw createError({
        statusCode: backendError.statusCode,
        statusMessage: backendError.statusMessage,
        cause: error,
      })
    }
  }
)
