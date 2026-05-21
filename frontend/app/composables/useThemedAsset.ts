import { computed, shallowRef, toValue, watchEffect, type MaybeRef } from 'vue'
import { useTheme } from 'vuetify'

import {
  THEME_ASSETS_FALLBACK,
  themeAssets,
  type ThemeAssetKey,
} from '~~/config/theme/assets'
import { resolveThemeName, type ThemeName } from '~~/shared/constants/theme'

export type ThemedAssetIndex = Record<string, string>
export type ThemedAssetLoaderIndex = Record<string, () => Promise<string>>

const rawAssetLoaders = import.meta.glob(
  '../assets/themes/**/*.{png,jpg,jpeg,svg,webp,avif,ico}',
  {
    import: 'default',
  }
) as Record<string, () => Promise<string>>

const normalizePath = (filePath: string): string =>
  filePath
    .replace(/\\/g, '/')
    .replace(/^.*assets\/themes\//, '')
    .replace(/^\//, '')

const themedAssetLoaders: ThemedAssetLoaderIndex = Object.entries(
  rawAssetLoaders
).reduce(
  (acc, [filePath, loader]) => ({
    ...acc,
    [normalizePath(filePath)]: loader,
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

export const resolveThemedAssetLoaderFromIndex = (
  relativePath: string | string[],
  themeName: ThemeName,
  index: ThemedAssetLoaderIndex,
  fallbackTheme: ThemeName = THEME_ASSETS_FALLBACK
): (() => Promise<string>) | undefined => {
  const sanitizedPaths = (
    Array.isArray(relativePath) ? relativePath : [relativePath]
  ).map(path => path.replace(/^\//, ''))

  const candidates = sanitizedPaths.flatMap(path => [
    `${themeName}/${path}`,
    `common/${path}`,
    `${fallbackTheme}/${path}`,
  ])

  return candidates.reduce<(() => Promise<string>) | undefined>(
    (resolved, candidate) => {
      if (resolved) {
        return resolved
      }
      return index[candidate]
    },
    undefined
  )
}

const useCurrentThemeName = () => {
  const vuetifyTheme = useTheme()

  return computed<ThemeName>(() =>
    resolveThemeName(vuetifyTheme.global.name.value, THEME_ASSETS_FALLBACK)
  )
}

export const loadThemedAssetUrl = async (
  relativePath: string | string[],
  themeName: ThemeName
): Promise<string | undefined> => {
  const loader = resolveThemedAssetLoaderFromIndex(
    relativePath,
    themeName,
    themedAssetLoaders,
    THEME_ASSETS_FALLBACK
  )

  return loader ? await loader() : undefined
}

export const useThemedAsset = (
  relativePath: MaybeRef<string | string[] | undefined>
) => {
  const themeName = useCurrentThemeName()
  const assetUrl = shallowRef('')
  let requestId = 0

  watchEffect(async () => {
    const currentRequestId = ++requestId
    const path = toValue(relativePath)

    if (
      !path ||
      (Array.isArray(path) && path.every(entry => !entry.trim().length)) ||
      (typeof path === 'string' && !path.trim().length)
    ) {
      assetUrl.value = ''
      return
    }

    const resolved = await loadThemedAssetUrl(path, themeName.value)

    if (currentRequestId === requestId) {
      assetUrl.value = resolved ?? ''
    }
  })

  return assetUrl
}

export const useThemeAsset = (assetKey: MaybeRef<ThemeAssetKey>) => {
  const themeName = useCurrentThemeName()
  const assetUrl = shallowRef('')
  let requestId = 0

  watchEffect(async () => {
    const currentRequestId = ++requestId
    const key = toValue(assetKey)
    if (!key) {
      assetUrl.value = ''
      return
    }

    const candidates: string[] = []

    const fromTheme = themeAssets[themeName.value]?.[key]
    const fromCommon = themeAssets.common?.[key]
    const fallback = themeAssets[THEME_ASSETS_FALLBACK]?.[key]

    if (fromTheme) candidates.push(fromTheme)
    if (fromCommon) candidates.push(fromCommon)
    if (fallback) candidates.push(fallback)

    const uniqueCandidates = Array.from(new Set(candidates))

    if (!uniqueCandidates.length) {
      assetUrl.value = ''
      return
    }

    const resolved = await loadThemedAssetUrl(uniqueCandidates, themeName.value)

    if (currentRequestId === requestId) {
      assetUrl.value = resolved ?? ''
    }
  })

  return assetUrl
}

export const useLogoAsset = () => useThemeAsset('logo')

export const useFooterLogoAsset = () => useThemeAsset('footerLogo')

export const useFaviconAsset = () => useThemeAsset('favicon')

export const useHeroBackgroundAsset = () => useThemeAsset('heroBackground')

export const useIllustrationAsset = () => useThemeAsset('illustration')
