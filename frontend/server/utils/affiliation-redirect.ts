import { isbot } from 'isbot'
import {
  createError,
  getCookie,
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
import {
  isSiteReferer,
  NAVIGATION_SIGNAL_COOKIE_NAME,
  NAVIGATION_SIGNAL_COOKIE_VALUE,
} from '~~/shared/utils/navigation-signal'

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

const MAX_ORIGIN_LENGTH = 200

const firstHeader = (
  value: string | string[] | undefined
): string | undefined => (Array.isArray(value) ? value[0] : value)

/**
 * Returns the path (no query, no fragment, max 200 chars) of the same-site
 * Referer, or undefined when absent, invalid or from another host.
 */
export const resolveAffiliationOrigin = (
  event: H3Event
): string | undefined => {
  const referer = firstHeader(event.node.req.headers.referer)
  const host = firstHeader(
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  )
    ?.split(',')[0]
    ?.trim()
    .toLowerCase()

  if (!referer || !host) {
    return undefined
  }

  try {
    const url = new URL(referer)
    if (url.host.toLowerCase() !== host || !url.pathname.startsWith('/')) {
      return undefined
    }
    return url.pathname.slice(0, MAX_ORIGIN_LENGTH)
  } catch {
    return undefined
  }
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

export const MISSING_NAVIGATION_SIGNAL_MESSAGE =
  'Ouvrez ce lien depuis la fiche produit sur nudger.fr'

const NAVIGATION_SIGNAL_LOG_INTERVAL_MS = 60_000

const missingSignalLog = { lastLoggedAt: 0, refusedSinceLastLog: 0 }

/**
 * Kill switch (NUXT_AFFILIATION_REQUIRE_NAVIGATION_SIGNAL): only an explicit
 * false disables the check, so a missing or mistyped value keeps it on.
 */
const isNavigationSignalRequired = (event: H3Event): boolean => {
  const { affiliationRequireNavigationSignal: value } = useRuntimeConfig(
    event
  ) as { affiliationRequireNavigationSignal?: unknown }

  if (typeof value === 'string') {
    return !['false', '0', 'off', 'no'].includes(value.trim().toLowerCase())
  }

  return value !== false && value !== 0
}

/**
 * A real visitor follows /contrib/ links from a page of the site: the request
 * carries a site Referer or the navigation-signal cookie (GOU-1184).
 */
const hasNavigationSignal = (
  event: H3Event,
  rawHost: string | string[] | undefined
): boolean =>
  isSiteReferer(event.node.req.headers.referer, rawHost) ||
  getCookie(event, NAVIGATION_SIGNAL_COOKIE_NAME) ===
    NAVIGATION_SIGNAL_COOKIE_VALUE

/**
 * Aggregated warning: at most one line per interval with the refusal count
 * and one truncated sample, never the full token nor the client IP.
 */
const logMissingNavigationSignal = (token: string, userAgent?: string) => {
  missingSignalLog.refusedSinceLastLog += 1
  const now = Date.now()

  if (now - missingSignalLog.lastLoggedAt < NAVIGATION_SIGNAL_LOG_INTERVAL_MS) {
    return
  }

  console.warn(
    `Affiliation redirect refused without navigation signal: ${missingSignalLog.refusedSinceLastLog} request(s) since last report (sample user agent "${(userAgent ?? '').slice(0, MAX_LOGGED_USER_AGENT_LENGTH)}" token "${token.slice(0, MAX_LOGGED_TOKEN_LENGTH)}…")`
  )
  missingSignalLog.lastLoggedAt = now
  missingSignalLog.refusedSinceLastLog = 0
}

const PLAIN_ERROR_BODIES: Record<number, string> = {
  400: 'Bad Request',
  403: 'Forbidden',
  404: 'Not Found',
  405: 'Method Not Allowed',
  410: 'Gone',
  429: 'Too Many Requests',
}

/**
 * Sends a short text/plain error: /contrib/** must never render an HTML page
 * (the Plausible script it carries would count robot visits).
 */
export const sendContribPlainError = (
  event: H3Event,
  error: unknown
): Promise<void> => {
  const { statusCode } = (error ?? {}) as { statusCode?: number }
  const status = statusCode && statusCode >= 400 ? statusCode : 500

  setResponseStatus(event, status)
  setResponseHeader(event, 'Content-Type', 'text/plain; charset=utf-8')
  setResponseHeader(event, 'X-Robots-Tag', 'noindex, nofollow')
  setResponseHeader(event, 'Cache-Control', 'no-store')

  // Short generic body: the upstream problem JSON (front-api) is not echoed.
  return send(
    event,
    PLAIN_ERROR_BODIES[status] ?? 'Error',
    'text/plain; charset=utf-8'
  )
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

  if (
    isNavigationSignalRequired(event) &&
    !hasNavigationSignal(event, rawHost)
  ) {
    logMissingNavigationSignal(token, userAgent)
    throw createError({
      statusCode: 403,
      statusMessage: MISSING_NAVIGATION_SIGNAL_MESSAGE,
    })
  }

  try {
    return await useAffiliationService(domainLanguage).resolveRedirect({
      token,
      userAgent,
      method,
      origin: resolveAffiliationOrigin(event),
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
