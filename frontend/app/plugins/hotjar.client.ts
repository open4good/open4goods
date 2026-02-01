import type { Plugin } from 'vue'
import Hotjar from 'vue-hotjar'

import { defineNuxtPlugin, useCookie, useRuntimeConfig } from '#imports'
import {
  HOTJAR_RECORDING_COOKIE_NAME,
  isHotjarRecordingCookieEnabled,
} from '~~/shared/utils/hotjar-recording'
import { isDoNotTrackEnabled } from '~/utils/do-not-track'

type HotjarPublicConfig = {
  enabled: boolean
  mode?: 'always' | 'never' | 'query'
  siteId: number
  snippetVersion: number
}

const isValidSiteId = (siteId: number): boolean =>
  Number.isFinite(siteId) && siteId > 0

export default defineNuxtPlugin({
  name: 'hotjar-recording',
  setup(nuxtApp) {
    if (isDoNotTrackEnabled()) {
      return
    }

    const runtimeConfig = useRuntimeConfig()
    const hotjarConfig = runtimeConfig.public.hotjar as
      | HotjarPublicConfig
      | undefined

    if (!hotjarConfig) {
      return
    }

    if (hotjarConfig.mode === 'never') {
      return
    }

    if (!isValidSiteId(hotjarConfig.siteId)) {
      return
    }

    if (hotjarConfig.mode !== 'always') {
      const hotjarCookie = useCookie(HOTJAR_RECORDING_COOKIE_NAME)

      if (!isHotjarRecordingCookieEnabled(hotjarCookie.value)) {
        return
      }
    }

    nuxtApp.vueApp.use(Hotjar as Plugin, {
      id: hotjarConfig.siteId,
      isProduction: hotjarConfig.mode !== 'never',
      snippetVersion: hotjarConfig.snippetVersion,
    })
  },
})
