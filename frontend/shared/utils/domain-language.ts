export type DomainLanguage = 'en' | 'fr'
export type NuxtLocale = 'en-US' | 'fr-FR'

export const DEFAULT_DOMAIN_LANGUAGE: DomainLanguage = 'fr'
export const DEFAULT_NUXT_LOCALE: NuxtLocale = 'fr-FR'

export const HOST_DOMAIN_LANGUAGE_MAP: Record<string, DomainLanguage> = {
  'nudger.fr': 'fr',
  'beta.nudger.fr': 'fr',
  localhost: 'fr',
  '127.0.0.1': 'fr',
  'nudger.com': 'en',
}

export interface NuxtI18nLocaleDomains {
  domain: string
  domains?: string[]
}

const DOMAIN_LANGUAGE_TO_LOCALE_MAP: Record<DomainLanguage, NuxtLocale> = {
  en: 'en-US',
  fr: 'fr-FR',
}

export interface ResolveDomainLanguageOptions {
  /**
   * Whether the helper should log when the incoming host cannot be matched.
   * Defaults to `true` to surface misconfigurations.
   */
  logUnknownHost?: boolean
  /**
   * Optional logger implementation. When omitted, the global console is used.
   */
  logger?: Pick<Console, 'warn'>
  /**
   * Optional prefix injected at the beginning of the warning message.
   */
  logPrefix?: string
}

export interface DomainLanguageResolution {
  domainLanguage: DomainLanguage
  locale: NuxtLocale
  hostname: string | null
  matched: boolean
}

export const normalizeHost = (
  rawHost: string | string[] | undefined | null
): string | null => {
  if (!rawHost) {
    return null
  }

  const hostValue = Array.isArray(rawHost) ? rawHost[0] : rawHost

  if (!hostValue) {
    return null
  }

  const firstHost = hostValue.split(',')[0]?.trim()

  if (!firstHost) {
    return null
  }

  const hostname = firstHost.split(':')[0]?.toLowerCase()

  return hostname || null
}

export const resolveDomainLanguage = (
  rawHost: string | string[] | undefined | null,
  options: ResolveDomainLanguageOptions = {}
): DomainLanguageResolution => {
  const hostname = normalizeHost(rawHost)
  const matchedDomainLanguage = hostname
    ? HOST_DOMAIN_LANGUAGE_MAP[hostname]
    : undefined

  if (!matchedDomainLanguage) {
    if (options.logUnknownHost ?? true) {
      const logger = options.logger ?? console
      const prefix = options.logPrefix ? `${options.logPrefix} ` : ''

      logger?.warn?.(
        `${prefix}Unknown hostname "${hostname ?? 'unknown'}" received. Falling back to ${DEFAULT_DOMAIN_LANGUAGE} / ${DEFAULT_NUXT_LOCALE}.`
      )
    }

    return {
      domainLanguage: DEFAULT_DOMAIN_LANGUAGE,
      locale: DEFAULT_NUXT_LOCALE,
      hostname,
      matched: false,
    }
  }

  const locale = DOMAIN_LANGUAGE_TO_LOCALE_MAP[matchedDomainLanguage]

  return {
    domainLanguage: matchedDomainLanguage,
    locale,
    hostname,
    matched: true,
  }
}

export const getNuxtLocaleForDomainLanguage = (
  domainLanguage: DomainLanguage
): NuxtLocale => DOMAIN_LANGUAGE_TO_LOCALE_MAP[domainLanguage]

export const buildI18nLocaleDomains = (): Record<NuxtLocale, NuxtI18nLocaleDomains> => {
  const localeHostMap = new Map<NuxtLocale, string[]>()

  Object.entries(HOST_DOMAIN_LANGUAGE_MAP).forEach(([hostname, domainLanguage]) => {
    const locale = getNuxtLocaleForDomainLanguage(domainLanguage)
    const hostsForLocale = localeHostMap.get(locale)

    if (hostsForLocale) {
      hostsForLocale.push(hostname)
      return
    }

    localeHostMap.set(locale, [hostname])
  })

  return Object.fromEntries(
    Array.from(localeHostMap.entries()).map(([locale, hostnames]) => {
      const [primaryDomain, ...alternateDomains] = hostnames

      return [
        locale,
        {
          domain: primaryDomain ?? '',
          ...(alternateDomains.length > 0 ? { domains: alternateDomains } : {}),
        } satisfies NuxtI18nLocaleDomains,
      ]
    }),
  ) as Record<NuxtLocale, NuxtI18nLocaleDomains>
}

export const getDomainLanguageFromHostname = (
  hostname: string | null
): DomainLanguageResolution =>
  resolveDomainLanguage(hostname, { logUnknownHost: false })

export const DEFAULT_DOMAIN_LANGUAGE_RESOLUTION: DomainLanguageResolution = {
  domainLanguage: DEFAULT_DOMAIN_LANGUAGE,
  locale: DEFAULT_NUXT_LOCALE,
  hostname: null,
  matched: false,
}
