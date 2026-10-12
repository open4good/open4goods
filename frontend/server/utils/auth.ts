import { createHmac, timingSafeEqual } from 'node:crypto'

import type { H3Event } from 'h3'
import { createError, getCookie } from 'h3'
import { useRuntimeConfig } from '#imports'

import { hasAdminAccess } from '~~/shared/utils/_roles'

/**
 * Server-side verification of the HS256 access tokens issued by front-api
 * (front.security.jwt-secret). The frontend only ever decoded this token
 * client-side without checking its signature; routes gating admin-only data
 * must verify it themselves instead of trusting an unverified payload.
 */
interface AccessTokenPayload {
  roles?: string[]
  exp?: number
}

const base64UrlDecode = (value: string): Buffer => Buffer.from(value, 'base64url')

export const verifyAccessToken = (
  token: string,
  secret: string
): AccessTokenPayload | null => {
  if (!token || !secret) return null

  const parts = token.split('.')
  if (parts.length !== 3) return null
  const [headerB64, payloadB64, signatureB64] = parts
  if (!headerB64 || !payloadB64 || !signatureB64) return null

  try {
    const header = JSON.parse(base64UrlDecode(headerB64).toString('utf-8')) as {
      alg?: string
    }
    if (header.alg !== 'HS256') return null

    const expectedSignature = createHmac('sha256', secret)
      .update(`${headerB64}.${payloadB64}`)
      .digest()
    const actualSignature = base64UrlDecode(signatureB64)

    if (
      expectedSignature.length !== actualSignature.length ||
      !timingSafeEqual(expectedSignature, actualSignature)
    ) {
      return null
    }

    const payload = JSON.parse(
      base64UrlDecode(payloadB64).toString('utf-8')
    ) as AccessTokenPayload

    if (typeof payload.exp === 'number' && payload.exp * 1000 <= Date.now()) {
      return null
    }

    return payload
  } catch {
    return null
  }
}

interface RequireAdminAccessOptions {
  cookieName?: string
  secret?: string
}

/** Throws 401/403 unless the request carries a signature-verified admin access token. */
export const requireAdminAccess = (
  event: H3Event,
  options?: RequireAdminAccessOptions
): void => {
  const config = useRuntimeConfig()
  const cookieName =
    options?.cookieName ?? config.public.tokenCookieName ?? 'access_token'
  const secret = options?.secret ?? config.jwtSecret

  const token = getCookie(event, cookieName)
  if (!token) {
    throw createError({
      statusCode: 401,
      statusMessage: 'Authentication required',
    })
  }

  if (!secret) {
    throw createError({
      statusCode: 500,
      statusMessage: 'Server misconfiguration',
    })
  }

  const payload = verifyAccessToken(token, secret)
  if (!payload) {
    throw createError({
      statusCode: 401,
      statusMessage: 'Invalid or expired session',
    })
  }

  if (!hasAdminAccess(payload.roles)) {
    throw createError({
      statusCode: 403,
      statusMessage: 'Forbidden',
    })
  }
}
