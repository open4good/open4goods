<template>
  <section
    v-if="hub"
    class="category-hub"
    :class="{ 'category-hub--preview': !hub.published }"
    :aria-labelledby="headingId"
  >
    <v-alert
      v-if="!hub.published"
      class="category-hub__preview mb-4"
      type="info"
      variant="tonal"
      density="compact"
    >
      {{ $t('category.hub.previewNotice') }}
    </v-alert>

    <h2 :id="headingId" class="category-hub__title">
      {{ title }}
    </h2>

    <!-- Rendered server-side by markdown-it with raw HTML disabled (server/utils/category-hub-content.ts). -->
    <!-- eslint-disable-next-line vue/no-v-html -->
    <div class="category-hub__intro" v-html="hub.introHtml" />

    <v-row class="category-hub__columns">
      <v-col v-if="topProducts.length" cols="12" md="6">
        <h3 class="category-hub__subtitle">
          {{ $t('category.hub.topTitle', { count: topProducts.length }) }}
        </h3>
        <ol class="category-hub__top">
          <li
            v-for="(product, index) in topProducts"
            :key="product.gtin ?? product.fullSlug ?? index"
            class="category-hub__top-item"
          >
            <span class="category-hub__top-rank" aria-hidden="true">
              {{ index + 1 }}
            </span>
            <NuxtLink
              v-if="product.fullSlug"
              :to="product.fullSlug"
              class="category-hub__top-name"
            >
              {{ productName(product) }}
            </NuxtLink>
            <span v-else class="category-hub__top-name">
              {{ productName(product) }}
            </span>
            <ImpactScore
              class="category-hub__top-score"
              :score="resolvePrimaryImpactScore(product) ?? 0"
              :max="20"
              size="xs"
              flat
              :show-methodology="false"
              :v2="resolveImpactScoreV2(product.scores)"
            />
          </li>
        </ol>
        <p v-if="rankingDate" class="category-hub__top-date">
          {{ $t('category.hub.rankingDate', { date: rankingDate }) }}
        </p>
      </v-col>

      <v-col cols="12" :md="topProducts.length ? 6 : 12">
        <h3 class="category-hub__subtitle">
          {{ $t('category.hub.faqTitle') }}
        </h3>
        <div
          v-for="item in hub.faq"
          :key="item.question"
          class="category-hub__faq-item"
        >
          <h4 class="category-hub__faq-question">{{ item.question }}</h4>
          <p class="category-hub__faq-answer">{{ item.answer }}</p>
        </div>
      </v-col>
    </v-row>

    <nav
      v-if="links.length"
      class="category-hub__links"
      :aria-label="$t('category.hub.linksTitle')"
    >
      <h3 class="category-hub__subtitle">
        {{ $t('category.hub.linksTitle') }}
      </h3>
      <ul class="category-hub__links-list">
        <li v-for="link in links" :key="link.to">
          <NuxtLink :to="link.to" class="category-hub__link">
            <v-icon :icon="link.icon" size="18" aria-hidden="true" />
            {{ link.label }}
          </NuxtLink>
        </li>
      </ul>
    </nav>
  </section>
</template>

<script setup lang="ts">
import { computed, useId } from 'vue'

import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import {
  isImpactScoreNotRated,
  resolveImpactScoreV2,
} from '~/utils/_impact-score-v2'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'
import { resolveProductShortName } from '~/utils/_product-title-resolver'
import type {
  ImpactScoreStatsDto,
  ProductDto,
  VerticalSubCategoryDto,
} from '~~/shared/api-client'
import {
  buildCategoryHubFaqJsonLd,
  type CategoryHubResponse,
} from '~~/shared/utils/category-hub'

/**
 * Editorial block of a category hub (GOU-2716): intro, visible FAQ with its
 * FAQPage JSON-LD, Top 5 ImpactScore dated with the last calculation of the
 * vertical, and links to its sub-categories and guides. Renders nothing when
 * the vertical has no published file in content/category-hubs/.
 */
const props = defineProps<{
  verticalId: string
  verticalHomeUrl?: string | null
  categoryName: string
  subCategories?: VerticalSubCategoryDto[]
  guides?: string[]
  /** Server-rendered listing, sorted by ImpactScore (CATEGORY_INITIAL_SORT), without filters. */
  rankedProducts?: ProductDto[]
}>()

const TOP_SIZE = 5

const { t, locale } = useI18n()
const requestFetch = useRequestFetch()
const headingId = useId()

const { data } = await useAsyncData(
  `category-hub-${props.verticalId}`,
  async () => {
    const { hub } = await requestFetch<CategoryHubResponse>(
      `/api/category-hubs/${encodeURIComponent(props.verticalId)}`
    )
    if (!hub) {
      return null
    }

    let lastScoredAt: number | null = null
    try {
      const stats = await requestFetch<ImpactScoreStatsDto>(
        `/api/categories/${encodeURIComponent(props.verticalId)}/impact-score-stats`
      )
      lastScoredAt = stats?.lastScoredAt ?? null
    } catch (error) {
      // The block stays useful without the date; the ranking is then shown undated.
      console.error(
        '[CategoryHubEditorial] Failed to load ImpactScore stats',
        error
      )
    }

    return { hub, lastScoredAt }
  },
  { server: true }
)

const hub = computed(() => data.value?.hub ?? null)

const title = computed(
  () =>
    hub.value?.title ||
    t('category.hub.defaultTitle', { category: props.categoryName })
)

const topProducts = computed(() =>
  (props.rankedProducts ?? [])
    .filter(
      product =>
        resolvePrimaryImpactScore(product) != null &&
        !isImpactScoreNotRated(resolveImpactScoreV2(product.scores))
    )
    .slice(0, TOP_SIZE)
)

// The date of the last real calculation, from front-api (max of impactScoreMeta.scoredAt).
// Formatted in UTC so that SSR and hydration render the same day.
const rankingDate = computed(() => {
  const at = data.value?.lastScoredAt
  if (!at) {
    return null
  }
  return new Intl.DateTimeFormat(locale.value, {
    dateStyle: 'long',
    timeZone: 'UTC',
  }).format(new Date(at))
})

const productName = (product: ProductDto) =>
  resolveProductShortName(product, locale.value) ||
  t('category.products.untitledProduct')

const basePath = computed(() => {
  const raw = props.verticalHomeUrl?.trim().replace(/^\/+|\/+$/g, '')
  return raw && !/^https?:/i.test(raw) ? `/${raw}` : null
})

// A declared link is rendered only when its target exists in the vertical.
const links = computed(() => {
  const base = basePath.value
  if (!hub.value || !base) {
    return []
  }

  const subCategoryLinks = hub.value.subCategories.flatMap(reference => {
    const subCategory = (props.subCategories ?? []).find(
      item => item.slug === reference || item.id === reference
    )
    const slug = subCategory?.slug?.trim()
    return slug
      ? [
          {
            to: `${base}/${slug}`,
            label: subCategory?.h1Title?.trim() || slug,
            icon: 'mdi-shape-outline',
          },
        ]
      : []
  })

  const knownGuides = new Set(props.guides ?? [])
  const guideLinks = hub.value.guides
    .filter(guide => knownGuides.has(guide.slug))
    .map(guide => ({
      to: `${base}/${guide.slug}`,
      label: guide.label,
      icon: 'mdi-book-open-page-variant-outline',
    }))

  return [...subCategoryLinks, ...guideLinks]
})

const faqJsonLd = computed(() =>
  hub.value ? buildCategoryHubFaqJsonLd(hub.value.faq) : null
)

useHead(() => ({
  script: faqJsonLd.value
    ? [
        {
          key: 'category-hub-faq-jsonld',
          type: 'application/ld+json',
          innerHTML: JSON.stringify(faqJsonLd.value),
        },
      ]
    : [],
}))
</script>

<style scoped lang="sass">
.category-hub
  margin-top: 2rem
  padding: 1.5rem
  border-radius: 24px
  background: rgb(var(--v-theme-surface-muted))
  color: rgb(var(--v-theme-text-neutral-strong))

.category-hub__title
  font-size: 1.5rem
  font-weight: 700
  margin-bottom: 1rem

.category-hub__subtitle
  font-size: 1.125rem
  font-weight: 700
  margin-bottom: 0.75rem

.category-hub__intro
  color: rgb(var(--v-theme-text-neutral-secondary))
  margin-bottom: 1rem

  :deep(p)
    margin-bottom: 0.75rem

.category-hub__top
  list-style: none
  padding: 0
  margin: 0

.category-hub__top-item
  display: flex
  align-items: center
  gap: 0.75rem
  padding: 0.5rem 0
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.5)

.category-hub__top-rank
  flex: 0 0 auto
  font-weight: 700
  min-width: 1.5rem
  color: rgb(var(--v-theme-text-neutral-soft))

.category-hub__top-name
  flex: 1 1 auto
  min-width: 0
  color: inherit
  font-weight: 600

.category-hub__top-score
  flex: 0 0 auto

.category-hub__top-date
  margin-top: 0.75rem
  font-size: 0.875rem
  color: rgb(var(--v-theme-text-neutral-soft))

.category-hub__faq-item
  margin-bottom: 1rem

.category-hub__faq-question
  font-size: 1rem
  font-weight: 700
  margin-bottom: 0.25rem

.category-hub__faq-answer
  color: rgb(var(--v-theme-text-neutral-secondary))
  margin: 0

.category-hub__link
  display: inline-flex
  align-items: center
  gap: 0.375rem
  padding: 0.375rem 0.875rem
  border-radius: 999px
  background: rgb(var(--v-theme-surface-primary-100))
  color: rgb(var(--v-theme-primary))
  font-weight: 600
  text-decoration: none

  &:hover,
  &:focus-visible
    background: rgb(var(--v-theme-surface-primary-120))

.category-hub__links-list
  display: flex
  flex-wrap: wrap
  gap: 0.5rem
  list-style: none
  padding: 0
  margin: 0
</style>
