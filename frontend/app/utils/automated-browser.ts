export interface AutomatedBrowserSignals {
  userAgent?: string | null
  userAgentData?: {
    brands?: ReadonlyArray<{ brand: string; version: string }>
  } | null
  webdriver?: boolean | null
  languages?: ReadonlyArray<string> | null
  isSecureContext?: boolean
}

const CHROMIUM_BRANDS = new Set(['Chromium', 'Google Chrome'])

const startsWith = (value: unknown, prefix: string) =>
  typeof value === 'string' && value.toLowerCase().startsWith(prefix)

/**
 * Ferme de vrais Chrome de bureau (GOU-2573) : Windows ou Linux, hors dérivés
 * (Edge, Opera...), langue `['en-US', 'en']` ou `zh-CN…`, sans aucune langue fr.
 */
const isDesktopChromeWithBotLanguages = (
  ua: string,
  languages: AutomatedBrowserSignals['languages']
): boolean => {
  if (!Array.isArray(languages) || languages.length === 0) {
    return false
  }
  if (
    !/Chrome\/\d+/.test(ua) ||
    !/Windows NT|X11; Linux/.test(ua) ||
    /Android|CrOS|Macintosh|Mobile|Edg\/|EdgA\/|EdgiOS\/|OPR\/|Opera|CriOS|Firefox|SamsungBrowser|YaBrowser|Vivaldi/i.test(
      ua
    )
  ) {
    return false
  }
  if (languages.some(lang => startsWith(lang, 'fr'))) {
    return false
  }
  const enUs =
    languages.length === 2 && languages[0] === 'en-US' && languages[1] === 'en'
  return enUs || startsWith(languages[0], 'zh-cn')
}

/**
 * Heuristique prudente (GOU-1973) : seuls les cas très probablement
 * automatisés sont signalés. Android, iOS, Firefox, Safari et les contextes non
 * sécurisés ne sont jamais écartés (risque de faux positifs).
 */
export const isLikelyAutomatedBrowser = ({
  userAgent,
  userAgentData,
  webdriver,
  languages,
  isSecureContext,
}: AutomatedBrowserSignals): boolean => {
  if (webdriver === true) {
    return true
  }

  const ua = userAgent ?? ''
  if (/HeadlessChrome|Lightpanda/i.test(ua)) {
    return true
  }

  if (isDesktopChromeWithBotLanguages(ua, languages)) {
    return true
  }

  const major = /Chrome\/(\d+)/.exec(ua)?.[1]
  const desktop = /Windows|Macintosh|X11|Linux/.test(ua) && !/Android/i.test(ua)
  if (!major || !desktop || isSecureContext !== true) {
    return false
  }

  if (!userAgentData) {
    return true
  }

  return !(userAgentData.brands ?? []).some(
    ({ brand, version }) => CHROMIUM_BRANDS.has(brand) && version === major
  )
}

export const detectAutomatedBrowser = (): boolean => {
  if (typeof navigator === 'undefined') {
    return false
  }
  return isLikelyAutomatedBrowser({
    userAgent: navigator.userAgent,
    userAgentData: (
      navigator as unknown as {
        userAgentData?: AutomatedBrowserSignals['userAgentData']
      }
    ).userAgentData,
    webdriver: navigator.webdriver,
    languages: navigator.languages,
    isSecureContext: typeof window !== 'undefined' && window.isSecureContext,
  })
}
