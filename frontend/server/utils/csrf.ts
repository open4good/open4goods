import { randomUUID } from 'node:crypto'

import type { H3Event } from 'h3'
import { createError, getCookie, getRequestHeader, setCookie } from 'h3'

import {
  CSRF_COOKIE_NAME,
  CSRF_HEADER_NAME,
  isSafeMethod,
} from '~~/shared/utils/csrf'

const CSRF_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24

const isProduction = () => process.env.NODE_ENV === 'production'

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

export const ensureCsrfCookie = (event: H3Event) => {
  const existingToken = getCookie(event, CSRF_COOKIE_NAME)

  if (existingToken) {
    console.log('[CSRF] Existing token found:', existingToken)
    return existingToken
  }

  const token = randomUUID()
  console.log('[CSRF] Generating new token:', token)
  setCookie(event, CSRF_COOKIE_NAME, token, {
    httpOnly: false,
    sameSite: 'lax',
    secure: isProduction(),
    path: '/',
    maxAge: CSRF_COOKIE_MAX_AGE_SECONDS,
  })

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

  console.log(
    '[CSRF] Asserting token. Cookie:',
    csrfToken,
    'Header:',
    headerToken
  )

  if (!csrfToken || !headerToken || csrfToken !== headerToken) {
    throw createError({
      statusCode: 403,
      statusMessage: `Invalid CSRF token. Cookie: '${csrfToken}', Header: '${headerToken}'`,
    })
  }
}

export const shouldValidateCsrf = (event: H3Event) => {
  return !isSafeMethod(event.method)
}
