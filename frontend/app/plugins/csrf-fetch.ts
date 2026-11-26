import { CSRF_COOKIE_NAME } from '~~/shared/utils/csrf'
import {
  buildCsrfCookie,
  createCsrfFetch,
  readCookieValue,
} from '~/utils/_csrf-fetch'

export default defineNuxtPlugin(nuxtApp => {
  // Server: the incoming request cookie, read-only so the render never emits
  // Set-Cookie and anonymous HTML stays cacheable (GOU-2700).
  const requestCookie = import.meta.server
    ? useCookie(CSRF_COOKIE_NAME, { readonly: true })
    : undefined

  const csrfFetch = createCsrfFetch({
    isServer: import.meta.server,
    readCookie: () =>
      import.meta.server
        ? requestCookie?.value
        : readCookieValue(document.cookie, CSRF_COOKIE_NAME),
    writeCookie: token => {
      // Written synchronously (useCookie defers its write to a watcher), so the
      // request that created the token already carries the cookie.
      document.cookie = buildCsrfCookie(
        token,
        window.location.protocol === 'https:'
      )
    },
    // Bound to this request's nuxtApp: useRequestEvent() would need the
    // ambient Nuxt context, lost after the caller's first await (GOU-1226).
    getServerContext: () => nuxtApp.ssrContext?.event.context,
  })

  globalThis.$fetch = csrfFetch
  nuxtApp.$fetch = csrfFetch
})
