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

      // On the very first SSR request, the CSRF cookie hasn't been sent back to
      // the browser yet (it lives only in the Set-Cookie response header).
      // useCookie() reads the *incoming* request cookie, so it returns undefined.
      // We therefore fall back to event.context.csrfToken which the server
      // middleware sets immediately after generating (or reading) the token.
      let token = csrfCookie.value
      if (import.meta.server && !token) {
        const event = useRequestEvent()
        if (event?.context.csrfToken) {
          console.log(
            '[CSRF-Plugin] Using context token for SSR:',
            event.context.csrfToken
          )
          token = event.context.csrfToken as string
        }
      }

      if (!token) {
        console.log(
          '[CSRF-Plugin] No CSRF token found – request will proceed without it'
        )
        return
      }

      console.log('[CSRF-Plugin] Attaching CSRF token:', token)

      const headers = new Headers(options.headers || {})

      if (!headers.has(CSRF_HEADER_NAME)) {
        headers.set(CSRF_HEADER_NAME, token)
      }

      // On the server side we must also pass the cookie so the middleware can
      // verify that the header and the cookie match.
      if (import.meta.server) {
        headers.append('Cookie', `${CSRF_COOKIE_NAME}=${token}`)
      }

      // Convert back to a plain object to ensure ofetch compatibility
      options.headers = Object.fromEntries(headers.entries())
    },
  })

  globalThis.$fetch = csrfFetch
  nuxtApp.$fetch = csrfFetch
})
