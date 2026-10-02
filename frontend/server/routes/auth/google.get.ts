import type { H3Event } from 'h3'

import { isSafeRedirectTarget } from '~~/shared/utils/_safe-redirect'

import {
  GOOGLE_SSO_PENDING_COOKIE,
  GOOGLE_SSO_PENDING_COOKIE_MAX_AGE_SECONDS,
  buildGoogleAuthorizationUrl,
  createGoogleSsoAttempt,
  serializePendingState,
} from '../../utils/google-sso'

/**
 * Starts the loopback-local Google SSO flow (GOU-167): disabled unless the
 * flag and the OAuth client are both configured, so the existing password
 * login remains the only reachable path by default.
 */
export default defineEventHandler((event: H3Event) => {
  const config = useRuntimeConfig()

  if (
    !config.public.googleSsoEnabled ||
    !config.googleOAuthClientId ||
    !config.googleOAuthRedirectUri
  ) {
    throw createError({ statusCode: 404, statusMessage: 'Not Found' })
  }

  const query = getQuery(event)
  const redirectCandidate = Array.isArray(query.redirect)
    ? query.redirect[0]
    : query.redirect
  const redirect = isSafeRedirectTarget(redirectCandidate)
    ? redirectCandidate
    : '/'

  const { pending, codeChallenge } = createGoogleSsoAttempt(redirect)

  setCookie(
    event,
    GOOGLE_SSO_PENDING_COOKIE,
    serializePendingState(pending),
    {
      httpOnly: true,
      sameSite: 'lax',
      secure: process.env.NODE_ENV === 'production',
      path: '/',
      maxAge: GOOGLE_SSO_PENDING_COOKIE_MAX_AGE_SECONDS,
    }
  )

  const authorizationUrl = buildGoogleAuthorizationUrl({
    clientId: config.googleOAuthClientId,
    redirectUri: config.googleOAuthRedirectUri,
    state: pending.state,
    nonce: pending.nonce,
    codeChallenge,
  })

  return sendRedirect(event, authorizationUrl, 302)
})
