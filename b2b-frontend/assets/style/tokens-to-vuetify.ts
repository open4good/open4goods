import tokens from './tokens.json'

export type PdapiTokens = typeof tokens
export type PdapiThemeName = keyof PdapiTokens['themes']

const FALLBACK_THEME: PdapiThemeName = 'dark'

function resolveThemeTokens(themeName: PdapiThemeName) {
  return tokens.themes[themeName] ?? tokens.themes[FALLBACK_THEME]
}

export function buildVuetifyThemes() {
  return {
    dark: {
      dark: true,
      colors: {
        ...tokens.themes.dark.color,
        'on-surface': tokens.themes.dark.landing.textPrimary,
        'on-surface-variant': tokens.themes.dark.landing.textSecondary,
        'on-background': tokens.themes.dark.landing.textPrimary
      }
    },
    light: {
      dark: false,
      colors: {
        ...tokens.themes.light.color,
        'on-surface': tokens.themes.light.landing.textPrimary,
        'on-surface-variant': tokens.themes.light.landing.textSecondary,
        'on-background': tokens.themes.light.landing.textPrimary
      }
    }
  }
}

export function buildCssVariables(themeName: PdapiThemeName) {
  const themeTokens = resolveThemeTokens(themeName)

  return {
    '--pdapi-token-color-primary': themeTokens.color.primary,
    '--pdapi-token-color-secondary': themeTokens.color.secondary,
    '--pdapi-token-color-surface': themeTokens.color.surface,
    '--pdapi-token-color-background': themeTokens.color.background,
    '--pdapi-token-color-success': themeTokens.color.success,
    '--pdapi-token-color-error': themeTokens.color.error,
    '--pdapi-token-color-warning': themeTokens.color.warning,
    '--pdapi-token-color-info': themeTokens.color.info,

    '--pdapi-token-color-bg-base': themeTokens.landing.bgBase,
    '--pdapi-token-color-bg-elevated': themeTokens.landing.bgElevated,
    '--pdapi-token-color-text-primary': themeTokens.landing.textPrimary,
    '--pdapi-token-color-text-secondary': themeTokens.landing.textSecondary,
    '--pdapi-token-color-line-subtle': themeTokens.landing.lineSubtle,
    '--pdapi-token-color-accent-primary': themeTokens.landing.accentPrimary,
    '--pdapi-token-color-accent-primary-soft': themeTokens.landing.accentPrimarySoft,
    '--pdapi-token-color-accent-glow': themeTokens.landing.accentGlow,
    '--pdapi-token-color-surface-input': themeTokens.landing.surfaceInput,
    '--pdapi-token-color-surface-footer': themeTokens.landing.surfaceFooter,

    '--pdapi-token-spacing-xs': tokens.spacing.xs,
    '--pdapi-token-spacing-sm': tokens.spacing.sm,
    '--pdapi-token-spacing-md': tokens.spacing.md,
    '--pdapi-token-spacing-lg': tokens.spacing.lg,
    '--pdapi-token-spacing-xl': tokens.spacing.xl
  }
}

export function applyThemeCssVariables(themeName: PdapiThemeName) {
  const cssVars = buildCssVariables(themeName)

  Object.entries(cssVars).forEach(([key, value]) => {
    document.documentElement.style.setProperty(key, value)
  })

  document.documentElement.style.setProperty('color-scheme', themeName)
}
