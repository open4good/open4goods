import { createVuetify } from 'vuetify'
import { buildVuetifyThemes } from '~/assets/style/tokens-to-vuetify'

export default defineNuxtPlugin((nuxtApp) => {
  const initialTheme = resolveThemeFromPreference(useCookie<ThemePreference>(THEME_PREFERENCE_STORAGE_KEY).value ?? 'light')

  const vuetify = createVuetify({
    theme: {
      defaultTheme: initialTheme,
      themes: buildVuetifyThemes()
    },
    defaults: {
      VTooltip: {
        openDelay: 400,
        transition: 'fade-transition'
      }
    }
  })

  nuxtApp.vueApp.use(vuetify)

  if (import.meta.client) {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    mediaQuery.addEventListener('change', () => {
      const storedPreference = useCookie<ThemePreference>(THEME_PREFERENCE_STORAGE_KEY).value ?? 'light'
      if (storedPreference === 'system') {
        vuetify.theme.global.name.value = getSystemThemePreference()
      }
    })
  }
})
