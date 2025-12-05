import { describe, expect, it } from 'vitest'

import {
  HOST_DOMAIN_LANGUAGE_MAP,
  buildI18nLocaleDomains,
  getDomainLanguageFromHostname,
  getNuxtLocaleForDomainLanguage,
} from './domain-language'

describe('domain-language helpers', () => {
  it('builds locale domain mapping for Nuxt i18n configuration', () => {
    const domainsConfig = buildI18nLocaleDomains()

    const expectedConfig = Object.entries(HOST_DOMAIN_LANGUAGE_MAP).reduce(
      (acc, [domain, domainLanguage]) => {
        const locale = getNuxtLocaleForDomainLanguage(domainLanguage)
        const current = acc[locale]

        if (!current) {
          acc[locale] = { domain, domains: [] }
          return acc
        }

        current.domains.push(domain)
        return acc
      },
      {} as Partial<Record<ReturnType<typeof getNuxtLocaleForDomainLanguage>, { domain: string; domains: string[] }>>,
    )

    Object.entries(expectedConfig).forEach(([locale, value]) => {
      const config = domainsConfig[locale as keyof typeof domainsConfig]

      expect(config.domain).toBe(value.domain)
      expect(config.domains ?? []).toEqual(value.domains)
    })
  })

  it('falls back to default mapping when hostname is missing', () => {
    const resolution = getDomainLanguageFromHostname(null)

    expect(resolution.domainLanguage).toBe('fr')
    expect(resolution.locale).toBe('fr-FR')
    expect(resolution.matched).toBe(false)
  })
})
