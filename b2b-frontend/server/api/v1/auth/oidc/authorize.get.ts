import { createError, getQuery, getRequestURL, sendRedirect, setCookie } from 'h3'
import { resolveOidcProviderConfig } from '~/server/utils/oidcProviders'
import { normalizeLocalRedirectPath, normalizeOidcCallbackPath } from '~/server/utils/localRedirects'

export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const provider = String(query.provider || '').trim().toLowerCase()
  const next = normalizeLocalRedirectPath(query.next, '/')
  const callbackPath = normalizeOidcCallbackPath(query.callback, provider)

  const config = useRuntimeConfig(event)
  const providerConfig = resolveOidcProviderConfig(provider, config)
  if (!providerConfig) throw createError({ statusCode: 400, statusMessage: 'unsupported_oidc_provider' })
  if (!providerConfig.clientId || !providerConfig.clientSecret) throw createError({ statusCode: 500, statusMessage: `oidc_${provider}_credentials_missing` })

  const requestUrl = getRequestURL(event)
  const origin = requestUrl.origin
  const redirectUri = providerConfig.redirectUri || new URL(callbackPath, origin).toString()
  const nonce = crypto.randomUUID()

  setCookie(event, 'PDAPI_OIDC_STATE', Buffer.from(JSON.stringify({ next, nonce, provider, redirectUri })).toString('base64url'), {
    httpOnly: true,
    secure: requestUrl.protocol === 'https:',
    sameSite: 'lax',
    path: '/',
    maxAge: 600
  })

  const authorizeUrl = new URL(providerConfig.authorizeEndpoint)
  authorizeUrl.searchParams.set('client_id', providerConfig.clientId)
  authorizeUrl.searchParams.set('redirect_uri', redirectUri)
  authorizeUrl.searchParams.set('response_type', 'code')
  authorizeUrl.searchParams.set('scope', providerConfig.scopes)
  authorizeUrl.searchParams.set('state', nonce)
  authorizeUrl.searchParams.set('nonce', nonce)

  return sendRedirect(event, authorizeUrl.toString(), 302)
})
