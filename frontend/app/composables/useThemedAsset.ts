import { computed } from 'vue'
import { useTheme } from 'vuetify'

import {
  THEME_ASSETS_FALLBACK,
  seasonalThemeAssets,
  themeAssets,
  type ThemeAssetKey,
  type EventPackName,
} from '~~/config/theme/assets'
import { useSeasonalEventPack } from './useSeasonalEventPack'
import { resolveThemeName, type ThemeName } from '~~/shared/constants/theme'

export type ThemedAssetIndex = Record<string, string>

const rawAssetIndex = import.meta.glob(
  '../assets/themes/**/*.{png,jpg,jpeg,svg,webp,avif,ico}',
  {
    eager: true,
    import: 'default',
  }
) as Record<string, string>

const normalizePath = (filePath: string): string =>
  filePath
    .replace(/\\/g, '/')
    .replace(/^.*assets\/themes\//, '')
    .replace(/^\//, '')

const themedAssetIndex: ThemedAssetIndex = Object.entries(rawAssetIndex).reduce(
  (acc, [filePath, url]) => ({
    ...acc,
    [normalizePath(filePath)]: url as string,
  }),
  {}
)

export const resolveAssetPathForTheme = (
  assetKey: ThemeAssetKey,
  themeName: ThemeName,
  seasonalPack?: EventPackName
): string[] => {
  const seasonalTheme = seasonalPack
    ? seasonalThemeAssets[seasonalPack]?.[themeName]?.[assetKey]
    : undefined
  const seasonalCommon = seasonalPack
    ? seasonalThemeAssets[seasonalPack]?.common?.[assetKey]
    : undefined
  const seasonalFallback = seasonalPack
    ? seasonalThemeAssets[seasonalPack]?.[THEME_ASSETS_FALLBACK]?.[assetKey]
    : undefined

  const fromTheme = themeAssets[themeName]?.[assetKey]
  const fromCommon = themeAssets.common?.[assetKey]
  const fallback = themeAssets[THEME_ASSETS_FALLBACK]?.[assetKey]

  const candidates = [
    seasonalTheme,
    seasonalCommon,
    seasonalFallback,
    fromTheme,
    fromCommon,
    fallback,
  ].filter(Boolean) as string[]

  return Array.from(new Set(candidates))
}

export const resolveThemedAssetUrlFromIndex = (
  relativePath: string | string[],
  themeName: ThemeName,
  index: ThemedAssetIndex,
  fallbackTheme: ThemeName = THEME_ASSETS_FALLBACK,
  seasonalPack?: EventPackName
): string | undefined => {
  const sanitizedPaths = (Array.isArray(relativePath)
    ? relativePath
    : [relativePath]
  ).map(path => path.replace(/^\//, ''))

  const candidates = sanitizedPaths.flatMap(path => [
    ...(seasonalPack
      ? [
          `${themeName}/${seasonalPack}/${path}`,
          `common/${seasonalPack}/${path}`,
          `${fallbackTheme}/${seasonalPack}/${path}`,
        ]
      : []),
    `${themeName}/${path}`,
    `common/${path}`,
    `${fallbackTheme}/${path}`,
  ])

  return candidates.reduce<string | undefined>((resolved, candidate) => {
    if (resolved) {
      return resolved
    }

    return index[candidate]
  }, undefined)
}

const useCurrentThemeName = () => {
  const vuetifyTheme = useTheme()

  return computed<ThemeName>(() =>
    resolveThemeName(vuetifyTheme.global.name.value, THEME_ASSETS_FALLBACK)
  )
}

export const resolveThemedAssetUrl = (
  relativePath: string | string[],
  themeName: ThemeName,
  seasonalPack?: EventPackName
): string | undefined =>
  resolveThemedAssetUrlFromIndex(
    relativePath,
    themeName,
    themedAssetIndex,
    THEME_ASSETS_FALLBACK,
    seasonalPack
  )

export const useThemedAsset = (relativePath: string) => {
  const themeName = useCurrentThemeName()
  const seasonalPack = useSeasonalEventPack()

  return computed(
    () =>
      resolveThemedAssetUrl(
        relativePath,
        themeName.value,
        seasonalPack.value
      ) ?? ''
  )
}

export const useThemeAsset = (assetKey: ThemeAssetKey) => {
  const themeName = useCurrentThemeName()
  const seasonalPack = useSeasonalEventPack()

  return computed(() => {
    const relativePaths = resolveAssetPathForTheme(
      assetKey,
      themeName.value,
      seasonalPack.value
    )

    if (!relativePaths.length) {
      return ''
    }

    return (
      resolveThemedAssetUrl(
        relativePaths,
        themeName.value,
        seasonalPack.value
      ) ?? ''
    )
  })
}

export const useLogoAsset = () => useThemeAsset('logo')

export const useFooterLogoAsset = () => useThemeAsset('footerLogo')

export const useFaviconAsset = () => useThemeAsset('favicon')

export const useHeroBackgroundAsset = () => useThemeAsset('heroBackground')

export const useIllustrationAsset = () => useThemeAsset('illustration')
