import { isbot } from 'isbot'
import {
  createError,
  getRouterParam,
  send,
  setResponseHeader,
  setResponseStatus,
} from 'h3'
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

const MAX_LOGGED_USER_AGENT_LENGTH = 80
const MAX_LOGGED_TOKEN_LENGTH = 6

/**
 * Parses the comma-separated extra user-agent patterns (case-insensitive substrings).
 */
const parseBlockedPatterns = (raw: unknown): string[] =>
  typeof raw === 'string'
    ? raw
        .split(',')
        .map(pattern => pattern.trim().toLowerCase())
        .filter(Boolean)
    : []

const isBlockedUserAgent = (event: H3Event, userAgent?: string): boolean => {
  if (!userAgent?.trim()) {
    return true
  }

  if (isbot(userAgent)) {
    return true
  }

  const config = useRuntimeConfig(event) as {
    affiliationBlockedUserAgents?: string
  }
  const lowered = userAgent.toLowerCase()

  return parseBlockedPatterns(config.affiliationBlockedUserAgents).some(
    pattern => lowered.includes(pattern)
  )
}

/**
 * Sends a short text/plain error: /contrib/** must never render an HTML page
 * (the Plausible script it carries would count robot visits).
 */
export const sendContribPlainError = (
  event: H3Event,
  error: unknown
): Promise<void> => {
  const { statusCode, statusMessage } = (error ?? {}) as {
    statusCode?: number
    statusMessage?: string
  }
  const status = statusCode && statusCode >= 400 ? statusCode : 500

  setResponseStatus(event, status)
  setResponseHeader(event, 'Content-Type', 'text/plain; charset=utf-8')
  setResponseHeader(event, 'X-Robots-Tag', 'noindex, nofollow')
  setResponseHeader(event, 'Cache-Control', 'no-store')

  return send(event, statusMessage || 'Error', 'text/plain; charset=utf-8')
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

  if (isBlockedUserAgent(event, userAgent)) {
    console.warn(
      `Affiliation redirect refused for robot user agent "${(userAgent ?? '').slice(0, MAX_LOGGED_USER_AGENT_LENGTH)}" token "${token.slice(0, MAX_LOGGED_TOKEN_LENGTH)}…"`
    )
    throw createError({ statusCode: 403, statusMessage: 'Forbidden' })
  }

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
