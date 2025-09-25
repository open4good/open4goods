import { en as vuetifyEn, fr as vuetifyFr } from 'vuetify/locale'

export interface LocaleDefinition {
  /**
   * Two-letter domain language identifier used across domain helpers.
   */
  domainLanguage: 'en' | 'fr'
  /**
   * Locale code consumed by Nuxt I18n.
   */
  nuxtLocale: 'en-US' | 'fr-FR'
  /**
   * Metadata required by the Nuxt I18n module.
   */
  i18n: {
    name: string
    file: string
  }
  /**
   * List of domains mapped to the locale. The first entry is considered the
   * primary domain when building Nuxt I18n domain configuration.
   */
  domains: readonly [string, ...string[]]
  /**
   * Vuetify locale bundle merged into Vue I18n to avoid missing key warnings.
   */
  vuetify: typeof vuetifyEn
}

export const LOCALE_DEFINITIONS = [
  {
    domainLanguage: 'en',
    nuxtLocale: 'en-US',
    i18n: {
      name: 'English',
      file: 'en-US.json',
    },
    domains: ['nudger.com', '127.0.0.1'],
    vuetify: vuetifyEn,
  },
  {
    domainLanguage: 'fr',
    nuxtLocale: 'fr-FR',
    i18n: {
      name: 'Français',
      file: 'fr-FR.json',
    },
    domains: ['nudger.fr', 'localhost'],
    vuetify: vuetifyFr,
  },
] as const satisfies readonly LocaleDefinition[]

export type DomainLanguage = (typeof LOCALE_DEFINITIONS)[number]['domainLanguage']
export type NuxtLocale = (typeof LOCALE_DEFINITIONS)[number]['nuxtLocale']

export const DEFAULT_DOMAIN_LANGUAGE: DomainLanguage = 'en'
export const DEFAULT_NUXT_LOCALE: NuxtLocale = 'en-US'

export const HOST_DOMAIN_LANGUAGE_MAP: Record<string, DomainLanguage> =
  LOCALE_DEFINITIONS.reduce((accumulator, definition) => {
    definition.domains.forEach((domain) => {
      accumulator[domain] = definition.domainLanguage
    })

    return accumulator
  }, {} as Record<string, DomainLanguage>)

