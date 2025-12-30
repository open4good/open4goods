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
import {
  EVENT_PACK_I18N_BASE_KEY,
  type EventPackName,
} from '~~/config/theme/event-packs'
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
    const i18nKey = `${EVENT_PACK_I18N_BASE_KEY}.${eventPack}.parallax`
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

    // i18n resolution
    const i18nPack = resolveI18nPack(activePackName)

    return PARALLAX_SECTION_KEYS.reduce<
      Record<ParallaxSectionKey, ParallaxLayerConfig[]>
    >(
      (acc, section) => {
        const assets = overrides?.[section] ?? i18nPack[section] ?? []

        return {
          ...acc,
          [section]: resolveParallaxLayers(assets, themeName.value),
        }
      },
      {} as Record<ParallaxSectionKey, ParallaxLayerConfig[]>
    )
  })
}
