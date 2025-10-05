export type ThemeName = 'light' | 'dark'

export const THEME_PREFERENCE_KEY = 'open4goods-preferred-theme'

export const resolveThemeName = (
  value: string | null | undefined,
  fallback: ThemeName = 'light',
): ThemeName => {
  if (value === 'dark') {
    return 'dark'
  }

  if (value === 'light') {
    return 'light'
  }

  return fallback
}
