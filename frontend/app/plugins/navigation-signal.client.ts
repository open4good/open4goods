import { defineNuxtPlugin } from '#imports'
import { buildNavigationSignalCookie } from '~~/shared/utils/navigation-signal'

/**
 * Writes the first-party navigation-signal cookie checked by /contrib/<token>
 * before redirecting to an affiliate network (GOU-1184). Set on the client
 * only, so SSR HTML stays cookie-free and cacheable; refreshed on every page
 * so its short lifetime never expires while the visitor browses the site.
 */
export default defineNuxtPlugin({
  name: 'navigation-signal',
  setup(nuxtApp) {
    const writeCookie = () => {
      document.cookie = buildNavigationSignalCookie(
        window.location.protocol === 'https:'
      )
    }

    writeCookie()
    nuxtApp.hook('page:finish', writeCookie)
  },
})
