import tokens from './tokens.json'

export type InferaTokens = typeof tokens
export type InferaThemeName = keyof InferaTokens['themes']

const FALLBACK_THEME: InferaThemeName = 'dark'

function resolveThemeTokens(themeName: InferaThemeName) {
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

export function buildCssVariables(themeName: InferaThemeName) {
  const themeTokens = resolveThemeTokens(themeName)

  return {
    '--inf-token-color-primary': themeTokens.color.primary,
    '--inf-token-color-secondary': themeTokens.color.secondary,
    '--inf-token-color-surface': themeTokens.color.surface,
    '--inf-token-color-background': themeTokens.color.background,
    '--inf-token-color-success': themeTokens.color.success,
    '--inf-token-color-error': themeTokens.color.error,
    '--inf-token-color-warning': themeTokens.color.warning,
    '--inf-token-color-info': themeTokens.color.info,

    '--inf-token-color-bg-base': themeTokens.landing.bgBase,
    '--inf-token-color-bg-elevated': themeTokens.landing.bgElevated,
    '--inf-token-color-text-primary': themeTokens.landing.textPrimary,
    '--inf-token-color-text-secondary': themeTokens.landing.textSecondary,
    '--inf-token-color-line-subtle': themeTokens.landing.lineSubtle,
    '--inf-token-color-accent-primary': themeTokens.landing.accentPrimary,
    '--inf-token-color-accent-primary-soft': themeTokens.landing.accentPrimarySoft,
    '--inf-token-color-accent-glow': themeTokens.landing.accentGlow,
    '--inf-token-color-surface-input': themeTokens.landing.surfaceInput,
    '--inf-token-color-surface-footer': themeTokens.landing.surfaceFooter,

    '--inf-token-spacing-xs': tokens.spacing.xs,
    '--inf-token-spacing-sm': tokens.spacing.sm,
    '--inf-token-spacing-md': tokens.spacing.md,
    '--inf-token-spacing-lg': tokens.spacing.lg,
    '--inf-token-spacing-xl': tokens.spacing.xl
  }
}

export function applyThemeCssVariables(themeName: InferaThemeName) {
  const cssVars = buildCssVariables(themeName)

  Object.entries(cssVars).forEach(([key, value]) => {
    document.documentElement.style.setProperty(key, value)
  })

  document.documentElement.style.setProperty('color-scheme', themeName)
}
