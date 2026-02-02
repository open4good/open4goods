import { computed, unref, type MaybeRef } from 'vue'
import { useTheme } from 'vuetify'

import {
  PARALLAX_SECTION_KEYS,
  THEME_ASSETS_FALLBACK,
  type ParallaxLayerConfig,
  type ParallaxLayerSource,
  type ParallaxSectionKey,
  type ParallaxPackConfig,
} from '~~/config/theme/assets'
import { resolveThemeName, type ThemeName } from '~~/shared/constants/theme'
import { resolveThemedAssetUrl } from './useThemedAsset'

const normalizeParallaxAsset = (value: string): string => {
  const trimmed = value.trim()

  if (!trimmed || trimmed.startsWith('/') || trimmed.startsWith('http')) {
    return trimmed
  }

  if (trimmed.includes('/')) {
    return trimmed
  }

  return `parallax/${trimmed}`
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
      const normalized = normalizeParallaxAsset(source)
      if (!normalized) {
        return null
      }

      const resolved = resolveThemedAssetUrl(normalized, themeName)

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

const DEFAULT_PARALLAX_ASSETS: ParallaxPackConfig = {
  essentials: ['parallax/parallax-placeholder.svg'],
  features: ['parallax/parallax-placeholder.svg'],
  blog: ['parallax/parallax-placeholder.svg'],
  objections: ['parallax/parallax-placeholder.svg'],
  cta: ['parallax/parallax-placeholder.svg'],
}

export const useThemedParallaxBackgrounds = (
  dynamicOverrides?: MaybeRef<
    Partial<Record<ParallaxSectionKey, ParallaxLayerSource[]>>
  >
) => {
  const theme = useTheme()

  const themeName = computed<ThemeName>(() =>
    resolveThemeName(theme.global.name.value, THEME_ASSETS_FALLBACK)
  )

  return computed<Record<ParallaxSectionKey, ParallaxLayerConfig[]>>(() => {
    const overrides = unref(dynamicOverrides)

    return PARALLAX_SECTION_KEYS.reduce<
      Record<ParallaxSectionKey, ParallaxLayerConfig[]>
    >(
      (acc, section) => {
        const assets = overrides?.[section] ?? DEFAULT_PARALLAX_ASSETS[section]

        return {
          ...acc,
          [section]: resolveParallaxLayers(assets, themeName.value),
        }
      },
      {} as Record<ParallaxSectionKey, ParallaxLayerConfig[]>
    )
  })
}
