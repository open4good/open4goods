import { createError, defineEventHandler, getRouterParam } from 'h3'
import type { H3Event } from 'h3'
import { useAffiliationService } from '~~/shared/api-client/services/affiliation.services'
import type {
  AffiliationRedirectResponse,
  AffiliationRedirectHttpMethod,
} from '~~/shared/api-client/services/affiliation.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'

const SUPPORTED_METHODS: AffiliationRedirectHttpMethod[] = ['GET', 'POST']

const resolveMethod = (event: H3Event): AffiliationRedirectHttpMethod => {
  const method = (event.node.req.method ?? 'GET').toUpperCase()

  if (SUPPORTED_METHODS.includes(method as AffiliationRedirectHttpMethod)) {
    return method as AffiliationRedirectHttpMethod
  }

  throw createError({
    statusCode: 405,
    statusMessage: 'Method Not Allowed',
  })
}

const normalizeUserAgent = (
  rawUserAgent: string | string[] | undefined
): string | undefined => {
  if (!rawUserAgent) {
    return undefined
  }

  if (Array.isArray(rawUserAgent)) {
    return rawUserAgent[0]
  }

  return rawUserAgent
}

const handleRedirect = async (
  event: H3Event,
  method: AffiliationRedirectHttpMethod
): Promise<AffiliationRedirectResponse> => {
  const token = getRouterParam(event, 'token')

  if (!token) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Affiliation token is required',
    })
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const userAgent = normalizeUserAgent(event.node.req.headers['user-agent'])

  const affiliationService = useAffiliationService(domainLanguage)

  try {
    return await affiliationService.resolveRedirect({
      token,
      userAgent,
      method,
    })
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error(
      'Error resolving affiliation redirect',
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

export default defineEventHandler(
  async (event): Promise<AffiliationRedirectResponse> => {
    const method = resolveMethod(event)
    return handleRedirect(event, method)
  }
)
