import { defineNuxtPlugin, useRuntimeConfig } from '#imports'
import { isDoNotTrackEnabled } from '~/utils/do-not-track'

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

    if (isDoNotTrackEnabled()) {
      plausibleConfig.enabled = false
    }

    runtimeConfig.public.plausible = plausibleConfig
  },
})
