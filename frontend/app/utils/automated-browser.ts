export interface AutomatedBrowserSignals {
  userAgent?: string | null
  userAgentData?: {
    brands?: ReadonlyArray<{ brand: string; version: string }>
  } | null
  webdriver?: boolean | null
  isSecureContext?: boolean
}

const CHROMIUM_BRANDS = new Set(['Chromium', 'Google Chrome'])

/**
 * Heuristique prudente (GOU-1973) : seuls les cas très probablement
 * automatisés sont signalés. Android, iOS, Firefox, Safari et les contextes non
 * sécurisés ne sont jamais écartés (risque de faux positifs).
 */
export const isLikelyAutomatedBrowser = ({
  userAgent,
  userAgentData,
  webdriver,
  isSecureContext,
}: AutomatedBrowserSignals): boolean => {
  if (webdriver === true) {
    return true
  }

  const ua = userAgent ?? ''
  if (/HeadlessChrome|Lightpanda/i.test(ua)) {
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
    isSecureContext: typeof window !== 'undefined' && window.isSecureContext,
  })
}
