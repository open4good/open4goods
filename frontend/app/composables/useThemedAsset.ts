import { computed, toValue, type MaybeRef } from 'vue'
import { useTheme } from 'vuetify'

import {
  THEME_ASSETS_FALLBACK,
  themeAssets,
  type ThemeAssetKey,
} from '~~/config/theme/assets'
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

export const resolveThemedAssetUrlFromIndex = (
  relativePath: string | string[],
  themeName: ThemeName,
  index: ThemedAssetIndex,
  fallbackTheme: ThemeName = THEME_ASSETS_FALLBACK
): string | undefined => {
  const sanitizedPaths = (
    Array.isArray(relativePath) ? relativePath : [relativePath]
  ).map(path => path.replace(/^\//, ''))

  const candidates = sanitizedPaths.flatMap(path => [
    `${themeName}/${path}`,
    `common/${path}`,
    `${fallbackTheme}/${path}`,
  ])

  const result = candidates.reduce<string | undefined>(
    (resolved, candidate) => {
      if (resolved) {
        return resolved
      }
      return index[candidate]
    },
    undefined
  )

  return result
}

const useCurrentThemeName = () => {
  const vuetifyTheme = useTheme()

  return computed<ThemeName>(() =>
    resolveThemeName(vuetifyTheme.global.name.value, THEME_ASSETS_FALLBACK)
  )
}

export const resolveThemedAssetUrl = (
  relativePath: string | string[],
  themeName: ThemeName
): string | undefined =>
  resolveThemedAssetUrlFromIndex(
    relativePath,
    themeName,
    themedAssetIndex,
    THEME_ASSETS_FALLBACK
  )

export const useThemedAsset = (relativePath: string) => {
  const themeName = useCurrentThemeName()

  return computed(
    () => resolveThemedAssetUrl(relativePath, themeName.value) ?? ''
  )
}

export const useThemeAsset = (assetKey: MaybeRef<ThemeAssetKey>) => {
  const themeName = useCurrentThemeName()

  return computed(() => {
    const key = toValue(assetKey)
    if (!key) return ''

    const candidates: string[] = []

    const fromTheme = themeAssets[themeName.value]?.[key]
    const fromCommon = themeAssets.common?.[key]
    const fallback = themeAssets[THEME_ASSETS_FALLBACK]?.[key]

    if (fromTheme) candidates.push(fromTheme)
    if (fromCommon) candidates.push(fromCommon)
    if (fallback) candidates.push(fallback)

    const uniqueCandidates = Array.from(new Set(candidates))

    if (!uniqueCandidates.length) {
      return ''
    }

    return resolveThemedAssetUrl(uniqueCandidates, themeName.value) ?? ''
  })
}

export const useLogoAsset = () => useThemeAsset('logo')

export const useFooterLogoAsset = () => useThemeAsset('footerLogo')

export const useFaviconAsset = () => useThemeAsset('favicon')

export const useHeroBackgroundAsset = () => useThemeAsset('heroBackground')

export const useIllustrationAsset = () => useThemeAsset('illustration')
