import { createHash, randomBytes, randomUUID } from 'node:crypto'

/**
 * Cookie used to bind a Google authorization request to its callback: carries
 * the anti-CSRF state, the OIDC nonce, the PKCE code verifier and the original
 * redirect target for the short window between the two server routes.
 */
export const GOOGLE_SSO_PENDING_COOKIE = 'google_sso_pending'
export const GOOGLE_SSO_PENDING_COOKIE_MAX_AGE_SECONDS = 5 * 60

export const GOOGLE_AUTHORIZATION_ENDPOINT =
  'https://accounts.google.com/o/oauth2/v2/auth'
export const GOOGLE_TOKEN_ENDPOINT = 'https://oauth2.googleapis.com/token'

export interface GoogleSsoPendingState {
  state: string
  nonce: string
  codeVerifier: string
  redirect: string
}

const base64UrlEncode = (input: Buffer) =>
  input
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')

export const createPkcePair = () => {
  const codeVerifier = base64UrlEncode(randomBytes(32))
  const codeChallenge = base64UrlEncode(
    createHash('sha256').update(codeVerifier).digest()
  )
  return { codeVerifier, codeChallenge }
}

/**
 * Starts a new Google SSO attempt: the Authorization Code + PKCE exchange
 * means no client_secret is ever required or stored.
 */
export const createGoogleSsoAttempt = (redirect: string) => {
  const { codeVerifier, codeChallenge } = createPkcePair()

  const pending: GoogleSsoPendingState = {
    state: randomUUID(),
    nonce: randomUUID(),
    codeVerifier,
    redirect,
  }

  return { pending, codeChallenge }
}

export const serializePendingState = (pending: GoogleSsoPendingState) =>
  JSON.stringify(pending)

export const parsePendingState = (
  raw: string | undefined | null
): GoogleSsoPendingState | null => {
  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as Partial<GoogleSsoPendingState>
    if (
      typeof parsed.state === 'string' &&
      typeof parsed.nonce === 'string' &&
      typeof parsed.codeVerifier === 'string' &&
      typeof parsed.redirect === 'string'
    ) {
      return parsed as GoogleSsoPendingState
    }
    return null
  } catch {
    return null
  }
}

export const buildGoogleAuthorizationUrl = (params: {
  clientId: string
  redirectUri: string
  state: string
  nonce: string
  codeChallenge: string
}) => {
  const url = new URL(GOOGLE_AUTHORIZATION_ENDPOINT)
  url.searchParams.set('client_id', params.clientId)
  url.searchParams.set('redirect_uri', params.redirectUri)
  url.searchParams.set('response_type', 'code')
  url.searchParams.set('scope', 'openid email')
  url.searchParams.set('state', params.state)
  url.searchParams.set('nonce', params.nonce)
  url.searchParams.set('code_challenge', params.codeChallenge)
  url.searchParams.set('code_challenge_method', 'S256')
  url.searchParams.set('access_type', 'online')
  url.searchParams.set('prompt', 'select_account')
  return url.toString()
}
