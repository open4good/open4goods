import { $fetch } from 'ofetch'
import type { $Fetch } from 'ofetch'

import {
  CSRF_COOKIE_NAME,
  CSRF_HEADER_NAME,
  isSafeMethod,
} from '~~/shared/utils/csrf'
import { HTML_CACHE_DEGRADED_CONTEXT_KEY } from '~~/shared/utils/html-cache'

const API_PREFIX = '/api'
const CSRF_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24

/** Reads one cookie from a `document.cookie` string. */
export const readCookieValue = (cookies: string, name: string) => {
  for (const part of cookies.split(';')) {
    const separator = part.indexOf('=')
    if (separator === -1 || part.slice(0, separator).trim() !== name) {
      continue
    }
    try {
      return decodeURIComponent(part.slice(separator + 1).trim()) || undefined
    } catch {
      return undefined
    }
  }
  return undefined
}

/**
 * `document.cookie` assignment for a browser-created CSRF token. Written
 * synchronously, so the unsafe request that needed it already carries it: the
 * server no longer sets this cookie on page responses (GOU-2700).
 */
export const buildCsrfCookie = (token: string, secure: boolean) =>
  [
    `${CSRF_COOKIE_NAME}=${encodeURIComponent(token)}`,
    'Path=/',
    `Max-Age=${CSRF_COOKIE_MAX_AGE_SECONDS}`,
    'SameSite=Lax',
    ...(secure ? ['Secure'] : []),
  ].join('; ')

export interface CsrfFetchDependencies {
  isServer: boolean
  readCookie: () => string | null | undefined
  writeCookie: (token: string) => void
  // Request context of the SSR render that owns this client. It must come
  // from the plugin's nuxtApp, never from the ambient Nuxt context: that
  // context is gone once an async caller has awaited (GOU-1226).
  getServerContext: () => Record<string, unknown> | undefined
}

const createClientCsrfToken = () => {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID()
  }

  const bytes = new Uint8Array(16)
  globalThis.crypto?.getRandomValues(bytes)
  return Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
}

const isApiRequest = (request: RequestInfo | URL) => {
  if (typeof request === 'string') {
    return request.startsWith(API_PREFIX)
  }

  if (request instanceof URL) {
    return request.pathname.startsWith(API_PREFIX)
  }

  if (request instanceof Request) {
    try {
      return new URL(request.url).pathname.startsWith(API_PREFIX)
    } catch {
      return false
    }
  }

  return false
}

export const createCsrfFetch = ({
  isServer,
  readCookie,
  writeCookie,
  getServerContext,
}: CsrfFetchDependencies): $Fetch => {
  const markRenderDegraded = () => {
    const context = isServer ? getServerContext() : undefined
    if (context) {
      context[HTML_CACHE_DEGRADED_CONTEXT_KEY] = true
    }
  }

  return $fetch.create({
    onRequest({ request, options }) {
      const method = (options.method ?? 'GET').toString().toUpperCase()

      if (isSafeMethod(method) || !isApiRequest(request)) {
        return
      }

      // During SSR, a visitor without a CSRF cookie has no token to read: the
      // server never issues the cookie on page responses, so that anonymous HTML
      // stays cacheable (GOU-2700). We therefore fall back to
      // event.context.csrfToken, which the server middleware fills with the
      // incoming cookie or a per-render token that never leaves the server.
      let token = readCookie() ?? null
      if (isServer && !token) {
        const contextToken = getServerContext()?.csrfToken
        if (typeof contextToken === 'string' && contextToken) {
          token = contextToken
        }
      }

      if (!token) {
        if (isServer) {
          return
        }
        // First unsafe call of this browser: the token is created on demand.
        token = createClientCsrfToken()
        writeCookie(token)
      }

      const headers = new Headers(options.headers || {})

      if (!headers.has(CSRF_HEADER_NAME)) {
        headers.set(CSRF_HEADER_NAME, token)
      }

      // On the server side we must also pass the cookie so the middleware can
      // verify that the header and the cookie match.
      if (isServer) {
        headers.append('Cookie', `${CSRF_COOKIE_NAME}=${token}`)
      }

      // Convert back to a plain object to ensure ofetch compatibility
      options.headers = Object.fromEntries(headers.entries())
    },
    onRequestError() {
      markRenderDegraded()
    },
    onResponseError({ response }) {
      if (response.status >= 500) {
        markRenderDegraded()
      }
    },
  })
}
