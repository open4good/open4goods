import { createError, getRouterParam } from 'h3'
import type { H3Event } from 'h3'
import {
  useAffiliationService,
  type AffiliationRedirectHttpMethod,
  type AffiliationRedirectResponse,
} from '~~/shared/api-client/services/affiliation.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from './log-backend-error'

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

  return Array.isArray(rawUserAgent) ? rawUserAgent[0] : rawUserAgent
}

/**
 * Resolves an affiliation token while preserving the browser request context.
 */
export const resolveAffiliationRedirect = async (
  event: H3Event
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
  const method = resolveMethod(event)

  try {
    return await useAffiliationService(domainLanguage).resolveRedirect({
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
