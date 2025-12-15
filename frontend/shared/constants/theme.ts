export const THEME_NAMES = ['light', 'dark'] as const

export type ThemeName = (typeof THEME_NAMES)[number]

export const THEME_PREFERENCE_KEY = 'open4goods-preferred-theme'

export const resolveThemeName = (
  value: string | null | undefined,
  fallback: ThemeName = 'light',
): ThemeName => {
  if (value && (THEME_NAMES as readonly string[]).includes(value)) {
    return value as ThemeName
  }

  return fallback
}
