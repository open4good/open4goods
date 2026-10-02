import type { CookieSerializeOptions } from 'cookie-es'
import type { H3Event } from 'h3'
import { FetchError } from 'ofetch'

import { isSafeRedirectTarget } from '~~/shared/utils/_safe-redirect'

import {
  GOOGLE_SSO_PENDING_COOKIE,
  GOOGLE_TOKEN_ENDPOINT,
  parsePendingState,
} from '../../../utils/google-sso'

interface GoogleTokenResponse {
  id_token?: string
}

interface AuthTokensResponse {
  accessToken: string
  refreshToken: string
}

/**
 * Completes the loopback-local Google SSO flow (GOU-167): checks the
 * anti-CSRF state bound in google.get.ts, exchanges the authorization code
 * with PKCE (no client_secret involved), then forwards the resulting Google
 * ID token to front-api's /auth/google for verification against the role
 * allowlist before issuing session cookies.
 */
export default defineEventHandler(async (event: H3Event) => {
  const config = useRuntimeConfig()

  if (
    !config.public.googleSsoEnabled ||
    !config.googleOAuthClientId ||
    !config.googleOAuthRedirectUri
  ) {
    throw createError({ statusCode: 404, statusMessage: 'Not Found' })
  }

  const pendingRaw = getCookie(event, GOOGLE_SSO_PENDING_COOKIE)
  deleteCookie(event, GOOGLE_SSO_PENDING_COOKIE, { path: '/' })
  const pending = parsePendingState(pendingRaw)

  if (!pending) {
    throw createError({
      statusCode: 401,
      statusMessage: 'Missing or expired Google SSO attempt',
    })
  }

  const query = getQuery(event)
  const state = typeof query.state === 'string' ? query.state : undefined
  const code = typeof query.code === 'string' ? query.code : undefined

  if (!state || state !== pending.state) {
    throw createError({ statusCode: 401, statusMessage: 'Invalid state' })
  }

  if (query.error || !code) {
    throw createError({
      statusCode: 401,
      statusMessage: 'Google authorization failed',
    })
  }

  // Bound to the same allowlist as the original redirect query parameter:
  // the pending cookie only ever carries a value already validated in
  // google.get.ts, this re-check guards against a tampered cookie.
  const redirectTarget = isSafeRedirectTarget(pending.redirect)
    ? pending.redirect
    : '/'

  try {
    const tokenResponse = await $fetch<GoogleTokenResponse>(
      GOOGLE_TOKEN_ENDPOINT,
      {
        method: 'POST',
        body: new URLSearchParams({
          client_id: config.googleOAuthClientId,
          redirect_uri: config.googleOAuthRedirectUri,
          code,
          code_verifier: pending.codeVerifier,
          grant_type: 'authorization_code',
        }).toString(),
        headers: { 'content-type': 'application/x-www-form-urlencoded' },
      }
    )

    const idToken = tokenResponse.id_token
    if (!idToken) {
      throw createError({
        statusCode: 401,
        statusMessage: 'Google did not return an ID token',
      })
    }

    const tokens = await $fetch<AuthTokensResponse>(
      `${config.apiUrl}/auth/google`,
      {
        method: 'POST',
        body: { idToken, nonce: pending.nonce },
      }
    )

    const secure = process.env.NODE_ENV === 'production'
    const sameSite: 'lax' | 'none' = secure ? 'none' : 'lax'
    const cookieOptions: CookieSerializeOptions = {
      httpOnly: true,
      sameSite,
      secure,
      path: '/',
    }
    setCookie(
      event,
      config.public.tokenCookieName,
      tokens.accessToken,
      cookieOptions
    )
    setCookie(
      event,
      config.public.refreshCookieName,
      tokens.refreshToken,
      cookieOptions
    )

    return sendRedirect(event, redirectTarget, 302)
  } catch (err) {
    if (err instanceof FetchError) {
      const statusCode = err.response?.status ?? 401
      console.error('Google SSO fetch error', {
        statusCode,
        payload: err.data,
      })
      throw createError({
        statusCode: statusCode === 404 ? 404 : 401,
        statusMessage: 'Google SSO authentication failed',
        cause: err,
      })
    }

    throw err
  }
})
