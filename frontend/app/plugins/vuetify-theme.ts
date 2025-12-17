import type { NuxtApp } from 'nuxt/app'

import { usePreferredDark } from '@vueuse/core'

import type { Ref } from 'vue'

import {
  THEME_PREFERENCE_KEY,
  resolveThemeName,
  type ThemeName,
} from '~~/shared/constants/theme'

type VuetifyThemeBridge = {
  theme?: {
    global?: {
      name?: Ref<ThemeName>
    }
  }
}

const resolveVuetifyInstance = (
  nuxtApp: NuxtApp
): VuetifyThemeBridge | undefined => {
  const direct = nuxtApp.$vuetify as unknown as VuetifyThemeBridge | undefined

  if (direct?.theme?.global?.name) {
    return direct
  }

  const fromGlobals = nuxtApp.vueApp.config.globalProperties
    .$vuetify as unknown as VuetifyThemeBridge | undefined

  if (fromGlobals?.theme?.global?.name) {
    return fromGlobals
  }

  return undefined
}

const applyVuetifyTheme = (nuxtApp: NuxtApp, value: ThemeName) => {
  const vuetify = resolveVuetifyInstance(nuxtApp)
  const globalThemeName = vuetify?.theme?.global?.name

  if (globalThemeName && globalThemeName.value !== value) {
    globalThemeName.value = value
  }
}

const themePlugin = defineNuxtPlugin({
  name: 'open4goods:vuetify-theme',
  enforce: 'pre',
  setup(nuxt) {
    const nuxtApp = nuxt as NuxtApp
    const storedPreference = useCookie<string | null>(THEME_PREFERENCE_KEY, {
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

    const syncTheme = () => applyVuetifyTheme(nuxtApp, themeName)

    // Ensure the preferred theme is already set before Vue renders on the server.
    syncTheme()

    nuxtApp.hooks.hook('app:created', syncTheme)
    nuxtApp.hooks.hook('app:mounted', syncTheme)

    if (!storedPreference.value && isClient) {
      storedPreference.value = themeName
    }

    if (import.meta.server) {
      nuxtApp.payload.state.preferredTheme = themeName
    }
  },
})

export default themePlugin
