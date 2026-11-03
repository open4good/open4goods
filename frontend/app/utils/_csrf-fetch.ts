import { $fetch } from 'ofetch'
import type { $Fetch } from 'ofetch'

import {
  CSRF_COOKIE_NAME,
  CSRF_HEADER_NAME,
  isSafeMethod,
} from '~~/shared/utils/csrf'

const API_PREFIX = '/api'

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
}: CsrfFetchDependencies): $Fetch =>
  $fetch.create({
    onRequest({ request, options }) {
      const method = (options.method ?? 'GET').toString().toUpperCase()

      if (isSafeMethod(method) || !isApiRequest(request)) {
        return
      }

      // On the very first SSR request, the CSRF cookie hasn't been sent back to
      // the browser yet (it lives only in the Set-Cookie response header).
      // useCookie() reads the *incoming* request cookie, so it returns undefined.
      // We therefore fall back to event.context.csrfToken which the server
      // middleware sets immediately after generating (or reading) the token.
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
  })
