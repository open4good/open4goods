import { useRequestHeaders } from '#app'

const DEFAULT_LOCALE = 'en-US' as const
const SUPPORTED_LOCALES = ['en-US', 'fr-FR'] as const

export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number]
export type DomainLanguage = 'en' | 'fr'

const HOST_LOCALE_MAP: Record<string, SupportedLocale> = {
  'nudger.com': DEFAULT_LOCALE,
  'nudger.fr': 'fr-FR',
  localhost: 'fr-FR',
  '127.0.0.1': DEFAULT_LOCALE,
}

const LOCALE_DOMAIN_LANGUAGE_MAP: Record<SupportedLocale, DomainLanguage> = {
  'en-US': 'en',
  'fr-FR': 'fr',
}

export const DEFAULT_DOMAIN_LANGUAGE = LOCALE_DOMAIN_LANGUAGE_MAP[DEFAULT_LOCALE]

export interface DomainResolution {
  hostname: string | null
  locale: SupportedLocale
  domainLanguage: DomainLanguage
  matched: boolean
}

const formatWarningMessage = (hostname: string | null) =>
  `[i18n] Unknown hostname "${hostname ?? 'unknown'}" received. Falling back to ${DEFAULT_LOCALE}.`

export const warnOnUnknownHostname = (
  resolution: Pick<DomainResolution, 'matched' | 'hostname'>,
  logger: (message: string) => void = console.warn
) => {
  if (!resolution.matched) {
    logger(formatWarningMessage(resolution.hostname))
  }
}

export const normalizeHostname = (
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

export const resolveDomainResolutionFromHostname = (
  hostname: string | null
): DomainResolution => {
  const locale = (hostname && HOST_LOCALE_MAP[hostname]) ?? DEFAULT_LOCALE
  const domainLanguage = LOCALE_DOMAIN_LANGUAGE_MAP[locale]
  const matched = Boolean(hostname && HOST_LOCALE_MAP[hostname])

  return {
    hostname,
    locale,
    domainLanguage,
    matched,
  }
}

export const resolveDomainResolutionFromHost = (
  rawHost: string | string[] | undefined | null
): DomainResolution => {
  const hostname = normalizeHostname(rawHost)
  return resolveDomainResolutionFromHostname(hostname)
}

export const resolveDomainResolutionFromHeaders = (
  headers: Record<string, string | string[] | undefined>
): DomainResolution => {
  const rawHost = headers['x-forwarded-host'] ?? headers.host
  return resolveDomainResolutionFromHost(rawHost ?? null)
}

export const resolveDomainResolutionFromRuntime = (): DomainResolution => {
  let rawHost: string | null = null

  if (import.meta.server) {
    const headers = useRequestHeaders(['x-forwarded-host', 'host'])
    rawHost = headers['x-forwarded-host'] ?? headers.host ?? null
  } else if (typeof window !== 'undefined') {
    rawHost = window.location.host
  }

  const resolution = resolveDomainResolutionFromHost(rawHost)

  if (import.meta.server) {
    warnOnUnknownHostname(resolution)
  }

  return resolution
}

export const formatUnknownHostnameWarning = (hostname: string | null) =>
  formatWarningMessage(hostname)

export { DEFAULT_LOCALE }
