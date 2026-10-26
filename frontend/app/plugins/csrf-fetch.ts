import { CSRF_COOKIE_NAME } from '~~/shared/utils/csrf'
import { createCsrfFetch } from '~/utils/_csrf-fetch'

export default defineNuxtPlugin(nuxtApp => {
  const csrfCookie = useCookie(CSRF_COOKIE_NAME)

  const csrfFetch = createCsrfFetch({
    isServer: import.meta.server,
    readCookie: () => csrfCookie.value,
    writeCookie: token => {
      csrfCookie.value = token
    },
    // Bound to this request's nuxtApp: useRequestEvent() would need the
    // ambient Nuxt context, lost after the caller's first await (GOU-1226).
    getServerContext: () => nuxtApp.ssrContext?.event.context,
  })

  globalThis.$fetch = csrfFetch
  nuxtApp.$fetch = csrfFetch
})
