import { computed } from 'vue'
import { PARALLAX_SECTION_KEYS, type ParallaxSectionKey } from '~~/config/theme/assets'

export type ParallaxSectionConfig = {
  overlay: number
  parallaxAmount: number
  ariaLabel: string
  maxOffsetRatio: number | null
}

type ParallaxSectionOverride = Partial<Omit<ParallaxSectionConfig, 'ariaLabel'>> & {
  ariaLabel?: string
}

export const useParallaxConfig = () => {
  const { t } = useI18n()
  const runtimeConfig = useRuntimeConfig()

  const defaults = computed<Record<ParallaxSectionKey, ParallaxSectionConfig>>(
    () => ({
      essentials: {
        overlay: 0.62,
        parallaxAmount: 0.16,
        ariaLabel: String(t('home.parallax.essentials.ariaLabel')),
        maxOffsetRatio: null,
      },
      features: {
        overlay: 0.55,
        parallaxAmount: 0.12,
        ariaLabel: String(t('home.parallax.features.ariaLabel')),
        maxOffsetRatio: null,
      },
      blog: {
        overlay: 0.5,
        parallaxAmount: 0.1,
        ariaLabel: String(t('home.parallax.knowledge.ariaLabel')),
        maxOffsetRatio: null,
      },
      objections: {
        overlay: 0.58,
        parallaxAmount: 0.11,
        ariaLabel: String(t('home.parallax.knowledge.ariaLabel')),
        maxOffsetRatio: null,
      },
      cta: {
        overlay: 0.48,
        parallaxAmount: 0.08,
        ariaLabel: String(t('home.parallax.cta.ariaLabel')),
        maxOffsetRatio: null,
      },
    })
  )

  const overrides = computed<
    Partial<Record<ParallaxSectionKey, ParallaxSectionOverride>>
  >(() => runtimeConfig.public?.parallaxSections ?? {})

  return computed<Record<ParallaxSectionKey, ParallaxSectionConfig>>(() =>
    PARALLAX_SECTION_KEYS.reduce<Record<ParallaxSectionKey, ParallaxSectionConfig>>(
      (acc, section) => ({
        ...acc,
        [section]: {
          ...defaults.value[section],
          ...overrides.value[section],
        },
      }),
      {} as Record<ParallaxSectionKey, ParallaxSectionConfig>
    )
  )
}
