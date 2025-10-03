import { computed, watchEffect } from 'vue'
import { useRequestEvent, useState } from '#imports'
import { useDisplay } from 'vuetify'

const DESKTOP_APP_BAR_HEIGHT = 64
const MOBILE_APP_BAR_HEIGHT = 56
const MOBILE_UA_REGEX = /Mobile|Android|iP(ad|hone|od)|IEMobile|BlackBerry|Opera Mini/i

export function useAppBarHeight() {
  const requestEvent = useRequestEvent()
  const userAgent = requestEvent?.node.req.headers['user-agent'] ?? ''
  const initialHeight = MOBILE_UA_REGEX.test(userAgent)
    ? MOBILE_APP_BAR_HEIGHT
    : DESKTOP_APP_BAR_HEIGHT

  const appBarHeight = useState('app-bar-height', () => initialHeight)

  if (import.meta.client) {
    const { mdAndUp } = useDisplay()

    watchEffect(() => {
      const targetHeight = mdAndUp.value ? DESKTOP_APP_BAR_HEIGHT : MOBILE_APP_BAR_HEIGHT

      if (appBarHeight.value !== targetHeight) {
        appBarHeight.value = targetHeight
      }
    })
  }

  const appBarCssVariables = computed(() => ({
    '--app-bar-height': `${appBarHeight.value}px`,
  }))

  return {
    appBarHeight,
    appBarCssVariables,
  }
}
