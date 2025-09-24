import type { NuxtLocale } from './domain-language'
import { DEFAULT_NUXT_LOCALE } from './domain-language'

export type LocalizedRouteName =
  | 'team'

export type LocalizedRoutePath = `/${string}`
export type LocalizedRoutePaths = Record<LocalizedRouteName, Record<NuxtLocale, LocalizedRoutePath>>

export const LOCALIZED_ROUTE_PATHS: LocalizedRoutePaths = {

  team: {
    'fr-FR': '/equipe',
    'en-US': '/team',
  },
} satisfies LocalizedRoutePaths

const SUPPORTED_LOCALES: readonly NuxtLocale[] = ['en-US', 'fr-FR'] as const

type RouteParams = Record<string, string | number | undefined>

const ROUTE_PARAM_PATTERN = /\[([^\]/]+)\]/g

const injectParamsIntoPath = (
  template: string,
  params: RouteParams,
): string =>
  template.replace(ROUTE_PARAM_PATTERN, (segment, paramName) => {
    const value = params[paramName]

    if (value === undefined || value === null) {
      throw new Error(`Missing parameter "${paramName}" for route template "${template}"`)
    }

    return encodeURIComponent(String(value))
  })

const normalizeRouteNameToPath = (routeName: string): LocalizedRoutePath => {
  if (!routeName || routeName === 'index') {
    return '/'
  }

  return (routeName.startsWith('/') ? routeName : `/${routeName}`) as LocalizedRoutePath
}

export const isSupportedLocale = (locale: string): locale is NuxtLocale =>
  (SUPPORTED_LOCALES as readonly string[]).includes(locale)

export const normalizeLocale = (locale: string | undefined | null): NuxtLocale =>
  (locale && isSupportedLocale(locale) ? locale : DEFAULT_NUXT_LOCALE)

export const resolveLocalizedRoutePath = (
  routeName: string,
  locale: string | undefined | null,
  params: RouteParams = {},
): string => {
  const normalizedLocale = normalizeLocale(locale)
  const localePaths = LOCALIZED_ROUTE_PATHS[routeName as LocalizedRouteName]
  const template = (localePaths?.[normalizedLocale] ?? normalizeRouteNameToPath(routeName)) as string

  return injectParamsIntoPath(template, params)
}

export const buildI18nPagesConfig = (): Record<string, Partial<Record<NuxtLocale, LocalizedRoutePath>>> =>
  Object.fromEntries(
    (Object.entries(LOCALIZED_ROUTE_PATHS) as [LocalizedRouteName, Record<NuxtLocale, LocalizedRoutePath>][]).map(
      ([routeName, locales]) => [routeName, locales],
    ),
  )
