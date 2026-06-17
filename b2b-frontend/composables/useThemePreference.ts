export type PdapiThemeName = 'light' | 'dark'
export type ThemePreference = 'system' | PdapiThemeName

export const THEME_PREFERENCE_STORAGE_KEY = 'pdapi.theme.preference'

export function getSystemThemePreference(): PdapiThemeName {
  if (import.meta.server) {
    return 'light'
  }

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function resolveThemeFromPreference(preference: ThemePreference): PdapiThemeName {
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
  const systemTheme = useState<PdapiThemeName>('theme.system-theme', () => getSystemThemePreference())

  if (import.meta.client) {
    const windowWithFlag = window as Window & { __pdapiThemeListenerBound?: boolean }

    if (!windowWithFlag.__pdapiThemeListenerBound) {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      const onChange = () => {
        systemTheme.value = mediaQuery.matches ? 'dark' : 'light'
      }

      mediaQuery.addEventListener('change', onChange)
      windowWithFlag.__pdapiThemeListenerBound = true
    }
  }

  const activeTheme = computed<PdapiThemeName>(() => preference.value === 'system' ? systemTheme.value : preference.value)

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
