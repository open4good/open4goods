import { defineNuxtPlugin, useRuntimeConfig } from '#imports'
import { isDoNotTrackEnabled } from '~/utils/do-not-track'
import { isLocalReferrer } from '~/utils/local-referrer'

export default defineNuxtPlugin({
  name: 'plausible-privacy-guard',
  order: -20,
  setup() {
    const runtimeConfig = useRuntimeConfig()

    if (import.meta.server) {
      return
    }

    const plausibleConfig = runtimeConfig.public.plausible || {}

    plausibleConfig.ignoredHostnames = Array.from(
      new Set([
        ...(plausibleConfig.ignoredHostnames ?? []),
        'localhost',
        '127.0.0.1',
      ])
    )

    if (
      isDoNotTrackEnabled() ||
      window.location.pathname.startsWith('/contrib/') ||
      isLocalReferrer(document.referrer)
    ) {
      plausibleConfig.enabled = false
    }

    runtimeConfig.public.plausible = plausibleConfig
  },
})
