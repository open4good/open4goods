<script setup lang="ts">
import { computed } from 'vue'
import { useEventPackI18n } from '~/composables/useEventPackI18n'
import { useSeasonalEventPack } from '~~/app/composables/useSeasonalEventPack'
import {
  DEFAULT_EVENT_PACK,
  EVENT_PACK_I18N_BASE_KEY,
} from '~~/config/theme/event-packs'
import RoundedCornerCard from '~/components/shared/cards/RoundedCornerCard.vue'
import SectionReveal from '~/components/shared/ui/SectionReveal.vue'

type HeroHighlightSegment = {
  text: string
  to?: string
}

type HeroHighlightItem = {
  title: string
  segments: HeroHighlightSegment[]
}

const props = defineProps<{
  partnersCount?: number
  openDataMillions?: number
  productsCount?: number
  categoriesCount?: number
}>()

const { t, te, tm, locale } = useI18n()
const activeEventPack = useSeasonalEventPack()
const packI18n = useEventPackI18n(activeEventPack)

const partnersLinkPlaceholder = '{partnersLink}'
const openDataMillionsPlaceholder = '{millions}'
const productsCountPlaceholder = '{products}'
const categoriesCountPlaceholder = '{categories}'

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

      const { text, to } = segment as { text?: unknown; to?: unknown }
      const normalizedText = typeof text === 'string' ? text.trim() : ''
      const normalizedTo =
        typeof to === 'string' && to.trim().length > 0 ? to.trim() : undefined

      if (!normalizedText) {
        return null
      }

      return {
        text: normalizedText,
        to: normalizedTo,
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

const heroHighlightItems = computed<HeroHighlightItem[]>(() => {
  const packItems = packI18n.resolveList('hero.highlights', {
    fallbackKeys: ['home.hero.highlights'],
  })

  const itemsToNormalize = Array.isArray(packItems)
    ? packItems
    : Object.values(packItems ?? {})

  const translatedItems = normalizeHighlightItems(itemsToNormalize)

  if (translatedItems.length > 0) {
    return applyOpenDataMillionsPlaceholder(
      applyProductsCategoriesPlaceholder(
        applyPartnerLinkPlaceholder(translatedItems)
      )
    )
  }

  const tmItems = tm('home.hero.highlights')
  if (Array.isArray(tmItems) && tmItems.length > 0) {
    const directTranslated = normalizeHighlightItems(tmItems)
    if (directTranslated.length > 0) {
      return applyOpenDataMillionsPlaceholder(
        applyProductsCategoriesPlaceholder(
          applyPartnerLinkPlaceholder(directTranslated)
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

const formattedOpenDataMillions = computed(() => {
  const value = props.openDataMillions

  if (typeof value !== 'number' || !Number.isFinite(value) || value <= 0) {
    return null
  }

  try {
    return new Intl.NumberFormat(locale.value).format(value)
  } catch {
    return String(value)
  }
})

const formattedProductsCount = computed(() => {
  const value = props.productsCount

  if (typeof value !== 'number' || !Number.isFinite(value) || value <= 0) {
    return null
  }

  try {
    return new Intl.NumberFormat(locale.value).format(value)
  } catch {
    return String(value)
  }
})

const formattedCategoriesCount = computed(() => {
  const value = props.categoriesCount

  if (typeof value !== 'number' || !Number.isFinite(value) || value <= 0) {
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
</script>

<template>
  <div class="home-hero-highlights" role="list">
    <v-row align="stretch">
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
            :selectable="false"
            :elevation="10"
            :hover-elevation="14"
          >
            <div class="home-hero-highlights__card-content" role="listitem">
              <p class="home-hero-highlights__title">{{ item.title }}</p>
              <p class="home-hero-highlights__description">
                <template
                  v-for="(segment, segmentIndex) in item.segments"
                  :key="`hero-highlight-${index}-segment-${segmentIndex}`"
                >
                  <NuxtLink
                    v-if="segment.to"
                    class="home-hero-highlights__link"
                    :to="segment.to"
                  >
                    {{ segmentIndex > 0 ? ` ${segment.text}` : segment.text }}
                  </NuxtLink>
                  <span v-else>
                    {{ segmentIndex > 0 ? ` ${segment.text}` : segment.text }}
                  </span>
                </template>
              </p>
            </div>
          </RoundedCornerCard>
        </SectionReveal>
      </v-col>
    </v-row>
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

  :deep(.rounded-card__content)
    min-height: auto

.home-hero-highlights__card-content
  display: flex
  flex-direction: column
  gap: 0.6rem

.home-hero-highlights__title
  margin: 0
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))
  font-size: 1rem

.home-hero-highlights__description
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  line-height: 1.35

.home-hero-highlights__link
  color: rgb(var(--v-theme-text-neutral-strong))
  font-weight: 600
  text-decoration: underline
  text-decoration-thickness: 0.08em
  text-underline-offset: 0.1em
</style>
