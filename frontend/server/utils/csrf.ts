import { randomUUID } from 'node:crypto'

import type { H3Event } from 'h3'
import { createError, getCookie, getRequestHeader } from 'h3'

import {
  CSRF_COOKIE_NAME,
  CSRF_HEADER_NAME,
  isSafeMethod,
} from '~~/shared/utils/csrf'

const getRequestHost = (event: H3Event) =>
  getRequestHeader(event, 'x-forwarded-host') || getRequestHeader(event, 'host')

const normaliseHost = (host?: string | null) => host?.toLowerCase()

const resolveOriginHost = (origin?: string | null) => {
  if (!origin) {
    return undefined
  }

  if (origin === 'null') {
    return 'null'
  }

  try {
    return new URL(origin).host.toLowerCase()
  } catch {
    return undefined
  }
}

const resolveRefererHost = (referer?: string | null) => {
  if (!referer) {
    return undefined
  }

  try {
    return new URL(referer).host.toLowerCase()
  } catch {
    return undefined
  }
}

/**
 * Exposes the request's CSRF token to the internal SSR `$fetch` calls through
 * `event.context.csrfToken`, without ever issuing a cookie: a `Set-Cookie` on an
 * anonymous page response would keep the CDN from caching its HTML (GOU-2700).
 * When the browser has no token yet, a per-render token is generated; it only
 * lives in this server-side context. The browser creates its own token before
 * its first unsafe call (`app/utils/_csrf-fetch.ts`).
 */
export const ensureCsrfContextToken = (event: H3Event) => {
  const token = getCookie(event, CSRF_COOKIE_NAME) || randomUUID()
  event.context.csrfToken = token
  return token
}

export const assertSameOrigin = (event: H3Event) => {
  const host = normaliseHost(getRequestHost(event))
  const originHost = resolveOriginHost(getRequestHeader(event, 'origin'))
  const refererHost = resolveRefererHost(getRequestHeader(event, 'referer'))

  if (!host) {
    return
  }

  const matchesHost = (value?: string) => value === host

  if (originHost && !matchesHost(originHost)) {
    throw createError({
      statusCode: 403,
      statusMessage: 'Cross-site request blocked',
    })
  }

  if (!originHost && refererHost && !matchesHost(refererHost)) {
    throw createError({
      statusCode: 403,
      statusMessage: 'Cross-site request blocked',
    })
  }

  if (originHost === 'null') {
    throw createError({
      statusCode: 403,
      statusMessage: 'Cross-site request blocked',
    })
  }
}

export const assertCsrfToken = (event: H3Event) => {
  const csrfToken = getCookie(event, CSRF_COOKIE_NAME)
  const headerToken = getRequestHeader(event, CSRF_HEADER_NAME)

  if (!csrfToken || !headerToken || csrfToken !== headerToken) {
    throw createError({
      statusCode: 403,
      statusMessage: 'Invalid CSRF token.',
    })
  }
}

export const shouldValidateCsrf = (event: H3Event) => {
  return !isSafeMethod(event.method)
}
