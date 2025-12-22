import { computed, unref, type MaybeRef } from 'vue'
import { useTheme } from 'vuetify'

import {
  PARALLAX_SECTION_KEYS,
  THEME_ASSETS_FALLBACK,
  DEFAULT_EVENT_PACK,
  eventParallaxPacks,
  type ParallaxPackConfig,
  type ParallaxLayerConfig,
  type ParallaxLayerSource,
  type ParallaxSectionKey,
  type EventPackName,
} from '~~/config/theme/assets'
import { resolveThemeName, type ThemeName } from '~~/shared/constants/theme'
import { resolveThemedAssetUrl } from './useThemedAsset'

const resolvePackForTheme = (
  packName: EventPackName,
  themeName: ThemeName,
  fallbackPackName: EventPackName = DEFAULT_EVENT_PACK
): ParallaxPackConfig => {
  const themePack = eventParallaxPacks[themeName]?.[packName]
  const commonPack = eventParallaxPacks.common?.[packName]
  const fallbackPack = eventParallaxPacks[THEME_ASSETS_FALLBACK]?.[packName]

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
  packName: MaybeRef<EventPackName>,
  dynamicOverrides?: MaybeRef<
    Partial<Record<ParallaxSectionKey, ParallaxLayerSource[]>>
  >
) => {
  const theme = useTheme()
  const { tm } = useI18n()

  const themeName = computed<ThemeName>(() =>
    resolveThemeName(theme.global.name.value, THEME_ASSETS_FALLBACK)
  )

  const resolveI18nPack = (eventPack: EventPackName): ParallaxPackConfig => {
    const i18nKey = `home.events.${eventPack}.parallax`
    const entries = tm(i18nKey) as Record<string, string> | undefined

    if (!entries) {
      return {}
    }

    return Object.fromEntries(
      Object.entries(entries).map(([section, path]) => [section, [path]])
    )
  }

  return computed<Record<ParallaxSectionKey, ParallaxLayerConfig[]>>(() => {
    const activePackName = unref(packName)
    const overrides = unref(dynamicOverrides)

    // 1. Static config resolution
    const packConfig = resolvePackForTheme(activePackName, themeName.value)
    const fallbackPack =
      activePackName === DEFAULT_EVENT_PACK
        ? packConfig
        : resolvePackForTheme(DEFAULT_EVENT_PACK, themeName.value)

    // 2. i18n resolution (takes precedence if present for specific sections)
    const i18nPack = resolveI18nPack(activePackName)

    return PARALLAX_SECTION_KEYS.reduce<
      Record<ParallaxSectionKey, ParallaxLayerConfig[]>
    >(
      (acc, section) => {
        const assets =
          overrides?.[section] ??
          i18nPack[section] ??
          packConfig[section] ??
          fallbackPack[section]

        return {
          ...acc,
          [section]: resolveParallaxLayers(assets, themeName.value),
        }
      },
      {} as Record<ParallaxSectionKey, ParallaxLayerConfig[]>
    )
  })
}
