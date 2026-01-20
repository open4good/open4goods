import { $fetch } from 'ofetch'

import {
  CSRF_COOKIE_NAME,
  CSRF_HEADER_NAME,
  isSafeMethod,
} from '~~/shared/utils/csrf'

const API_PREFIX = '/api'

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

export default defineNuxtPlugin(nuxtApp => {
  const csrfCookie = useCookie(CSRF_COOKIE_NAME)

  const csrfFetch = $fetch.create({
    onRequest({ request, options }) {
      const method = (options.method ?? 'GET').toString().toUpperCase()

      if (isSafeMethod(method) || !isApiRequest(request)) {
        return
      }

      if (!csrfCookie.value) {
        console.log('[CSRF-Plugin] No CSRF cookie found in plugin')
        return
      }

      console.log(
        '[CSRF-Plugin] Found cookie, adding header:',
        csrfCookie.value
      )

      const headers = new Headers(options.headers || {})
      if (!headers.has(CSRF_HEADER_NAME)) {
        headers.set(CSRF_HEADER_NAME, csrfCookie.value)
      }

      // On server side, we need to manually pass the cookie so the middleware can verify it
      if (import.meta.server) {
        headers.append('Cookie', `${CSRF_COOKIE_NAME}=${csrfCookie.value}`)
      }

      options.headers = headers
    },
  })

  globalThis.$fetch = csrfFetch
  nuxtApp.$fetch = csrfFetch
})
