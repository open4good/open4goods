import type { H3Event } from 'h3'
import { useRequestEvent, useState } from '#imports'

export type RawHostInput = string | string[] | null | undefined

export type DomainLanguage = 'en' | 'fr'

export const DEFAULT_LOCALE = 'en-US' as const
export const DEFAULT_DOMAIN_LANGUAGE: DomainLanguage = 'en'
export const DOMAIN_LANGUAGE_STATE_KEY = 'domain-language'

const HOST_LOCALE_MAP: Record<string, string> = {
  'nudger.com': DEFAULT_LOCALE,
  'nudger.fr': 'fr-FR',
  localhost: 'fr-FR',
  '127.0.0.1': DEFAULT_LOCALE,
}

const LOCALE_DOMAIN_LANGUAGE_MAP: Record<string, DomainLanguage> = {
  'fr-FR': 'fr',
  'en-US': 'en',
}

export interface DomainResolution {
  hostname: string | null
  locale: string
  domainLanguage: DomainLanguage
  matched: boolean
}

const DOMAIN_CONTEXT_KEY = '__domainLanguageContext'

export const normalizeHostname = (rawHost: RawHostInput): string | null => {
  if (!rawHost) {
    return null
  }

  const firstValue = Array.isArray(rawHost) ? rawHost[0] : rawHost
  if (!firstValue) {
    return null
  }

  const hostValue = firstValue.split(',')[0]?.trim()
  if (!hostValue) {
    return null
  }

  const hostname = hostValue.split(':')[0]?.toLowerCase()
  return hostname || null
}

export const resolveLocaleForHostname = (hostname: string | null) => {
  if (!hostname) {
    return {
      locale: DEFAULT_LOCALE,
      matched: false,
    }
  }

  const locale = HOST_LOCALE_MAP[hostname]

  if (!locale) {
    return {
      locale: DEFAULT_LOCALE,
      matched: false,
    }
  }

  return {
    locale,
    matched: true,
  }
}

export const resolveDomainLanguageForLocale = (locale: string): DomainLanguage => {
  return LOCALE_DOMAIN_LANGUAGE_MAP[locale] ?? DEFAULT_DOMAIN_LANGUAGE
}

export const resolveDomainContext = (rawHost: RawHostInput): DomainResolution => {
  const hostname = normalizeHostname(rawHost)
  const { locale, matched } = resolveLocaleForHostname(hostname)
  const domainLanguage = resolveDomainLanguageForLocale(locale)

  return {
    hostname,
    locale,
    domainLanguage,
    matched,
  }
}

const logUnknownHostname = (hostname: string | null) => {
  console.error(
    `[domain-language] Unknown hostname "${hostname ?? 'unknown'}" received. Falling back to ${DEFAULT_LOCALE}/${DEFAULT_DOMAIN_LANGUAGE}.`
  )
}

export const getRequestDomainContext = (event: H3Event): DomainResolution => {
  const context = event.context as Record<string, DomainResolution | undefined>
  const cached = context[DOMAIN_CONTEXT_KEY]
  if (cached) {
    return cached
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const resolution = resolveDomainContext(rawHost)

  if (!resolution.matched) {
    logUnknownHostname(resolution.hostname)
  }

  context[DOMAIN_CONTEXT_KEY] = resolution
  return resolution
}

export const getCurrentDomainLanguage = (): DomainLanguage => {
  if (import.meta.server) {
    const event = useRequestEvent()
    if (event) {
      return getRequestDomainContext(event).domainLanguage
    }
  }

  return useDomainLanguageState().value
}

export const useDomainLanguageState = () =>
  useState<DomainLanguage>(DOMAIN_LANGUAGE_STATE_KEY, () => DEFAULT_DOMAIN_LANGUAGE)

export const syncDomainLanguageState = (domainLanguage: DomainLanguage) => {
  const state = useDomainLanguageState()
  state.value = domainLanguage
  return state
}
