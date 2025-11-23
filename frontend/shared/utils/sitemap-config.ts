import { joinURL } from 'ufo'

import {
  type DomainLanguage,
  HOST_DOMAIN_LANGUAGE_MAP,
  DEFAULT_DOMAIN_LANGUAGE,
} from './domain-language'

export const SITEMAP_PATH_PREFIX = '/sitemap'
export const APP_ROUTES_SITEMAP_KEY = 'main-pages'

const DEFAULT_ADDITIONAL_SITEMAP_PATHS = [
  'https://nudger.fr/sitemap_legacy/blog-posts.xml',
  'https://nudger.fr/sitemap_legacy/category-pages.xml',
  'https://nudger.fr/sitemap_legacy/product-pages.xml',
  'https://nudger.fr/sitemap_legacy/wiki-pages.xml',
] as const

export interface DomainLanguageSitemapConfig {
  additionalPaths: readonly string[]
}

const FALLBACK_DOMAIN_LANGUAGE_SITEMAP_CONFIG: DomainLanguageSitemapConfig = {
  additionalPaths: DEFAULT_ADDITIONAL_SITEMAP_PATHS,
}

const CONFIGURED_DOMAIN_LANGUAGE_SITEMAPS: Partial<Record<DomainLanguage, DomainLanguageSitemapConfig>> = {
  en: {
    additionalPaths: DEFAULT_ADDITIONAL_SITEMAP_PATHS,
  },
  fr: {
    additionalPaths: DEFAULT_ADDITIONAL_SITEMAP_PATHS,
  },
}

const ABSOLUTE_URL_PATTERN = /^[a-zA-Z][a-zA-Z\d+\-.]*:/

const getAllDomainLanguages = (): DomainLanguage[] =>
  Array.from(new Set(Object.values(HOST_DOMAIN_LANGUAGE_MAP)))

const buildDomainLanguageConfigMap = (): Record<DomainLanguage, DomainLanguageSitemapConfig> => {
  const configMap = Object.create(null) as Record<DomainLanguage, DomainLanguageSitemapConfig>

  for (const domainLanguage of getAllDomainLanguages()) {
    configMap[domainLanguage] =
      CONFIGURED_DOMAIN_LANGUAGE_SITEMAPS[domainLanguage] ?? FALLBACK_DOMAIN_LANGUAGE_SITEMAP_CONFIG
  }

  return configMap
}

const DOMAIN_LANGUAGE_SITEMAP_CONFIG_MAP = buildDomainLanguageConfigMap()

export const getDomainLanguageSitemapConfig = (
  domainLanguage: DomainLanguage,
): DomainLanguageSitemapConfig =>
  DOMAIN_LANGUAGE_SITEMAP_CONFIG_MAP[domainLanguage] ?? FALLBACK_DOMAIN_LANGUAGE_SITEMAP_CONFIG

const resolveOrigin = (
  baseOrigin: string | null | undefined,
  host: string | null,
): string => {
  if (baseOrigin) {
    return baseOrigin.replace(/\/$/, '')
  }

  if (!host) {
    return 'https://nudger.com'
  }

  const normalizedHost = host.replace(/\/$/, '')
  const isLocal = normalizedHost === 'localhost' || normalizedHost.startsWith('localhost:') || normalizedHost.startsWith('127.0.0.1')

  const protocol = isLocal ? 'http' : 'https'

  return `${protocol}://${normalizedHost}`
}

export const getAdditionalSitemapUrlsForDomainLanguage = (
  domainLanguage: DomainLanguage = DEFAULT_DOMAIN_LANGUAGE,
  origin: string | null | undefined,
  host: string | null,
): string[] => {
  const { additionalPaths } = getDomainLanguageSitemapConfig(domainLanguage)

  if (!additionalPaths.length) {
    return []
  }

  const baseOrigin = resolveOrigin(origin, host)

  const urls = additionalPaths.map((pathOrUrl) => {
    if (ABSOLUTE_URL_PATTERN.test(pathOrUrl)) {
      return pathOrUrl
    }

    return joinURL(baseOrigin, pathOrUrl)
  })

  return Array.from(new Set(urls))
}

