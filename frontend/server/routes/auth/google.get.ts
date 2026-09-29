import { createHash, randomBytes } from 'node:crypto'

const OIDC_COOKIE_OPTIONS = {
  httpOnly: true,
  sameSite: 'lax' as const,
  secure: process.env.NODE_ENV === 'production',
  path: '/auth/google',
  maxAge: 600,
}

const safeRedirect = (value: unknown) => {
  if (typeof value !== 'string' || value.length === 0) return '/'
  if (value.includes('\\')) return '/'
  if (!value.startsWith('/') || value.startsWith('//')) return '/'
  return value
}

/** Starts Google Authorization Code with PKCE from the server-side Nuxt BFF. */
export default defineEventHandler(event => {
  const config = useRuntimeConfig(event)
  const { clientId, redirectUri } = config.googleOidc
  if (!clientId || !redirectUri) {
    throw createError({ statusCode: 503, statusMessage: 'Google sign-in is not configured' })
  }

  const state = randomBytes(32).toString('base64url')
  const nonce = randomBytes(32).toString('base64url')
  const verifier = randomBytes(64).toString('base64url')
  const challenge = createHash('sha256').update(verifier).digest('base64url')
  const redirect = safeRedirect(getQuery(event).redirect)
  setCookie(event, 'oidc_state', state, OIDC_COOKIE_OPTIONS)
  setCookie(event, 'oidc_nonce', nonce, OIDC_COOKIE_OPTIONS)
  setCookie(event, 'oidc_verifier', verifier, OIDC_COOKIE_OPTIONS)
  setCookie(event, 'oidc_redirect', redirect, OIDC_COOKIE_OPTIONS)

  const authorizationUrl = new URL('https://accounts.google.com/o/oauth2/v2/auth')
  authorizationUrl.search = new URLSearchParams({
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope: 'openid email',
    state,
    nonce,
    code_challenge: challenge,
    code_challenge_method: 'S256',
  }).toString()
  return sendRedirect(event, authorizationUrl.toString(), 302)
})
