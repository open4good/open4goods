import { describe, expect, it } from 'vitest'

import {
  HOST_DOMAIN_LANGUAGE_MAP,
  buildI18nLocaleDomains,
  buildProductHreflangLinks,
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
      {} as Partial<
        Record<
          ReturnType<typeof getNuxtLocaleForDomainLanguage>,
          { domain: string; domains: string[] }
        >
      >
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

  it('maps the English apex and www hosts to the English locale', () => {
    expect(getDomainLanguageFromHostname('nudger.com')).toMatchObject({
      domainLanguage: 'en',
      locale: 'en-US',
      matched: true,
    })
    expect(getDomainLanguageFromHostname('www.nudger.com')).toMatchObject({
      domainLanguage: 'en',
      locale: 'en-US',
      matched: true,
    })
  })

  it('emits exactly fr-FR and x-default for product pages, both on nudger.fr', () => {
    const links = buildProductHreflangLinks(
      '/climatiseurs/8431312260509-climatisation-midea-mmcs12hrn8qrd0'
    )

    expect(links).toHaveLength(2)
    expect(links.map(link => link.hreflang)).toEqual(['fr-FR', 'x-default'])
    links.forEach(link => {
      expect(link.rel).toBe('alternate')
      expect(link.href).toBe(
        'https://nudger.fr/climatiseurs/8431312260509-climatisation-midea-mmcs12hrn8qrd0'
      )
    })
  })

  it('never emits an en-US alternate for product pages', () => {
    const links = buildProductHreflangLinks('/some-product')

    expect(links.some(link => link.hreflang === 'en-US')).toBe(false)
  })
})
