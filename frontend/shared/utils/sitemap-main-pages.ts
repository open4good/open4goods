import { resolveLocalizedRoutePath } from './localized-routes'
import {
  type DomainLanguage,
  type NuxtLocale,
  getNuxtLocaleForDomainLanguage,
} from './domain-language'

const STATIC_ROUTE_NAMES = [
  'index',
  'blog',
  'categories',
  'contact',
  'feedback',
  'impact-score',
  'opendata',
  'opendata-gtin',
  'opendata-isbn',
  'opensource',
  'partners',
  'team',
  'legal-notice',
  'data-privacy',
] as const

export type StaticMainPageRouteName = (typeof STATIC_ROUTE_NAMES)[number]

const buildRoutePathsForLocale = (locale: NuxtLocale): string[] => {
  const seenPaths = new Set<string>()

  STATIC_ROUTE_NAMES.forEach((routeName) => {
    const path = resolveLocalizedRoutePath(routeName, locale)

    if (!path) {
      return
    }

    seenPaths.add(path)
  })

  return Array.from(seenPaths)
}

export const getMainPagePathsForDomainLanguage = (
  domainLanguage: DomainLanguage,
): string[] => {
  const locale = getNuxtLocaleForDomainLanguage(domainLanguage)

  return buildRoutePathsForLocale(locale)
}
