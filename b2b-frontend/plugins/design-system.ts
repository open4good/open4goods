import { buildCssVariables, type PdapiThemeName } from '~/assets/style/tokens-to-vuetify'

export default defineNuxtPlugin(() => {
  const { activeTheme } = useThemePreference()

  // SSR: inject tokens as a style block to avoid FOUC
  const cssVariables = computed(() => {
    const vars = buildCssVariables(activeTheme.value as PdapiThemeName)
    const styleString = Object.entries(vars)
      .map(([key, value]) => `${key}: ${value};`)
      .join(' ')

    return `:root { ${styleString} color-scheme: ${activeTheme.value}; }`
  })

  useHead({
    style: [
      {
        id: 'pdapi-design-tokens',
        textContent: cssVariables
      }
    ]
  })

  // Client: sync color-scheme attribute on <html>
  if (import.meta.client) {
    watch(activeTheme, (newTheme) => {
      document.documentElement.setAttribute('data-theme', newTheme)
      document.documentElement.style.colorScheme = newTheme
    }, { immediate: true })
  }
})
