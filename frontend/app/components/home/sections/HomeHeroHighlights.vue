<script setup lang="ts">
import { computed, ref } from 'vue'

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

const { t, locale } = useI18n()

const shouldRenderLinks = computed(() => props.enableLinks)
const scrollTargetId = computed(() => props.scrollTargetId?.trim() ?? '')
const isScrollEnabled = computed(() => Boolean(scrollTargetId.value))
const isHeroVariant = computed(() => props.variant === 'hero')

const heroHighlightItems = computed<HeroHighlightItem[]>(() => [
  {
    title: t('home.hero.highlights.impact.title'),
    segments: [{ text: t('home.hero.highlights.impact.subtitle') }],
  },
  {
    title: t('home.hero.highlights.price.title'),
    segments: [{ text: t('home.hero.highlights.price.subtitle') }],
  },
  {
    title: t('home.hero.highlights.ethics.title'),
    segments: [
      {
        text: t('home.hero.highlights.ethics.subtitle'),
        icon: 'breton-flag',
        iconPosition: 'after',
      },
    ],
  },
])

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
              <p class="home-hero-highlights__description ma-0">
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
