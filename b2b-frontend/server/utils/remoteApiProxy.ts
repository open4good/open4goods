import type { H3Event } from 'h3'
import {
  appendResponseHeader,
  createError,
  getMethod,
  getRequestHeaders,
  getRequestURL,
  readRawBody,
  setResponseHeader,
  setResponseStatus
} from 'h3'

interface RemoteApiProxyOptions {
  target: string
  mountPath: string
  allowMutatingMethods?: boolean
  allowedMutationPathPrefixes?: string[]
}

const hopByHopHeaders = new Set([
  'connection',
  'content-length',
  'keep-alive',
  'proxy-authenticate',
  'proxy-authorization',
  'te',
  'trailer',
  'transfer-encoding',
  'upgrade'
])

const proxyControlledHeaders = new Set([
  'accept-encoding',
  'forwarded',
  'host',
  'origin',
  'referer',
  'x-forwarded-for',
  'x-forwarded-host',
  'x-forwarded-port',
  'x-forwarded-proto'
])

/**
 * Proxies a local Nuxt API namespace to an explicitly configured remote API.
 * Intended for local development against hosted environments without exposing
 * production API origins directly to browser CORS and cookie handling.
 */
export async function proxyRemoteApi(event: H3Event, options: RemoteApiProxyOptions) {
  const target = options.target.trim()
  if (!target) {
    throw createError({ statusCode: 502, statusMessage: 'remote_api_proxy_target_missing' })
  }

  const targetBaseUrl = new URL(target)
  if (!['http:', 'https:'].includes(targetBaseUrl.protocol)) {
    throw createError({ statusCode: 502, statusMessage: 'remote_api_proxy_target_invalid' })
  }

  const requestUrl = getRequestURL(event)
  const suffix = requestUrl.pathname.slice(options.mountPath.length)
  const upstreamUrl = new URL(targetBaseUrl)
  upstreamUrl.pathname = `${trimTrailingSlash(targetBaseUrl.pathname)}/${suffix.replace(/^\/+/, '')}`
  upstreamUrl.search = requestUrl.search

  const headers = buildUpstreamHeaders(event)
  const method = getMethod(event)
  if (!isAllowedMethod(method, suffix, options)) {
    throw createError({ statusCode: 405, statusMessage: 'remote_api_proxy_mutation_blocked' })
  }

  const rawBody = method === 'GET' || method === 'HEAD' ? undefined : await readRawBody(event, false)
  const body = rawBody ? rawBody.buffer.slice(rawBody.byteOffset, rawBody.byteOffset + rawBody.byteLength) as ArrayBuffer : undefined
  const response = await fetch(upstreamUrl, {
    method,
    headers,
    body,
    redirect: 'manual'
  })

  setResponseStatus(event, response.status, response.statusText)
  for (const [name, value] of response.headers.entries()) {
    const normalized = name.toLowerCase()
    if (hopByHopHeaders.has(normalized) || normalized === 'content-encoding' || normalized === 'set-cookie') {
      continue
    }
    setResponseHeader(event, name, value)
  }

  for (const cookie of readSetCookies(response.headers)) {
    appendResponseHeader(event, 'set-cookie', rewriteSetCookieForLocalOrigin(cookie, requestUrl))
  }

  return response.body
}

function buildUpstreamHeaders(event: H3Event) {
  const headers = new Headers()
  const incomingHeaders = getRequestHeaders(event)

  for (const [name, value] of Object.entries(incomingHeaders)) {
    const normalized = name.toLowerCase()
    if (!value || hopByHopHeaders.has(normalized) || proxyControlledHeaders.has(normalized)) {
      continue
    }
    headers.set(name, value)
  }

  return headers
}

function isAllowedMethod(method: string, suffix: string, options: RemoteApiProxyOptions) {
  if (['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    return true
  }

  if (options.allowMutatingMethods) {
    return true
  }

  const normalizedSuffix = `/${suffix.replace(/^\/+/, '')}`
  return Boolean(options.allowedMutationPathPrefixes?.some(prefix => normalizedSuffix.startsWith(prefix)))
}

function readSetCookies(headers: Headers) {
  const withGetSetCookie = headers as Headers & { getSetCookie?: () => string[] }
  const cookies = withGetSetCookie.getSetCookie?.()
  if (cookies) {
    return cookies
  }

  const cookie = headers.get('set-cookie')
  return cookie ? [cookie] : []
}

export function rewriteSetCookieForLocalOrigin(cookie: string, requestUrl: URL) {
  const isLocalHttp = requestUrl.protocol === 'http:' && isLocalHost(requestUrl.hostname)
  const parts = cookie.split(';').map(part => part.trim()).filter(Boolean)
  const rewritten = parts
    .filter(part => !(isLocalHttp && part.toLowerCase().startsWith('domain=')))
    .filter(part => !(isLocalHttp && part.toLowerCase() === 'secure'))
    .map((part) => {
      if (isLocalHttp && part.toLowerCase() === 'samesite=none') {
        return 'SameSite=Lax'
      }
      return part
    })

  return rewritten.join('; ')
}

function isLocalHost(hostname: string) {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1'
}

function trimTrailingSlash(value: string) {
  return value.replace(/\/+$/, '')
}
