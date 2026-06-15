import { buildCssVariables, type InferaThemeName } from '~/assets/style/tokens-to-vuetify'

/**
 * Plugin to inject Design System CSS variables into the root element.
 * Handles both SSR (via useHead) and Client-side theme changes.
 */
export default defineNuxtPlugin(() => {
  const { activeTheme } = useThemePreference()

  // 1. SSR Injection: Inject tokens as a style block to avoid FOUC
  const cssVariables = computed(() => {
    const vars = buildCssVariables(activeTheme.value as InferaThemeName)
    const styleString = Object.entries(vars)
      .map(([key, value]) => `${key}: ${value};`)
      .join(' ')
    
    return `:root { ${styleString} color-scheme: ${activeTheme.value}; }`
  })

  useHead({
    style: [
      {
        id: 'infera-design-tokens',
        textContent: cssVariables
      }
    ]
  })

  // 2. Client-side: Sync color-scheme attribute on <html> for some browser UI elements
  if (import.meta.client) {
    watch(activeTheme, (newTheme) => {
      document.documentElement.setAttribute('data-theme', newTheme)
      document.documentElement.style.colorScheme = newTheme
    }, { immediate: true })
  }
})
