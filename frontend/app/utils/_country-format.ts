/**
 * Helpers to present an ISO 3166-1 alpha-2 country code (as stored in
 * `gtinInfos.country`) as a Unicode flag + a localised country name.
 *
 * No third-party dependency: flags are built from Regional Indicator Symbols
 * and names come from the built-in `Intl.DisplayNames` API.
 */

const ALPHA2_PATTERN = /^[A-Za-z]{2}$/
const REGIONAL_INDICATOR_OFFSET = 0x1f1e6 - 'A'.charCodeAt(0)

/**
 * Return the emoji flag for a 2-letter country code, or an empty string when
 * the code is not a valid alpha-2 value.
 */
export const countryFlag = (code?: string | null): string => {
  if (!code || !ALPHA2_PATTERN.test(code)) {
    return ''
  }

  return [...code.toUpperCase()]
    .map(char =>
      String.fromCodePoint(char.charCodeAt(0) + REGIONAL_INDICATOR_OFFSET)
    )
    .join('')
}

const displayNamesCache = new Map<string, Intl.DisplayNames | null>()

const resolveDisplayNames = (locale: string): Intl.DisplayNames | null => {
  if (displayNamesCache.has(locale)) {
    return displayNamesCache.get(locale) ?? null
  }

  let instance: Intl.DisplayNames | null = null
  try {
    instance = new Intl.DisplayNames([locale], { type: 'region' })
  } catch {
    instance = null
  }

  displayNamesCache.set(locale, instance)
  return instance
}

/**
 * Return the localised country name for a 2-letter code, falling back to the
 * raw (upper-cased) code when it cannot be resolved.
 */
export const countryName = (code?: string | null, locale = 'en'): string => {
  if (!code) {
    return ''
  }

  const upper = code.toUpperCase()
  if (!ALPHA2_PATTERN.test(code)) {
    return upper
  }

  try {
    return resolveDisplayNames(locale)?.of(upper) ?? upper
  } catch {
    return upper
  }
}

/**
 * Combined "🇫🇷 France" label. Falls back gracefully to whichever part resolves.
 */
export const formatCountryLabel = (
  code?: string | null,
  locale = 'en'
): string => {
  const flag = countryFlag(code)
  const name = countryName(code, locale)
  return flag ? `${flag} ${name}`.trim() : name
}
