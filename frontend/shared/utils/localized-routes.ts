import type { NuxtLocale } from './domain-language'
import { DEFAULT_NUXT_LOCALE } from './domain-language'

const SUPPORTED_LOCALES: readonly NuxtLocale[] = ['en-US', 'fr-FR'] as const

export type LocalizedRouteName =
  | 'compare'
  | 'partners'
  | 'team'
  | 'search'
  | LocalizedWikiRouteName

export type LocalizedRoutePath = `/${string}`
export type LocalizedRoutePaths = Record<
  LocalizedRouteName,
  Record<NuxtLocale, LocalizedRoutePath>
>

export interface LocalizedWikiRouteConfig {
  path: LocalizedRoutePath
  pageId: string
}

export type LocalizedWikiPaths = Record<
  string,
  Record<NuxtLocale, LocalizedWikiRouteConfig>
>

export const LOCALIZED_WIKI_PATHS = {
  'legal-notice': {
    'fr-FR': {
      path: '/mentions-legales',
      pageId: 'webpages:default:legal-notice:WebHome',
    },
    'en-US': {
      path: '/legal-notice',
      pageId: 'webpages:default:legal-notice:WebHome',
    },
  },
  'data-privacy': {
    'fr-FR': {
      path: '/politique-confidentialite',
      pageId: 'webpages:default:data-privacy:WebHome',
    },
    'en-US': {
      path: '/data-privacy',
      pageId: 'webpages:default:data-privacy:WebHome',
    },
  },
} satisfies LocalizedWikiPaths

export type LocalizedWikiRouteName = keyof typeof LOCALIZED_WIKI_PATHS

const mapWikiRoutesToLocalizedPaths = <
  T extends Record<string, Record<NuxtLocale, LocalizedWikiRouteConfig>>,
>(
  wikiRoutes: T
): { [K in keyof T]: { [L in keyof T[K]]: LocalizedRoutePath } } =>
  Object.fromEntries(
    Object.entries(wikiRoutes).map(([routeName, locales]) => [
      routeName,
      Object.fromEntries(
        Object.entries(locales).map(([locale, config]) => [locale, config.path])
      ),
    ])
  ) as { [K in keyof T]: { [L in keyof T[K]]: LocalizedRoutePath } }

const LOCALIZED_WIKI_ROUTE_PATHS =
  mapWikiRoutesToLocalizedPaths(LOCALIZED_WIKI_PATHS)

export const LOCALIZED_ROUTE_PATHS: LocalizedRoutePaths = {
  search: {
    'fr-FR': '/rechercher',
    'en-US': '/search',
  },
  compare: {
    'fr-FR': '/comparer',
    'en-US': '/compare',
  },
  partners: {
    'fr-FR': '/partenaires',
    'en-US': '/partners',
  },
  team: {
    'fr-FR': '/equipe',
    'en-US': '/team',
  },
  ...LOCALIZED_WIKI_ROUTE_PATHS,
} satisfies LocalizedRoutePaths

type RouteParams = Record<string, string | number | undefined>

const ROUTE_PARAM_PATTERN = /\[([^\]/]+)\]/g

const escapeRegex = (value: string): string =>
  value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const normalizeTemplate = (template: string): string => {
  if (!template || template === '/') {
    return '/'
  }

  return template.replace(/\/+$/u, '')
}

const PATH_MATCHER_CACHE = new Map<string, RegExp>()

const createPathMatcher = (template: string): RegExp => {
  const normalizedTemplate = normalizeTemplate(template)
  const cacheKey = normalizedTemplate
  const cachedMatcher = PATH_MATCHER_CACHE.get(cacheKey)

  if (cachedMatcher) {
    return cachedMatcher
  }

  let pattern = ''
  let lastIndex = 0

  for (const match of normalizedTemplate.matchAll(ROUTE_PARAM_PATTERN)) {
    const [segment] = match
    const matchIndex = match.index ?? 0

    pattern += escapeRegex(normalizedTemplate.slice(lastIndex, matchIndex))
    pattern += '[^/]+'
    lastIndex = matchIndex + segment.length
  }

  pattern += escapeRegex(normalizedTemplate.slice(lastIndex))

  const matcher = new RegExp(`^${pattern}(?:/)?$`, 'u')

  PATH_MATCHER_CACHE.set(cacheKey, matcher)

  return matcher
}

const injectParamsIntoPath = (template: string, params: RouteParams): string =>
  template.replace(ROUTE_PARAM_PATTERN, (segment, paramName) => {
    const value = params[paramName]

    if (value === undefined || value === null) {
      throw new Error(
        `Missing parameter "${paramName}" for route template "${template}"`
      )
    }

    return encodeURIComponent(String(value))
  })

const normalizeRouteNameToPath = (routeName: string): LocalizedRoutePath => {
  if (!routeName || routeName === 'index') {
    return '/'
  }

  return (
    routeName.startsWith('/') ? routeName : `/${routeName}`
  ) as LocalizedRoutePath
}

export const isSupportedLocale = (locale: string): locale is NuxtLocale =>
  (SUPPORTED_LOCALES as readonly string[]).includes(locale)

export const normalizeLocale = (
  locale: string | undefined | null
): NuxtLocale =>
  locale && isSupportedLocale(locale) ? locale : DEFAULT_NUXT_LOCALE

export const resolveLocalizedRoutePath = (
  routeName: string,
  locale: string | undefined | null,
  params: RouteParams = {}
): string => {
  const normalizedLocale = normalizeLocale(locale)
  const localePaths = LOCALIZED_ROUTE_PATHS[routeName as LocalizedRouteName]
  const template = (localePaths?.[normalizedLocale] ??
    normalizeRouteNameToPath(routeName)) as string

  return injectParamsIntoPath(template, params)
}

export const buildI18nPagesConfig = (): Record<
  string,
  Partial<Record<NuxtLocale, LocalizedRoutePath>>
> =>
  Object.fromEntries(
    (
      Object.entries(LOCALIZED_ROUTE_PATHS) as [
        LocalizedRouteName,
        Record<NuxtLocale, LocalizedRoutePath>,
      ][]
    ).map(([routeName, locales]) => [routeName, locales])
  )

const normalizePath = (path: string): string => {
  if (!path) {
    return '/'
  }

  const prefixedPath = path.startsWith('/') ? path : `/${path}`

  if (prefixedPath === '/') {
    return prefixedPath
  }

  return prefixedPath.replace(/\/+$/u, '')
}

export interface MatchedLocalizedRoute {
  routeName: LocalizedRouteName
  locale: NuxtLocale
}

export const matchLocalizedRouteByPath = (
  path: string
): MatchedLocalizedRoute | null => {
  const normalizedPath = normalizePath(path)

  for (const [routeName, locales] of Object.entries(LOCALIZED_ROUTE_PATHS) as [
    LocalizedRouteName,
    Record<NuxtLocale, LocalizedRoutePath>,
  ][]) {
    for (const [locale, template] of Object.entries(locales) as [
      NuxtLocale,
      LocalizedRoutePath,
    ][]) {
      const matcher = createPathMatcher(template)

      if (matcher.test(normalizedPath)) {
        return { routeName, locale }
      }
    }
  }

  return null
}

export interface MatchedLocalizedWikiRoute extends MatchedLocalizedRoute {
  pageId: string
}

export const matchLocalizedWikiRouteByPath = (
  path: string
): MatchedLocalizedWikiRoute | null => {
  const baseMatch = matchLocalizedRouteByPath(path)
  if (!baseMatch) {
    return null
  }

  if (
    !Object.prototype.hasOwnProperty.call(
      LOCALIZED_WIKI_PATHS,
      baseMatch.routeName
    )
  ) {
    return null
  }

  const wikiLocales =
    LOCALIZED_WIKI_PATHS[baseMatch.routeName as LocalizedWikiRouteName]
  const match = wikiLocales?.[baseMatch.locale]

  if (!match) {
    return null
  }

  return {
    ...baseMatch,
    pageId: match.pageId,
  }
}
