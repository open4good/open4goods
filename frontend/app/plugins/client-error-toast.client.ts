import { isNuxtError } from '#app'

/**
 * A page the server rendered successfully must never be swapped for error.vue by a
 * client-side failure (hydration, lazy chunk, late fetch): crawlers that execute
 * JavaScript would see the error page, with its noindex, over a healthy 200.
 * The error is cleared and surfaced as a toast instead. Genuine 404s and pages
 * the server itself failed to render (payload.error) keep the error page.
 */
export default defineNuxtPlugin(nuxtApp => {
  const toastMessage = useState<string | null>('client-error-toast', () => null)

  nuxtApp.hook('app:error', error => {
    if (!nuxtApp.payload.serverRendered || nuxtApp.payload.error) {
      return
    }

    if (isNuxtError(error) && error.statusCode === 404) {
      return
    }

    console.error('Client error replaced by a toast', error)
    clearError()
    toastMessage.value = nuxtApp.$i18n.t('common.errors.clientToast')
  })
})
