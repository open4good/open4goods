import { createError, getCookie, getQuery, getRequestURL, sendRedirect, deleteCookie, appendResponseHeader } from 'h3'
import { rewriteSetCookieForLocalOrigin } from '~/server/utils/remoteApiProxy'
import { resolveOidcProviderConfig } from '~/server/utils/oidcProviders'
import { normalizeLocalRedirectPath } from '~/server/utils/localRedirects'

interface OidcState { next: string, nonce: string, provider: string, redirectUri: string }

export default defineEventHandler(async (event) => {
  const provider = event.context.params?.provider?.toLowerCase() || ''
  const query = getQuery(event)
  if (query.error) return sendRedirect(event, '/auth/login?error=oidc_provider_error', 302)

  const code = String(query.code || '')
  const state = String(query.state || '')
  const stateCookie = getCookie(event, 'INFERA_OIDC_STATE')
  if (!code || !state || !stateCookie) {
    throw createError({
      statusCode: 400,
      statusMessage: 'oidc_callback_invalid_request',
      data: {
        missingCode: !code,
        missingState: !state,
        missingStateCookie: !stateCookie
      }
    })
  }

  let parsed: OidcState
  try {
    parsed = JSON.parse(Buffer.from(stateCookie, 'base64url').toString('utf8')) as OidcState
  } catch {
    throw createError({ statusCode: 400, statusMessage: 'oidc_state_cookie_invalid' })
  }
  deleteCookie(event, 'INFERA_OIDC_STATE', { path: '/' })
  if (parsed.provider !== provider || parsed.nonce !== state) throw createError({ statusCode: 400, statusMessage: 'oidc_state_mismatch' })

  const config = useRuntimeConfig(event)
  const providerConfig = resolveOidcProviderConfig(provider, config)
  if (!providerConfig) throw createError({ statusCode: 400, statusMessage: 'unsupported_oidc_provider' })

  const redirectUri = parsed.redirectUri || new URL(`/auth/callback/${provider}`, getRequestURL(event).origin).toString()
  const tokenResponse = await $fetch<{ id_token?: string, access_token?: string }>(providerConfig.tokenEndpoint, {
    method: 'POST',
    body: new URLSearchParams({
      grant_type: 'authorization_code', code, client_id: providerConfig.clientId, client_secret: providerConfig.clientSecret, redirect_uri: redirectUri
    }).toString(),
    headers: { 'content-type': 'application/x-www-form-urlencoded', accept: 'application/json' }
  })

  const token = tokenResponse.id_token || (provider === 'github' ? tokenResponse.access_token : null)
  if (!token) throw createError({ statusCode: 401, statusMessage: 'oidc_token_missing' })

  const requestUrl = getRequestURL(event)
  const backendResponse = await $fetch.raw<{ newAccount?: boolean }>('/api/v1/auth/oidc', {
    baseURL: resolveBackendBaseUrl(config),
    method: 'POST',
    body: { provider, idToken: token }
  })
  for (const cookie of backendResponse.headers.getSetCookie()) {
    appendResponseHeader(event, 'set-cookie', rewriteSetCookieForLocalOrigin(cookie, requestUrl))
  }

  if (backendResponse._data?.newAccount) {
    return sendRedirect(event, '/dashboard', 302)
  }

  return sendRedirect(event, normalizeLocalRedirectPath(parsed.next, '/'), 302)
})

function resolveBackendBaseUrl(config: ReturnType<typeof useRuntimeConfig>) {
  if (config.backendProxyTarget) {
    return config.backendProxyTarget
  }
  const configured = config.public.backendBaseUrl
  return configured
}
