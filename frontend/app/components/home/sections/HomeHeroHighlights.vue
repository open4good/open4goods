<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import { useDailyRandomProducts } from '~/composables/useDailyRandomProducts'
import { useEventPackI18n } from '~/composables/useEventPackI18n'
import { useSeasonalEventPack } from '~~/app/composables/useSeasonalEventPack'
import { resolveProductShortName } from '~/utils/_product-title-resolver'
import {
  DEFAULT_EVENT_PACK,
  EVENT_PACK_I18N_BASE_KEY,
} from '~~/config/theme/event-packs'
import RoundedCornerCard from '~/components/shared/cards/RoundedCornerCard.vue'
import SectionReveal from '~/components/shared/ui/SectionReveal.vue'

type HeroHighlightSegment = {
  text: string
  to?: string
  icon?: string
  iconPosition?: 'before' | 'after'
}

type HeroHighlightItem = {
  title: string
  segments: HeroHighlightSegment[]
}

const props = withDefaults(
  defineProps<{
    partnersCount?: number
    openDataMillions?: number
    productsCount?: number
    categoriesCount?: number
    impactScoreProductsCount?: number
    impactScoreCategoriesCount?: number
    productsWithoutVerticalCount?: number
    reviewedProductsCount?: number
    aiSummaryRemainingCredits?: number
    heroBackgroundI18nKey?: string
    highlightsI18nKey?: string
    highlightsFallbackKeys?: string[]
    aiSummaryI18nKey?: string
    enableLinks?: boolean
    scrollTargetId?: string
    variant?: 'hero' | 'section'
  }>(),
  {
    partnersCount: undefined,
    openDataMillions: undefined,
    productsCount: undefined,
    categoriesCount: undefined,
    impactScoreProductsCount: undefined,
    impactScoreCategoriesCount: undefined,
    productsWithoutVerticalCount: undefined,
    reviewedProductsCount: undefined,
    aiSummaryRemainingCredits: undefined,
    heroBackgroundI18nKey: undefined,
    highlightsI18nKey: 'hero.highlights',
    highlightsFallbackKeys: undefined,
    aiSummaryI18nKey: 'home.hero.aiSummary',
    enableLinks: true,
    scrollTargetId: '',
    variant: 'section',
  }
)

const { t, te, tm, locale } = useI18n()
const activeEventPack = useSeasonalEventPack()
const packI18n = useEventPackI18n(activeEventPack)

const partnersLinkPlaceholder = '{partnersLink}'
const openDataMillionsPlaceholder = '{millions}'
const productsCountPlaceholder = '{products}'
const categoriesCountPlaceholder = '{categories}'
const impactScoreProductsPlaceholder = '{impactScoreProducts}'
const impactScoreCategoriesPlaceholder = '{impactScoreCategories}'
const productsWithoutVerticalPlaceholder = '{priceHistoryProducts}'

const normalizeHighlightSegments = (
  segments: unknown
): HeroHighlightSegment[] => {
  if (!Array.isArray(segments)) {
    return []
  }

  return segments
    .map(segment => {
      if (typeof segment !== 'object' || segment == null) {
        return null
      }

      const { text, to, icon, iconPosition } = segment as {
        text?: unknown
        to?: unknown
        icon?: unknown
        iconPosition?: unknown
      }
      const normalizedText = typeof text === 'string' ? text.trim() : ''
      const normalizedTo =
        typeof to === 'string' && to.trim().length > 0 ? to.trim() : undefined
      const normalizedIcon =
        typeof icon === 'string' && icon.trim().length > 0
          ? icon.trim()
          : undefined
      const normalizedIconPosition =
        iconPosition === 'after' || iconPosition === 'before'
          ? iconPosition
          : undefined

      if (!normalizedText) {
        return null
      }

      return {
        text: normalizedText,
        to: normalizedTo,
        icon: normalizedIcon,
        iconPosition: normalizedIconPosition,
      }
    })
    .filter((segment): segment is HeroHighlightSegment => segment != null)
}

const normalizeHighlightItems = (items: unknown): HeroHighlightItem[] => {
  if (!Array.isArray(items)) {
    return []
  }

  return items
    .map(rawItem => {
      if (typeof rawItem !== 'object' || rawItem == null) {
        return null
      }

      const { title, segments, descriptionParts } = rawItem as {
        title?: unknown
        segments?: unknown
        descriptionParts?: unknown
      }

      const normalizedTitle = typeof title === 'string' ? title.trim() : ''
      const normalizedSegments = normalizeHighlightSegments(
        segments ?? descriptionParts
      )

      if (!normalizedTitle || normalizedSegments.length === 0) {
        return null
      }

      return {
        title: normalizedTitle,
        segments: normalizedSegments,
      }
    })
    .filter((item): item is HeroHighlightItem => item != null)
}

const resolvedHighlightsKey = computed(
  () => props.highlightsI18nKey?.trim() || 'hero.highlights'
)
const resolvedHighlightsFallbackKeys = computed(() => {
  if (props.highlightsFallbackKeys?.length) {
    return props.highlightsFallbackKeys
  }

  return [`home.${resolvedHighlightsKey.value}`]
})
const shouldRenderLinks = computed(() => props.enableLinks)
const scrollTargetId = computed(() => props.scrollTargetId?.trim() ?? '')
const isScrollEnabled = computed(() => Boolean(scrollTargetId.value))
const isHeroVariant = computed(() => props.variant === 'hero')

const heroHighlightItems = computed<HeroHighlightItem[]>(() => {
  const packItems = packI18n.resolveList(resolvedHighlightsKey.value, {
    fallbackKeys: resolvedHighlightsFallbackKeys.value,
  })

  const itemsToNormalize = Array.isArray(packItems)
    ? packItems
    : Object.values(packItems ?? {})

  const translatedItems = normalizeHighlightItems(itemsToNormalize)

  if (translatedItems.length > 0) {
    return applyRandomProductPlaceholder(
      applyProductsWithoutVerticalPlaceholder(
        applyImpactScorePlaceholders(
          applyOpenDataMillionsPlaceholder(
            applyProductsCategoriesPlaceholder(
              applyPartnerLinkPlaceholder(translatedItems)
            )
          )
        )
      )
    )
  }

  const tmItems = tm('home.hero.highlights')
  if (Array.isArray(tmItems) && tmItems.length > 0) {
    const directTranslated = normalizeHighlightItems(tmItems)
    if (directTranslated.length > 0) {
      return applyRandomProductPlaceholder(
        applyProductsWithoutVerticalPlaceholder(
          applyImpactScorePlaceholders(
            applyOpenDataMillionsPlaceholder(
              applyProductsCategoriesPlaceholder(
                applyPartnerLinkPlaceholder(directTranslated)
              )
            )
          )
        )
      )
    }
  }

  return []
})

const normalizedPartnersCount = computed(() => {
  const rawCount = Number(props.partnersCount ?? 0)

  if (!Number.isFinite(rawCount)) {
    return 0
  }

  return Math.max(0, Math.round(rawCount))
})

const formattedPartnersCount = computed(() => {
  const count = normalizedPartnersCount.value

  if (!count) {
    return null
  }

  try {
    return new Intl.NumberFormat(locale.value).format(count)
  } catch {
    return String(count)
  }
})

const heroPartnersLinkText = computed(() => {
  const count = normalizedPartnersCount.value
  const formattedCount = formattedPartnersCount.value
  const fallbackLabel =
    packI18n
      .resolveString('hero.search.partnerLinkFallback', {
        fallbackKeys: ['home.hero.search.partnerLinkFallback'],
      })
      ?.trim() ?? ''

  if (!count || !formattedCount) {
    return fallbackLabel || null
  }

  const translateKey = (key: string) => {
    if (!te(key)) {
      return ''
    }

    const translated = t(key, { count, formattedCount })
    const normalized =
      typeof translated === 'string'
        ? translated.trim()
        : String(translated ?? '').trim()

    return normalized && normalized !== key ? normalized : ''
  }

  const packKey = `${EVENT_PACK_I18N_BASE_KEY}.${activeEventPack.value}.hero.search.partnerLinkLabel`
  const defaultKey = `${EVENT_PACK_I18N_BASE_KEY}.${DEFAULT_EVENT_PACK}.hero.search.partnerLinkLabel`

  const translated =
    translateKey(packKey) ||
    translateKey(defaultKey) ||
    translateKey('home.hero.search.partnerLinkLabel')

  const normalizedTranslation =
    typeof translated === 'string'
      ? translated.trim()
      : String(translated ?? '').trim()

  if (
    normalizedTranslation &&
    normalizedTranslation !== 'home.hero.search.partnerLinkLabel'
  ) {
    return normalizedTranslation
  }

  if (!fallbackLabel) {
    return formattedCount
  }

  return `${formattedCount} ${fallbackLabel}`
})

const formatCount = (value?: number) => {
  if (typeof value !== 'number' || !Number.isFinite(value) || value < 0) {
    return null
  }

  try {
    return new Intl.NumberFormat(locale.value).format(value)
  } catch {
    return String(value)
  }
}

const formatCompactCount = (value?: number) => {
  if (typeof value !== 'number' || !Number.isFinite(value) || value < 0) {
    return null
  }

  if (value >= 1_000_000) {
    const thousands = Math.floor(value / 1_000_000)
    return `${thousands}k`
  }

  if (value >= 1000) {
    const thousands = Math.floor(value / 1000)
    return `${thousands}k`
  }

  return formatCount(value)
}

const formattedOpenDataMillions = computed(() =>
  formatCount(props.openDataMillions)
)

const formattedProductsCount = computed(() =>
  formatCompactCount(props.productsCount)
)

const formattedCategoriesCount = computed(() =>
  formatCount(props.categoriesCount)
)

const formattedImpactScoreProductsCount = computed(() =>
  formatCount(props.impactScoreProductsCount)
)

const formattedImpactScoreCategoriesCount = computed(() =>
  formatCount(props.impactScoreCategoriesCount ?? props.categoriesCount)
)

const formattedProductsWithoutVerticalCount = computed(() =>
  formatCompactCount(props.productsWithoutVerticalCount)
)

const formattedReviewedProductsCount = computed(() =>
  formatCount(props.reviewedProductsCount)
)

const formattedAiSummaryRemainingCredits = computed(() => {
  const value = props.aiSummaryRemainingCredits

  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return null
  }

  try {
    return new Intl.NumberFormat(locale.value).format(value)
  } catch {
    return String(value)
  }
})

const applyPartnerLinkPlaceholder = (items: HeroHighlightItem[]) => {
  const partnerLinkText = heroPartnersLinkText.value

  if (!partnerLinkText) {
    return items
  }

  return items.map(item => {
    const segmentsWithPartnerLink = item.segments
      .map(segment => {
        if (!segment.text.includes(partnersLinkPlaceholder)) {
          return segment
        }

        const replacedText = segment.text.replaceAll(
          partnersLinkPlaceholder,
          partnerLinkText
        )

        const normalizedText = replacedText.trim()

        if (!normalizedText) {
          return null
        }

        return {
          ...segment,
          text: normalizedText,
        }
      })
      .filter((segment): segment is HeroHighlightSegment => segment != null)

    return {
      ...item,
      segments:
        segmentsWithPartnerLink.length > 0
          ? segmentsWithPartnerLink
          : item.segments,
    }
  })
}

const applyOpenDataMillionsPlaceholder = (items: HeroHighlightItem[]) => {
  const millionsLabel = formattedOpenDataMillions.value

  if (!millionsLabel) {
    return items
  }

  return items.map(item => {
    const segmentsWithOpenDataMillions = item.segments
      .map(segment => {
        if (!segment.text.includes(openDataMillionsPlaceholder)) {
          return segment
        }

        const replacedText = segment.text.replaceAll(
          openDataMillionsPlaceholder,
          millionsLabel
        )

        const normalizedText = replacedText.trim()

        if (!normalizedText) {
          return null
        }

        return {
          ...segment,
          text: normalizedText,
        }
      })
      .filter((segment): segment is HeroHighlightSegment => segment != null)

    return {
      ...item,
      segments:
        segmentsWithOpenDataMillions.length > 0
          ? segmentsWithOpenDataMillions
          : item.segments,
    }
  })
}

const applyProductsCategoriesPlaceholder = (items: HeroHighlightItem[]) => {
  const productsLabel = formattedProductsCount.value
  const categoriesLabel = formattedCategoriesCount.value

  if (!productsLabel || !categoriesLabel) {
    return items
  }

  return items.map(item => {
    const segmentsWithCounts = item.segments
      .map(segment => {
        if (
          !segment.text.includes(productsCountPlaceholder) &&
          !segment.text.includes(categoriesCountPlaceholder)
        ) {
          return segment
        }

        const replacedText = segment.text
          .replaceAll(productsCountPlaceholder, productsLabel)
          .replaceAll(categoriesCountPlaceholder, categoriesLabel)

        const normalizedText = replacedText.trim()

        if (!normalizedText) {
          return null
        }

        return {
          ...segment,
          text: normalizedText,
        }
      })
      .filter((segment): segment is HeroHighlightSegment => segment != null)

    return {
      ...item,
      segments:
        segmentsWithCounts.length > 0 ? segmentsWithCounts : item.segments,
    }
  })
}

const applyImpactScorePlaceholders = (items: HeroHighlightItem[]) => {
  const productsLabel = formattedImpactScoreProductsCount.value
  const categoriesLabel = formattedImpactScoreCategoriesCount.value

  if (!productsLabel || !categoriesLabel) {
    return items
  }

  return items.map(item => {
    const segmentsWithCounts = item.segments
      .map(segment => {
        if (
          !segment.text.includes(impactScoreProductsPlaceholder) &&
          !segment.text.includes(impactScoreCategoriesPlaceholder)
        ) {
          return segment
        }

        const replacedText = segment.text
          .replaceAll(impactScoreProductsPlaceholder, productsLabel)
          .replaceAll(impactScoreCategoriesPlaceholder, categoriesLabel)

        const normalizedText = replacedText.trim()

        if (!normalizedText) {
          return null
        }

        return {
          ...segment,
          text: normalizedText,
        }
      })
      .filter((segment): segment is HeroHighlightSegment => segment != null)

    return {
      ...item,
      segments:
        segmentsWithCounts.length > 0 ? segmentsWithCounts : item.segments,
    }
  })
}

const applyProductsWithoutVerticalPlaceholder = (
  items: HeroHighlightItem[]
) => {
  const productsLabel = formattedProductsWithoutVerticalCount.value

  if (!productsLabel) {
    return items
  }

  return items.map(item => {
    const segmentsWithCounts = item.segments
      .map(segment => {
        if (!segment.text.includes(productsWithoutVerticalPlaceholder)) {
          return segment
        }

        const replacedText = segment.text.replaceAll(
          productsWithoutVerticalPlaceholder,
          productsLabel
        )

        const normalizedText = replacedText.trim()

        if (!normalizedText) {
          return null
        }

        return {
          ...segment,
          text: normalizedText,
        }
      })
      .filter((segment): segment is HeroHighlightSegment => segment != null)

    return {
      ...item,
      segments:
        segmentsWithCounts.length > 0 ? segmentsWithCounts : item.segments,
    }
  })
}

// Fetch random product
// Fetch random product
const { initDailyProducts, getRandomProduct } = useDailyRandomProducts()
const currentRandomProduct = ref<ProductDto | null>(null)

// Initialize on mount
onMounted(async () => {
  await initDailyProducts()
  currentRandomProduct.value = getRandomProduct()
})

const randomProductLink = computed(() => {
  const product = currentRandomProduct.value
  return product?.names?.slug ? `/p/${product.names.slug}` : null
})

const randomProductName = computed(() => {
  const product = currentRandomProduct.value
  if (!product) return null
  return resolveProductShortName(product, locale.value)
})

const applyRandomProductPlaceholder = (items: HeroHighlightItem[]) => {
  const productName = randomProductName.value
  const productLink = randomProductLink.value
  const placeholder = '{randomProduct}'

  if (!productName || !productLink) {
    // If no random product, remove the placeholder and potential intro text
    return items.map(item => {
      const segmentsWithCleanup = item.segments
        .map(segment => {
          if (!segment.text.includes(placeholder)) {
            return segment
          }

          // Try to remove "Un exemple ? {randomProduct}" or just "{randomProduct}"
          // We use a regex to match the placeholder and optional preceding text "Un exemple ?"
          // The regex handles potential spacing nuances.
          const cleanedText = segment.text
            .replace(/Un exemple\s*\?*\s*\{randomProduct\}/gi, '')
            .replace(placeholder, '')
            .trim()

          if (!cleanedText) {
            return null
          }

          return {
            ...segment,
            text: cleanedText,
          }
        })
        .filter((segment): segment is HeroHighlightSegment => segment != null)

      return {
        ...item,
        segments:
          segmentsWithCleanup.length > 0 ? segmentsWithCleanup : item.segments,
      }
    })
  }

  return items.map(item => {
    const segmentsWithProduct = item.segments.flatMap(
      (segment: HeroHighlightSegment): HeroHighlightSegment[] => {
        // Handle {randomProductLink} in existing links
        const to =
          segment.to && segment.to.includes('{randomProductLink}')
            ? productLink
            : segment.to

        if (!segment.text.includes(placeholder)) {
          return [{ ...segment, to }]
        }

        // Split segment text around the placeholder to create a proper link
        const parts = segment.text.split(placeholder)
        const result: HeroHighlightSegment[] = []

        parts.forEach((part: string, index: number) => {
          // Add text before placeholder (if any)
          if (part) {
            result.push({ text: part, to: segment.to ? to : undefined })
          }

          // Add the product link segment (except after the last part)
          if (index < parts.length - 1) {
            result.push({ text: productName, to: productLink })
          }
        })

        return result
      }
    )

    return {
      ...item,
      segments: segmentsWithProduct,
    }
  })
}

const resolveSegmentIcon = (icon?: string) => {
  if (!icon) {
    return null
  }

  switch (icon) {
    case 'breton-flag':
      return '/images/icons/breton-flag.svg'
    default:
      return null
  }
}

const aiSummaryReviewedLabel = computed(() => {
  if (!props.reviewedProductsCount) {
    return null
  }

  const reviewedProductsCount = formattedReviewedProductsCount.value

  if (reviewedProductsCount == null) {
    return null
  }

  return t(`${props.aiSummaryI18nKey}.reviewedProductsSuffix`, {
    reviewedProductsCount,
  })
})
const aiSummaryCreditsLabel = computed(() => {
  const remaining = formattedAiSummaryRemainingCredits.value
  if (!remaining) {
    return t(`${props.aiSummaryI18nKey}.creditsFallback`)
  }

  return t(`${props.aiSummaryI18nKey}.creditsLabel`, { count: remaining })
})

const isCreditsDialogActive = ref(false)
const aiSummaryTitle = computed(() => t(`${props.aiSummaryI18nKey}.title`))
const aiSummaryDescription = computed(() =>
  t(`${props.aiSummaryI18nKey}.description`)
)
const scrollCtaLabel = (title: string) =>
  t('home.hero.highlightsScrollCta', { title })

const handleHighlightClick = () => {
  if (!isScrollEnabled.value || !import.meta.client) {
    return
  }

  const target = document.getElementById(scrollTargetId.value)
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const resolveHighlightStyle = (index: number) => {
  if (!isHeroVariant.value) {
    return undefined
  }

  const tilt = index % 2 === 0 ? '-1.6deg' : '1.3deg'
  const offset = index % 2 === 0 ? '-10px' : '10px'

  return {
    '--highlight-tilt': tilt,
    '--highlight-offset': offset,
  } as Record<string, string>
}
</script>

<template>
  <div
    class="home-hero-highlights"
    :class="{
      'home-hero-highlights--hero': isHeroVariant,
      'home-hero-highlights--section': !isHeroVariant,
    }"
    role="list"
  >
    <v-row align="stretch" class="mb-10" justify="center">
      <v-col
        v-for="(item, index) in heroHighlightItems"
        :key="`hero-highlight-${index}`"
        cols="12"
        md="4"
        class="home-hero-highlights__col"
      >
        <SectionReveal
          class="home-hero-highlights__reveal"
          transition="fade"
          :style="{ transitionDelay: `${index * 150}ms` }"
        >
          <RoundedCornerCard
            class="home-hero-highlights__card"
            surface="strong"
            accent-corner="bottom-right"
            corner-variant="none"
            corner-size="lg"
            rounded="lg"
            :selectable="isScrollEnabled"
            :elevation="10"
            :hover-elevation="14"
            :aria-label="
              isScrollEnabled ? scrollCtaLabel(item.title) : undefined
            "
            :style="resolveHighlightStyle(index)"
            @click="handleHighlightClick"
          >
            <div
              class="home-hero-highlights__card-content d-flex flex-column align-center text-center ga-2"
              role="listitem"
            >
              <p class="home-hero-highlights__title ma-0 font-weight-bold">
                {{ item.title }}
              </p>
              <p
                class="home-hero-highlights__description ma-0"
                :class="{
                  'home-hero-highlights__description--compact': isHeroVariant,
                }"
              >
                <template
                  v-for="(segment, segmentIndex) in item.segments"
                  :key="`hero-highlight-${index}-segment-${segmentIndex}`"
                >
                  <NuxtLink
                    v-if="segment.to && shouldRenderLinks"
                    class="home-hero-highlights__link"
                    :to="segment.to"
                  >
                    <span
                      v-if="
                        segment.icon &&
                        resolveSegmentIcon(segment.icon) &&
                        segment.iconPosition !== 'after'
                      "
                      class="home-hero-highlights__icon"
                    >
                      <img
                        :src="resolveSegmentIcon(segment.icon) ?? ''"
                        alt=""
                        aria-hidden="true"
                      />
                    </span>
                    {{
                      segmentIndex > 0 && !/^[.,;!?)]/.test(segment.text)
                        ? ` ${segment.text}`
                        : segment.text
                    }}
                    <span
                      v-if="
                        segment.icon &&
                        resolveSegmentIcon(segment.icon) &&
                        segment.iconPosition === 'after'
                      "
                      class="home-hero-highlights__icon"
                    >
                      <img
                        :src="resolveSegmentIcon(segment.icon) ?? ''"
                        alt=""
                        aria-hidden="true"
                      />
                    </span>
                  </NuxtLink>
                  <span v-else>
                    <span
                      v-if="
                        shouldRenderLinks &&
                        segment.icon &&
                        resolveSegmentIcon(segment.icon) &&
                        segment.iconPosition !== 'after'
                      "
                      class="home-hero-highlights__icon"
                    >
                      <img
                        :src="resolveSegmentIcon(segment.icon) ?? ''"
                        alt=""
                        aria-hidden="true"
                      />
                    </span>
                    {{
                      segmentIndex > 0 && !/^[.,;!?)]/.test(segment.text)
                        ? ` ${segment.text}`
                        : segment.text
                    }}
                    <span
                      v-if="
                        shouldRenderLinks &&
                        segment.icon &&
                        resolveSegmentIcon(segment.icon) &&
                        segment.iconPosition === 'after'
                      "
                      class="home-hero-highlights__icon"
                    >
                      <img
                        :src="resolveSegmentIcon(segment.icon) ?? ''"
                        alt=""
                        aria-hidden="true"
                      />
                    </span>
                  </span>
                </template>
              </p>
            </div>
          </RoundedCornerCard>
        </SectionReveal>
      </v-col>
    </v-row>
    <SectionReveal class="home-hero-highlights__ai-summary" transition="fade">
      <RoundedCornerCard
        class="mt-8 home-hero-highlights__ai-summary-card"
        surface="strong"
        accent-corner="bottom-right"
        corner-variant="none"
        corner-size="lg"
        rounded="lg"
        :selectable="false"
        :elevation="10"
        :hover-elevation="14"
      >
        <div class="home-hero-highlights__ai-summary-content pa-4">
          <v-row align="center">
            <v-col cols="12" md="2" class="d-flex justify-center">
              <v-icon size="56" color="secondary">mdi-robot</v-icon>
            </v-col>
            <v-col
              cols="12"
              md="10"
              class="d-flex flex-column ga-1 text-center"
            >
              <div
                class="d-flex flex-wrap justify-center ga-2 font-weight-bold"
              >
                <span class="home-hero-highlights__ai-summary-title">
                  <v-icon color="secondary" class="mr-2"
                    >mdi-bullhorn-variant</v-icon
                  >
                  {{ aiSummaryTitle }}
                </span>
              </div>
              <p
                class="home-hero-highlights__ai-summary-description ma-0"
                :class="{
                  'home-hero-highlights__ai-summary-description--compact':
                    isHeroVariant,
                }"
              >
                {{ aiSummaryDescription }}
              </p>

              <div
                class="d-flex flex-wrap align-center justify-center ga-4 mt-4"
              >
                <div
                  v-if="aiSummaryReviewedLabel"
                  class="d-inline-flex align-center px-3 py-1 rounded-pill home-hero-highlights__ai-summary-secondary-info"
                >
                  <v-icon start size="small" color="secondary"
                    >mdi-information-outline</v-icon
                  >
                  {{ aiSummaryReviewedLabel }}
                </div>

                <v-btn
                  color="secondary"
                  variant="flat"
                  rounded="pill"
                  prepend-icon="mdi-bullhorn-variant"
                  @click="isCreditsDialogActive = true"
                >
                  {{ aiSummaryCreditsLabel }}
                </v-btn>
              </div>
            </v-col>
          </v-row>
        </div>
      </RoundedCornerCard>
    </SectionReveal>

    <v-dialog v-model="isCreditsDialogActive" max-width="600">
      <v-card class="pa-4" rounded="xl">
        <v-card-text class="text-center pa-6">
          <div class="d-flex justify-center gap-6 mb-8">
            <v-icon size="56" color="primary">mdi-robot</v-icon>
            <v-icon size="56" color="secondary">mdi-creation</v-icon>
            <v-icon size="56" color="accent">mdi-sparkles</v-icon>
          </div>
          <p class="text-h6 font-weight-medium mb-2">
            {{ t('home.hero.aiSummary.dialog.description') }}
          </p>
        </v-card-text>
        <v-card-actions class="justify-center pt-0 pb-4">
          <v-btn
            color="primary"
            variant="flat"
            size="large"
            @click="isCreditsDialogActive = false"
          >
            {{ t('home.hero.aiSummary.dialog.confirm') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped lang="sass">


.home-hero-highlights__reveal
  width: 100%
  height: 100%
  display: flex

  /* Target component root (transition) and inner content */
  :deep(> *)
    width: 100%
    height: 100%
    display: flex
    flex-direction: column
    flex: 1

    /* Target the inner div containing the slot */
    > div
      width: 100%
      height: 100%
      display: flex
      flex-direction: column
      flex: 1


.home-hero-highlights__col
  display: flex

.home-hero-highlights__card
  height: 100%
  width: 100%
  transform: translateY(var(--highlight-offset, 0)) rotate(var(--highlight-tilt, 0deg))
  transition: transform 180ms ease, box-shadow 180ms ease

  :deep(.rounded-card__content)
    min-height: auto

// .home-hero-highlights__card-content styles now handled by utility classes: d-flex flex-column align-center text-center ga-2

.home-hero-highlights__title
  // margin, font-weight now handled by utility classes: ma-0 font-weight-bold
  color: rgb(var(--v-theme-text-neutral-strong))
  font-size: 1rem

.home-hero-highlights__description
  // margin now handled by utility class: ma-0
  color: rgb(var(--v-theme-text-neutral-secondary))
  line-height: 1.35

.home-hero-highlights__description--compact
  display: -webkit-box
  -webkit-line-clamp: 1
  -webkit-box-orient: vertical
  overflow: hidden

.home-hero-highlights__link
  color: rgb(var(--v-theme-text-neutral-strong))
  font-weight: 600
  text-decoration: underline
  text-decoration-thickness: 0.08em
  text-underline-offset: 0.1em

.home-hero-highlights__icon
  display: inline-flex
  align-items: center
  margin-inline: 0.15rem
  vertical-align: text-bottom

  img
    width: 1rem
    height: 1rem
    display: inline-block
    vertical-align: middle

.home-hero-highlights__ai-summary
  margin-top: 1.25rem

.home-hero-highlights--section
  margin-top: clamp(1rem, 3vw, 1.5rem)

.home-hero-highlights--hero
  .home-hero-highlights__ai-summary
    margin-top: clamp(1rem, 4vw, 1.5rem)

@media (min-width: 960px)
  .home-hero-highlights--hero
    .home-hero-highlights__col:nth-child(odd) .home-hero-highlights__card
      --highlight-offset: -12px

    .home-hero-highlights__col:nth-child(even) .home-hero-highlights__card
      --highlight-offset: 12px

.home-hero-highlights__ai-summary-cards-container
  width: 100%

.home-hero-highlights__ai-summary-card
  width: 100%
  /* Styles are now handled by RoundedCornerCard props */

.home-hero-highlights__ai-summary-content
  padding: 0.9rem 1.1rem
  row-gap: 0.35rem

// .home-hero-highlights__ai-summary-icon styles now handled by utility classes: d-flex justify-center

// .home-hero-highlights__ai-summary-header styles now handled by utility classes: d-flex flex-wrap justify-center ga-2 font-weight-bold

.home-hero-highlights__ai-summary-title
  font-size: 0.95rem

// .home-hero-highlights__ai-summary-text styles now handled by utility classes: d-flex flex-column ga-1 text-center

.home-hero-highlights__ai-summary-description
  // margin now handled by utility class: ma-0
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-size: 0.85rem
  line-height: 1.4

.home-hero-highlights__ai-summary-description--compact
  display: -webkit-box
  -webkit-line-clamp: 1
  -webkit-box-orient: vertical
  overflow: hidden

// .home-hero-highlights__ai-summary-actions styles now handled by utility classes: d-flex flex-wrap align-center justify-center ga-4 mt-4

.home-hero-highlights__ai-summary-secondary-info
  // d-inline-flex align-center px-3 py-1 rounded-pill now handled by utility classes
  background: rgba(var(--v-theme-surface-default), 0.6)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3)
  font-size: 0.8rem
  color: rgb(var(--v-theme-text-neutral-secondary))
  backdrop-filter: blur(4px)
</style>
