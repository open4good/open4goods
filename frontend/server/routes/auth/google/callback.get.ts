import { timingSafeEqual } from 'node:crypto'

const OIDC_COOKIE_OPTIONS = {
  httpOnly: true,
  sameSite: 'lax' as const,
  secure: process.env.NODE_ENV === 'production',
  path: '/auth/google',
}

const matchingState = (actual: string | undefined, expected: string | undefined) => {
  if (!actual || !expected) return false
  const actualBuffer = Buffer.from(actual, 'utf8')
  const expectedBuffer = Buffer.from(expected, 'utf8')
  return actualBuffer.length === expectedBuffer.length && timingSafeEqual(actualBuffer, expectedBuffer)
}

const tokenNonce = (idToken: string) => {
  const payload = idToken.split('.')[1]
  if (!payload) return undefined
  try {
    return JSON.parse(Buffer.from(payload, 'base64url').toString('utf8')).nonce
  } catch {
    return undefined
  }
}

/** Completes the BFF-only OAuth exchange and establishes application cookies. */
export default defineEventHandler(async event => {
  const config = useRuntimeConfig(event)
  const query = getQuery(event)
  const expectedState = getCookie(event, 'oidc_state')
  const expectedNonce = getCookie(event, 'oidc_nonce')
  const verifier = getCookie(event, 'oidc_verifier')
  const redirect = getCookie(event, 'oidc_redirect') || '/'

  try {
    if (typeof query.error === 'string' || typeof query.code !== 'string' || !matchingState(query.state as string | undefined, expectedState) || !verifier) {
      throw createError({ statusCode: 401, statusMessage: 'Google sign-in was refused' })
    }

    const providerTokens = await $fetch<{ id_token?: string }>('https://oauth2.googleapis.com/token', {
      method: 'POST',
      body: new URLSearchParams({
        code: query.code,
        client_id: config.googleOidc.clientId,
        client_secret: config.googleOidc.clientSecret,
        redirect_uri: config.googleOidc.redirectUri,
        grant_type: 'authorization_code',
        code_verifier: verifier,
      }),
    })
    if (!providerTokens.id_token) {
      throw createError({ statusCode: 401, statusMessage: 'Google did not return an identity token' })
    }
    if (!matchingState(tokenNonce(providerTokens.id_token), expectedNonce)) {
      throw createError({ statusCode: 401, statusMessage: 'Google identity nonce did not match' })
    }
    const tokens = await $fetch<{ accessToken: string; refreshToken: string }>(`${config.apiUrl}/auth/google`, {
      method: 'POST',
      headers: { 'X-Shared-Token': config.machineToken },
      body: { idToken: providerTokens.id_token },
    })
    const sessionOptions = { ...OIDC_COOKIE_OPTIONS, path: '/', maxAge: undefined }
    setCookie(event, config.public.tokenCookieName, tokens.accessToken, sessionOptions)
    setCookie(event, config.public.refreshCookieName, tokens.refreshToken, sessionOptions)
  } finally {
    deleteCookie(event, 'oidc_state', OIDC_COOKIE_OPTIONS)
    deleteCookie(event, 'oidc_nonce', OIDC_COOKIE_OPTIONS)
    deleteCookie(event, 'oidc_verifier', OIDC_COOKIE_OPTIONS)
    deleteCookie(event, 'oidc_redirect', OIDC_COOKIE_OPTIONS)
  }
  return sendRedirect(event, redirect, 302)
})
