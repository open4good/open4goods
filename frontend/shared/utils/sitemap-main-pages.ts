import { LOCALIZED_ROUTE_PATHS, resolveLocalizedRoutePath } from './localized-routes'
import {
  type DomainLanguage,
  type NuxtLocale,
  getNuxtLocaleForDomainLanguage,
} from './domain-language'

const parseRouteNames = (value: unknown): string[] => {
  if (Array.isArray(value)) {
    return value.filter((entry): entry is string => typeof entry === 'string' && entry.length > 0)
  }

  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)

      return Array.isArray(parsed)
        ? parsed.filter((entry): entry is string => typeof entry === 'string' && entry.length > 0)
        : []
    }
    catch {
      return []
    }
  }

  return []
}

const EXCLUDED_STATIC_ROUTE_NAMES = new Set(['offline'])

const isExcludedStaticRouteName = (routeName: string): boolean => {
  const normalizedName = routeName
    .replace(/^\/+/, '')
    .replace(/\/+$/, '')

  return EXCLUDED_STATIC_ROUTE_NAMES.has(normalizedName)
}

const getRuntimeConfiguredRouteNames = (): string[] => {
  try {
    const { staticMainPageRoutes, public: publicConfig } = useRuntimeConfig()
    const configured = staticMainPageRoutes ?? publicConfig?.staticMainPageRoutes
    const parsed = parseRouteNames(configured)

    if (parsed.length > 0) {
      return parsed
    }
  }
  catch {
    // Ignore runtime config lookup failures when executed outside a Nuxt context
  }

  if (typeof process !== 'undefined' && process.env?.NUXT_STATIC_MAIN_PAGE_ROUTES) {
    return parseRouteNames(process.env.NUXT_STATIC_MAIN_PAGE_ROUTES)
  }

  return []
}

let staticRouteNamesCache: string[] | null = null

const resolveStaticRouteNames = (): string[] => {
  if (staticRouteNamesCache) {
    return staticRouteNamesCache
  }

  const routeNames = new Set<string>()

  for (const routeName of getRuntimeConfiguredRouteNames()) {
    if (routeName && !isExcludedStaticRouteName(routeName)) {
      routeNames.add(routeName)
    }
  }

  for (const routeName of Object.keys(LOCALIZED_ROUTE_PATHS)) {
    if (!isExcludedStaticRouteName(routeName)) {
      routeNames.add(routeName)
    }
  }

  staticRouteNamesCache = Array.from(routeNames).sort((a, b) => a.localeCompare(b))

  return staticRouteNamesCache
}

export type StaticMainPageRouteName = string

const buildRoutePathsForLocale = (locale: NuxtLocale): string[] => {
  const seenPaths = new Set<string>()

  resolveStaticRouteNames().forEach((routeName) => {
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
