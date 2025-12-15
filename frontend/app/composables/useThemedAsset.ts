import { computed } from 'vue'
import { useTheme } from 'vuetify'

import { THEME_ASSETS_FALLBACK, themeAssets, type ThemeAssetKey } from '~~/config/theme/assets'
import { resolveThemeName, type ThemeName } from '~~/shared/constants/theme'

export type ThemedAssetIndex = Record<string, string>

const rawAssetIndex = import.meta.glob('../assets/themes/**/*.{png,jpg,jpeg,svg,webp,avif,ico}', {
  eager: true,
  import: 'default',
}) as Record<string, string>

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
  {},
)

export const resolveAssetPathForTheme = (
  assetKey: ThemeAssetKey,
  themeName: ThemeName,
): string | undefined => {
  const fromTheme = themeAssets[themeName]?.[assetKey]
  const fromCommon = themeAssets.common?.[assetKey]
  const fallback = themeAssets[THEME_ASSETS_FALLBACK]?.[assetKey]

  return fromTheme ?? fromCommon ?? fallback
}

export const resolveThemedAssetUrlFromIndex = (
  relativePath: string,
  themeName: ThemeName,
  index: ThemedAssetIndex,
  fallbackTheme: ThemeName = THEME_ASSETS_FALLBACK,
): string | undefined => {
  const sanitizedPath = relativePath.replace(/^\//, '')
  const candidates = [
    `${themeName}/${sanitizedPath}`,
    `common/${sanitizedPath}`,
    `${fallbackTheme}/${sanitizedPath}`,
  ]

  return candidates.reduce<string | undefined>((resolved, candidate) => {
    if (resolved) {
      return resolved
    }

    return index[candidate]
  }, undefined)
}

const useCurrentThemeName = () => {
  const vuetifyTheme = useTheme()

  return computed<ThemeName>(() => resolveThemeName(vuetifyTheme.global.name.value, THEME_ASSETS_FALLBACK))
}

export const resolveThemedAssetUrl = (
  relativePath: string,
  themeName: ThemeName,
): string | undefined => resolveThemedAssetUrlFromIndex(relativePath, themeName, themedAssetIndex)

export const useThemedAsset = (relativePath: string) => {
  const themeName = useCurrentThemeName()

  return computed(() => resolveThemedAssetUrl(relativePath, themeName.value) ?? '')
}

export const useThemeAsset = (assetKey: ThemeAssetKey) => {
  const themeName = useCurrentThemeName()

  return computed(() => {
    const relativePath = resolveAssetPathForTheme(assetKey, themeName.value)

    if (!relativePath) {
      return ''
    }

    return resolveThemedAssetUrl(relativePath, themeName.value) ?? ''
  })
}

export const useLogoAsset = () => useThemeAsset('logo')

export const useFooterLogoAsset = () => useThemeAsset('footerLogo')

export const useFaviconAsset = () => useThemeAsset('favicon')

export const useHeroBackgroundAsset = () => useThemeAsset('heroBackground')

export const useIllustrationAsset = () => useThemeAsset('illustration')
