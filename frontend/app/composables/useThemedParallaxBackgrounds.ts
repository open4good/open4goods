import { computed, unref, type MaybeRef } from 'vue'
import { useTheme } from 'vuetify'

import {
  PARALLAX_SECTION_KEYS,
  DEFAULT_PARALLAX_PACK,
  THEME_ASSETS_FALLBACK,
  parallaxPacks,
  type ParallaxPackConfig,
  type ParallaxPackName,
  type ParallaxSectionKey,
} from '~~/config/theme/assets'
import { resolveThemeName, type ThemeName } from '~~/shared/constants/theme'
import { resolveThemedAssetUrl } from './useThemedAsset'

const resolvePackForTheme = (
  packName: ParallaxPackName,
  themeName: ThemeName,
  fallbackPackName: ParallaxPackName = DEFAULT_PARALLAX_PACK
): ParallaxPackConfig => {
  const themePack = parallaxPacks[themeName]?.[packName]
  const commonPack = parallaxPacks.common?.[packName]
  const fallbackPack = parallaxPacks[THEME_ASSETS_FALLBACK]?.[packName]

  if (themePack || commonPack || fallbackPack) {
    return themePack ?? commonPack ?? fallbackPack ?? {}
  }

  if (fallbackPackName !== packName) {
    return resolvePackForTheme(fallbackPackName, themeName, fallbackPackName)
  }

  return {}
}

const resolveParallaxLayers = (
  assets: string[] | undefined,
  themeName: ThemeName
) => {
  if (!assets?.length) {
    return []
  }

  return assets
    .map(asset => resolveThemedAssetUrl(asset, themeName))
    .filter((resolved): resolved is string => Boolean(resolved))
}

export const useThemedParallaxBackgrounds = (
  packName: MaybeRef<ParallaxPackName>,
  dynamicOverrides?: MaybeRef<Partial<Record<ParallaxSectionKey, string[]>>>
) => {
  const theme = useTheme()

  const themeName = computed<ThemeName>(() =>
    resolveThemeName(theme.global.name.value, THEME_ASSETS_FALLBACK)
  )

  return computed<Record<ParallaxSectionKey, string[]>>(() => {
    const activePackName = unref(packName)
    const overrides = unref(dynamicOverrides)
    const packConfig = resolvePackForTheme(activePackName, themeName.value)
    const fallbackPack =
      activePackName === DEFAULT_PARALLAX_PACK
        ? packConfig
        : resolvePackForTheme(DEFAULT_PARALLAX_PACK, themeName.value)

    return PARALLAX_SECTION_KEYS.reduce<Record<ParallaxSectionKey, string[]>>(
      (acc, section) => {
        const assets =
          overrides?.[section] ?? packConfig[section] ?? fallbackPack[section]

        return {
          ...acc,
          [section]: resolveParallaxLayers(assets, themeName.value),
        }
      },
      {} as Record<ParallaxSectionKey, string[]>
    )
  })
}
