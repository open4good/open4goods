import { computed, unref, type MaybeRef } from 'vue'
import { useTheme } from 'vuetify'

import {
  PARALLAX_SECTION_KEYS,
  DEFAULT_PARALLAX_PACK,
  THEME_ASSETS_FALLBACK,
  parallaxPacks,
  type ParallaxPackConfig,
  type ParallaxPackName,
  type ParallaxLayerConfig,
  type ParallaxLayerSource,
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
  assets: ParallaxLayerSource[] | undefined,
  themeName: ThemeName
): ParallaxLayerConfig[] => {
  if (!assets?.length) {
    return []
  }

  return assets
    .map(asset => {
      const source = typeof asset === 'string' ? asset : asset.src
      const resolved = resolveThemedAssetUrl(source, themeName)

      if (!resolved) {
        return null
      }

      if (typeof asset === 'string') {
        return { src: resolved }
      }

      const blendMode = asset.blendMode?.trim()

      return {
        src: resolved,
        speed: asset.speed,
        blendMode: blendMode?.length ? blendMode : undefined,
      }
    })
    .filter((resolved): resolved is ParallaxLayerConfig => Boolean(resolved))
}

export const useThemedParallaxBackgrounds = (
  packName: MaybeRef<ParallaxPackName>,
  dynamicOverrides?: MaybeRef<
    Partial<Record<ParallaxSectionKey, ParallaxLayerSource[]>>
  >
) => {
  const theme = useTheme()

  const themeName = computed<ThemeName>(() =>
    resolveThemeName(theme.global.name.value, THEME_ASSETS_FALLBACK)
  )

  return computed<Record<ParallaxSectionKey, ParallaxLayerConfig[]>>(() => {
    const activePackName = unref(packName)
    const overrides = unref(dynamicOverrides)
    const packConfig = resolvePackForTheme(activePackName, themeName.value)
    const fallbackPack =
      activePackName === DEFAULT_PARALLAX_PACK
        ? packConfig
        : resolvePackForTheme(DEFAULT_PARALLAX_PACK, themeName.value)

    return PARALLAX_SECTION_KEYS.reduce<
      Record<ParallaxSectionKey, ParallaxLayerConfig[]>
    >(
      (acc, section) => {
        const assets =
          overrides?.[section] ?? packConfig[section] ?? fallbackPack[section]

        return {
          ...acc,
          [section]: resolveParallaxLayers(assets, themeName.value),
        }
      },
      {} as Record<ParallaxSectionKey, ParallaxLayerConfig[]>
    )
  })
}
