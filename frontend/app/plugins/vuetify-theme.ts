import { usePreferredDark } from '@vueuse/core'

import type { Ref } from 'vue'

import { THEME_PREFERENCE_KEY, resolveThemeName, type ThemeName } from '~~/shared/constants/theme'

type VuetifyThemeBridge = {
  theme?: {
    global?: {
      name?: Ref<ThemeName>
    }
  }
}

export default defineNuxtPlugin((nuxtApp) => {
  const storedPreference = useCookie<ThemeName | null>(THEME_PREFERENCE_KEY, {
    sameSite: 'lax',
    path: '/',
    watch: false,
  })

  const isClient = typeof window !== 'undefined'
  let themeName = resolveThemeName(storedPreference.value)

  if (!storedPreference.value && isClient) {
    const prefersDark = usePreferredDark()
    themeName = prefersDark.value ? 'dark' : 'light'
  }

  const vuetify = nuxtApp.$vuetify as VuetifyThemeBridge | undefined
  const globalThemeName = vuetify?.theme?.global?.name

  if (globalThemeName && globalThemeName.value !== themeName) {
    globalThemeName.value = themeName
  }

  if (!storedPreference.value && isClient) {
    storedPreference.value = themeName
  }
})
