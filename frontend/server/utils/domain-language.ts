import type { H3Event } from 'h3'

export type DomainLanguage = 'en' | 'fr'

const DEFAULT_DOMAIN_LANGUAGE: DomainLanguage = 'en'

const HOST_DOMAIN_LANGUAGE_MAP: Record<string, DomainLanguage> = {
  'nudger.com': 'en',
  'nudger.fr': 'fr',
  localhost: 'fr',
  '127.0.0.1': 'en',
}

type NormalisableHost = string | string[] | undefined

const normalizeHostname = (rawHost: NormalisableHost): string | null => {
  if (!rawHost) {
    return null
  }

  const value = Array.isArray(rawHost) ? rawHost[0] : rawHost
  if (!value) {
    return null
  }

  const firstHost = value.split(',')[0]?.trim()
  if (!firstHost) {
    return null
  }

  const hostname = firstHost.split(':')[0]?.toLowerCase()
  return hostname || null
}

export interface DomainLanguageResolution {
  hostname: string | null
  domainLanguage: DomainLanguage
  matched: boolean
}

const resolveFromHostname = (hostname: string | null) => {
  if (!hostname) {
    return {
      domainLanguage: DEFAULT_DOMAIN_LANGUAGE,
      matched: false,
    } as const
  }

  const domainLanguage = HOST_DOMAIN_LANGUAGE_MAP[hostname]
  if (!domainLanguage) {
    return {
      domainLanguage: DEFAULT_DOMAIN_LANGUAGE,
      matched: false,
    } as const
  }

  return {
    domainLanguage,
    matched: true,
  } as const
}

export const resolveDomainLanguageFromEvent = (
  event: H3Event
): DomainLanguageResolution => {
  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host

  const hostname = normalizeHostname(rawHost)
  const { domainLanguage, matched } = resolveFromHostname(hostname)

  if (!matched && hostname) {
    console.warn(
      `[domain-language] Unknown hostname "${hostname}" received. Falling back to ${DEFAULT_DOMAIN_LANGUAGE}.`
    )
  }

  return {
    hostname,
    domainLanguage,
    matched,
  }
}

export { DEFAULT_DOMAIN_LANGUAGE }
