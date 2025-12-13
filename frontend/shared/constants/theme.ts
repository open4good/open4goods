export const THEME_NAMES = ['light', 'dark'] as const

export type ThemeName = (typeof THEME_NAMES)[number]

export const THEME_PREFERENCE_KEY = 'open4goods-preferred-theme'

export const resolveThemeName = (
  value: string | null | undefined,
  fallback: ThemeName = 'light',
): ThemeName => {
  const normalizedValue = value === 'nudger' ? 'light' : value

  if (normalizedValue && (THEME_NAMES as readonly string[]).includes(normalizedValue)) {
    return normalizedValue as ThemeName
  }

  return fallback
}
