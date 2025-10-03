import { usePreferredDark } from '@vueuse/core'

import { THEME_PREFERENCE_KEY, resolveThemeName, type ThemeName } from '~~/shared/constants/theme'

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

  if (nuxtApp.$vuetify?.theme?.global?.name) {
    if (nuxtApp.$vuetify.theme.global.name.value !== themeName) {
      nuxtApp.$vuetify.theme.global.name.value = themeName
    }
  }

  if (!storedPreference.value && isClient) {
    storedPreference.value = themeName
  }
})
