export type InferaThemeName = 'light' | 'dark'
export type ThemePreference = 'system' | InferaThemeName

export const THEME_PREFERENCE_STORAGE_KEY = 'infera.theme.preference'

export function getSystemThemePreference(): InferaThemeName {
  if (import.meta.server) {
    return 'light'
  }

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function resolveThemeFromPreference(preference: ThemePreference): InferaThemeName {
  if (preference !== 'system') {
    return preference
  }

  return getSystemThemePreference()
}

export function useThemePreference() {
  const cookiePreference = useCookie<ThemePreference>(THEME_PREFERENCE_STORAGE_KEY, {
    default: () => 'light',
    watch: true
  })

  const preference = useState<ThemePreference>('theme.preference', () => cookiePreference.value ?? 'light')
  const systemTheme = useState<InferaThemeName>('theme.system-theme', () => getSystemThemePreference())

  if (import.meta.client) {
    const windowWithFlag = window as Window & { __inferaThemeListenerBound?: boolean }

    if (!windowWithFlag.__inferaThemeListenerBound) {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      const onChange = () => {
        systemTheme.value = mediaQuery.matches ? 'dark' : 'light'
      }

      mediaQuery.addEventListener('change', onChange)
      windowWithFlag.__inferaThemeListenerBound = true
    }
  }

  const activeTheme = computed<InferaThemeName>(() => preference.value === 'system' ? systemTheme.value : preference.value)

  function setPreference(nextPreference: ThemePreference) {
    preference.value = nextPreference
    cookiePreference.value = nextPreference
  }

  return {
    preference,
    activeTheme,
    setPreference
  }
}
