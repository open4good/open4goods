import { computed, toValue } from 'vue'
import { useTheme } from 'vuetify'

import {
  THEME_ASSETS_FALLBACK,
  themeAssets,
  type ThemeAssetKey,
} from '~~/config/theme/assets'
import type { EventPackName } from '~~/config/theme/event-packs'
import { useSeasonalEventPack } from './useSeasonalEventPack'
import { resolveThemeName, type ThemeName } from '~~/shared/constants/theme'
import { useEventPackI18n } from './useEventPackI18n'

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

export const resolveThemedAssetUrlFromIndex = (
  relativePath: string | string[],
  themeName: ThemeName,
  index: ThemedAssetIndex,
  fallbackTheme: ThemeName = THEME_ASSETS_FALLBACK,
  seasonalPack?: EventPackName
): string | undefined => {
  const sanitizedPaths = (
    Array.isArray(relativePath) ? relativePath : [relativePath]
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
  const packI18n = useEventPackI18n(seasonalPack)

  return computed(() => {
    const pack = toValue(seasonalPack)
    const i18nAsset = packI18n.resolveString(`assets.${assetKey}`)

    // 1. Resolve candidates from i18n
    const candidates: string[] = []
    if (i18nAsset) {
      candidates.push(i18nAsset)
    }

    // 2. Add fallback candidates from static themeAssets config
    const fromTheme = themeAssets[themeName.value]?.[assetKey]
    const fromCommon = themeAssets.common?.[assetKey]
    const fallback = themeAssets[THEME_ASSETS_FALLBACK]?.[assetKey]

    if (fromTheme) candidates.push(fromTheme)
    if (fromCommon) candidates.push(fromCommon)
    if (fallback) candidates.push(fallback)

    const uniqueCandidates = Array.from(new Set(candidates))

    if (!uniqueCandidates.length) {
      return ''
    }

    return resolveThemedAssetUrl(uniqueCandidates, themeName.value, pack) ?? ''
  })
}

export const useLogoAsset = () => useThemeAsset('logo')

export const useFooterLogoAsset = () => useThemeAsset('footerLogo')

export const useFaviconAsset = () => useThemeAsset('favicon')

export const useHeroBackgroundAsset = () => useThemeAsset('heroBackground')

export const useIllustrationAsset = () => useThemeAsset('illustration')
