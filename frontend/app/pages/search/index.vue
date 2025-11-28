<template>
  <div class="search-page">
    <section class="search-hero" aria-labelledby="search-hero-heading">
      <v-container class="search-hero__container py-0 px-4 mx-auto" max-width="xl">
        <div class="search-hero__content">
          <p class="search-hero__eyebrow">{{ t('search.hero.eyebrow') }}</p>
          <div class="search-hero__copy">
            <h1 id="search-hero-heading" class="search-hero__title">
              {{ t('search.hero.title') }}
            </h1>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <p class="search-hero__subtitle subtitle-text" v-html="t('search.hero.subtitle')" />
          </div>

          <form class="search-hero__form" @submit.prevent="handleSearchSubmit">
            <SearchSuggestField
              v-model="searchInput"
              class="search-hero__field"
              :label="t('search.form.label')"
              :placeholder="t('search.form.placeholder')"
              :aria-label="t('search.form.ariaLabel')"
              :min-chars="MIN_QUERY_LENGTH"
              @clear="handleClear"
              @select-category="handleCategorySuggestion"
              @select-product="handleProductSuggestion"
              @submit="handleSearchSubmit"
            />
            <v-btn class="search-hero__submit" type="submit" size="large">
              {{ t('search.form.submit') }}
            </v-btn>
          </form>

          <p v-if="showInitialState" class="search-hero__helper">
            {{ t('search.states.initial') }}
          </p>
          <p v-else-if="showMinimumNotice" class="search-hero__helper search-hero__helper--warning">
            {{ t('search.states.minimum', { min: MIN_QUERY_LENGTH }) }}
          </p>
        </div>
      </v-container>
    </section>

    <v-progress-linear
      v-if="pending"
      class="search-page__loader"
      indeterminate
      color="primary"
      :aria-label="t('search.states.loadingAria')"
      role="progressbar"
    />

    <v-container v-if="hasMinimumLength" class="search-page__results py-10 px-4 mx-auto" max-width="xl">
      <v-alert
        v-if="error"
        type="error"
        variant="tonal"
        border="start"
        prominent
        class="mb-6"
        role="alert"
      >
        <div class="search-page__alert-content">
          <p class="search-page__alert-title">{{ t('search.states.error.title') }}</p>
          <p class="search-page__alert-description">{{ t('search.states.error.description') }}</p>
        </div>
        <template #append>
          <v-btn color="primary" variant="text" @click="refresh">{{ t('common.actions.retry') }}</v-btn>
        </template>
      </v-alert>

      <template v-else>
        <div v-if="!displayGroups.length && !pending" class="search-page__empty">
          <h2 class="search-page__empty-title">
            {{ t('search.states.empty.title', { query: normalizedQuery }) }}
          </h2>
          <p class="search-page__empty-description">
            {{ t('search.states.empty.description') }}
          </p>
        </div>

        <div v-else class="search-page__group-wrapper">
          <p class="search-page__summary">
            {{ resultsSummaryLabel }}
          </p>
          <p v-if="usingFallback" class="search-page__fallback">
            {{ t('search.notice.fallback') }}
          </p>

          <SearchResultGroup
            v-for="group in displayGroups"
            :key="group.key"
            :title="group.title"
            :count-label="group.countLabel"
            :products="group.products"
            :popular-attributes="group.popularAttributes"
            :vertical-home-url="group.verticalHomeUrl"
            :category-link-label="t('search.groups.viewCategory')"
            :category-link-aria="t('search.groups.viewCategoryAria', { title: group.title })"
          />
        </div>
      </template>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  AttributeConfigDto,
  GlobalSearchResponseDto,
  ProductDto,
  VerticalConfigDto,
} from '~~/shared/api-client'
import SearchSuggestField, {
  type CategorySuggestionItem,
  type ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'
import SearchResultGroup from '~/components/search/SearchResultGroup.vue'
import { usePluralizedTranslation } from '~/composables/usePluralizedTranslation'
import { useAnalytics } from '~/composables/useAnalytics'

const MIN_QUERY_LENGTH = 2

definePageMeta({
  ssr: true,
})

const { t, locale, availableLocales } = useI18n()
const { translatePlural } = usePluralizedTranslation()
const { trackSearch } = useAnalytics()
const route = useRoute()
const router = useRouter()
const localePath = useLocalePath()
const requestURL = useRequestURL()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const routeQuery = computed(() =>
  typeof route.query.q === 'string' ? route.query.q : '',
)
const searchInput = ref(routeQuery.value)

watch(routeQuery, (value) => {
  searchInput.value = value
})

const normalizedQuery = computed(() => routeQuery.value.trim())
const trimmedInput = computed(() => searchInput.value.trim())
const hasMinimumLength = computed(
  () => normalizedQuery.value.length >= MIN_QUERY_LENGTH,
)
const showInitialState = computed(
  () => trimmedInput.value.length === 0 && normalizedQuery.value.length === 0,
)
const showMinimumNotice = computed(
  () => trimmedInput.value.length > 0 && trimmedInput.value.length < MIN_QUERY_LENGTH,
)

const { data, pending, error, refresh } = await useAsyncData<
  GlobalSearchResponseDto | null
>(
  'global-search',
  async () => {
    if (!hasMinimumLength.value) {
      return null
    }

    return await $fetch<GlobalSearchResponseDto>('/api/search', {
      headers: requestHeaders,
      params: {
        query: normalizedQuery.value,
      },
    })
  },
  {
    watch: [() => normalizedQuery.value],
    immediate: hasMinimumLength.value,
  },
)

watch(
  () => hasMinimumLength.value,
  (canSearch) => {
    if (!canSearch && data.value) {
      data.value = null
    }
  },
)

const { data: verticalsData } = await useAsyncData<VerticalConfigDto[]>(
  'search-verticals',
  () =>
    $fetch<VerticalConfigDto[]>('/api/categories', {
      headers: requestHeaders,
      params: { onlyEnabled: true },
    }),
)

const verticals = computed(() => verticalsData.value ?? [])
const verticalById = computed(() => {
  const entries = new Map<string, VerticalConfigDto>()

  verticals.value.forEach((vertical) => {
    if (vertical.id) {
      entries.set(vertical.id, vertical)
    }
  })

  return entries
})

interface SearchGroup {
  key: string
  title: string
  countLabel: string | null
  products: ProductDto[]
  popularAttributes: AttributeConfigDto[]
  verticalHomeUrl: string | null
}

const normalizeVerticalHomeUrl = (raw: string | null | undefined): string | null => {
  if (!raw) {
    return null
  }

  const trimmed = raw.trim()

  if (!trimmed) {
    return null
  }

  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`
}

const buildGroupCountLabel = (count: number) =>
  translatePlural('search.groups.count', count, { count })

const extractProducts = (
  results: { product?: ProductDto | null }[] | undefined,
): ProductDto[] =>
  (results ?? [])
    .map((entry) => entry.product)
    .filter((product): product is ProductDto => Boolean(product))

const primaryGroups = computed(() => {
  const groups = data.value?.verticalGroups ?? []

  return groups
    .map((group, index) => {
      const products = extractProducts(group.results)
      const verticalId = group.verticalId ?? null
      const vertical = verticalId ? verticalById.value.get(verticalId) ?? null : null

      if (!products.length) {
        return null
      }

      const title =
        vertical?.verticalHomeTitle ??
        (verticalId ? formatFallbackVerticalTitle(verticalId) : t('search.groups.unknownTitle'))

      const countLabel = buildGroupCountLabel(products.length)
      const verticalHomeUrl = normalizeVerticalHomeUrl(vertical?.verticalHomeUrl)

      return {
        key: `primary-${verticalId ?? index}`,
        title,
        countLabel,
        products,
        popularAttributes: vertical?.popularAttributes ?? [],
        verticalHomeUrl,
      } satisfies SearchGroup | null
    })
    .filter((group): group is SearchGroup => Boolean(group))
})

const fallbackGroups = computed(() => {
  const grouped = new Map<string | null, ProductDto[]>()

  for (const entry of data.value?.fallbackResults ?? []) {
    if (!entry?.product) {
      continue
    }

    const verticalId = entry.product.base?.vertical ?? null

    if (!grouped.has(verticalId)) {
      grouped.set(verticalId, [])
    }

    grouped.get(verticalId)?.push(entry.product)
  }

  return Array.from(grouped.entries())
    .map(([verticalId, products], index) => {
      if (!products.length) {
        return null
      }

      const vertical = verticalId ? verticalById.value.get(verticalId) ?? null : null
      const title =
        vertical?.verticalHomeTitle ??
        (verticalId ? formatFallbackVerticalTitle(verticalId) : t('search.groups.unknownTitle'))
      const countLabel = buildGroupCountLabel(products.length)
      const verticalHomeUrl = normalizeVerticalHomeUrl(vertical?.verticalHomeUrl)

      return {
        key: `fallback-${verticalId ?? index}`,
        title,
        countLabel,
        products,
        popularAttributes: vertical?.popularAttributes ?? [],
        verticalHomeUrl,
      } satisfies SearchGroup | null
    })
    .filter((group): group is SearchGroup => Boolean(group))
    .sort((a, b) => b.products.length - a.products.length)
})

const usingFallback = computed(
  () => !primaryGroups.value.length && fallbackGroups.value.length > 0,
)
const displayGroups = computed(() =>
  primaryGroups.value.length ? primaryGroups.value : fallbackGroups.value,
)

const totalResults = computed(() =>
  displayGroups.value.reduce((sum, group) => sum + group.products.length, 0),
)

const resultsCountLabel = computed(() =>
  translatePlural('search.results.count', totalResults.value, {
    count: totalResults.value,
  }),
)

const resultsSummaryLabel = computed(() =>
  t('search.results.summary', {
    countLabel: resultsCountLabel.value,
    query: normalizedQuery.value,
  }),
)

const handleSearchSubmit = () => {
  const value = searchInput.value.trim()

  if (value.length > 0 && value.length < MIN_QUERY_LENGTH) {
    return
  }

  trackSearch({ query: value, source: 'form' })

  router.push({
    path: route.path,
    query: value ? { q: value } : {},
  })
}

const handleClear = () => {
  searchInput.value = ''
  router.replace({
    path: route.path,
    query: {},
  })
}

const handleCategorySuggestion = (suggestion: CategorySuggestionItem) => {
  const verticalUrl = normalizeVerticalHomeUrl(
    suggestion.url ??
      (suggestion.verticalId
        ? verticalById.value.get(suggestion.verticalId)?.verticalHomeUrl
        : null),
  )

  if (!verticalUrl) {
    return
  }

  trackSearch({ query: suggestion.title, source: 'suggestion' })

  router.push(verticalUrl)
}

const handleProductSuggestion = (suggestion: ProductSuggestionItem) => {
  const gtin = suggestion.gtin?.trim()

  if (!gtin) {
    return
  }

  trackSearch({ query: suggestion.title ?? gtin, source: 'suggestion' })

  router.push(
    localePath({
      name: 'gtin',
      params: { gtin },
    }),
  )
}

const canonicalUrl = computed(
  () => new URL(localePath({ name: 'search' }), requestURL.origin).toString(),
)
const siteName = computed(() => String(t('siteIdentity.siteName')))
const ogLocale = computed(() => locale.value.replace('-', '_'))
const ogImageUrl = computed(
  () => new URL('/nudger-icon-512x512.png', requestURL.origin).toString(),
)
const ogImageAlt = computed(() => String(t('search.seo.imageAlt')))
const alternateLinks = computed(() =>
  availableLocales.map((availableLocale) => ({
    rel: 'alternate' as const,
    hreflang: availableLocale,
    href: new URL(
      localePath({ name: 'search' }, availableLocale),
      requestURL.origin,
    ).toString(),
  })),
)

useSeoMeta({
  title: () => String(t('search.seo.title')),
  description: () => String(t('search.seo.description')),
  ogTitle: () => String(t('search.seo.title')),
  ogDescription: () => String(t('search.seo.description')),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
  ogSiteName: () => siteName.value,
  ogLocale: () => ogLocale.value,
  ogImageAlt: () => ogImageAlt.value,
})

useHead(() => ({
  link: [
    { rel: 'canonical', href: canonicalUrl.value },
    ...alternateLinks.value,
  ],
}))

function formatFallbackVerticalTitle(verticalId: string): string {
  return verticalId
    .split(/[-_]/u)
    .filter(Boolean)
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(' ')
}
</script>

<style scoped lang="sass">
.search-page
  display: flex
  flex-direction: column
  gap: 0

  &__loader
    margin-top: -1px

  &__results
    max-width: 1200px
    margin: 0 auto

  &__alert-content
    display: flex
    flex-direction: column
    gap: 0.5rem

  &__alert-title
    margin: 0
    font-weight: 600
    font-size: 1.1rem

  &__alert-description
    margin: 0

  &__empty
    text-align: center
    padding: 3rem 1rem
    background: rgba(var(--v-theme-surface-muted), 0.6)
    border-radius: 1.5rem

  &__empty-title
    margin: 0
    font-size: clamp(1.5rem, 1.2rem + 0.8vw, 2rem)
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__empty-description
    margin: 1rem auto 0
    max-width: 28rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__group-wrapper
    display: flex
    flex-direction: column
    gap: 2rem

  &__summary
    margin: 0
    font-weight: 500
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__fallback
    margin: 0
    color: rgb(var(--v-theme-accent-supporting))
    font-weight: 500

  &__group-wrapper :deep(.search-result-group:first-of-type)
    padding-top: 0

  &__group-wrapper :deep(.search-result-group:last-of-type)
    padding-bottom: 0
    border-bottom: none

.search-hero
  position: relative
  width: 100%
  background: linear-gradient(
    135deg,
    rgb(var(--v-theme-hero-gradient-start)) 0%,
    rgba(var(--v-theme-hero-gradient-mid), 0.92) 55%,
    rgb(var(--v-theme-hero-gradient-end)) 100%
  )
  color: rgb(255, 255, 255)
  padding-block: clamp(1.75rem, 4vw, 3rem)
  margin-bottom: clamp(1.5rem, 4vw, 2.75rem)
  overflow: hidden
  box-shadow: 0 18px 40px -24px rgba(var(--v-theme-shadow-primary-600), 0.45)

  &::after
    content: ''
    position: absolute
    inset: 0
    background: radial-gradient(circle at 20% 20%, rgba(var(--v-theme-hero-overlay-strong), 0.2), transparent 60%)
    pointer-events: none

  &__container
    position: relative
    z-index: 1
    padding-block: clamp(1rem, 3vw, 1.75rem)

  &__content
    display: flex
    flex-direction: column
    gap: 0.85rem
    max-width: min(52rem, 100%)

  &__eyebrow
    display: inline-flex
    align-items: center
    align-self: flex-start
    padding: 0.4rem 0.9rem
    border-radius: 999px
    background-color: rgba(var(--v-theme-hero-pill-on-dark), 0.16)
    font-weight: 600
    letter-spacing: 0.08em
    text-transform: uppercase
    font-size: 0.75rem
    line-height: 1.1

  &__copy
    display: flex
    flex-direction: column
    gap: 0.5rem

  &__title
    font-weight: 700
    font-size: clamp(1.95rem, 4.5vw, 2.65rem)
    line-height: 1.2
    margin: 0

  &__subtitle
    margin: 0
    --subtitle-size: clamp(1rem, 2.2vw, 1.2rem)
    --subtitle-color: rgba(var(--v-theme-hero-overlay-soft), 0.85)
    line-height: 1.55

  &__form
    display: flex
    flex-direction: column
    gap: 0.75rem
    margin-top: 1rem

    @media (min-width: 640px)
      flex-direction: row
      align-items: center
      gap: 1rem

  &__field
    flex: 1 1 auto

  &__submit
    align-self: stretch
    text-transform: none
    font-weight: 600
    letter-spacing: 0
    border-radius: 999px

    @media (min-width: 640px)
      align-self: center
      min-width: 8rem

    :deep(.v-btn)
      background-color: rgba(var(--v-theme-hero-overlay-soft), 0.18)
      color: rgb(var(--v-theme-hero-overlay-soft))
      border-radius: 999px
      padding-inline: 1.75rem
      backdrop-filter: blur(6px)
      transition: background-color 0.2s ease, transform 0.2s ease

      &:hover
        background-color: rgba(var(--v-theme-hero-overlay-soft), 0.28)
        transform: translateY(-1px)

      &:focus-visible
        box-shadow: 0 0 0 3px rgba(var(--v-theme-hero-overlay-soft), 0.35)

  &__helper
    margin: 0
    margin-top: 0.75rem
    font-size: 0.95rem
    color: rgba(var(--v-theme-hero-overlay-soft), 0.82)

    &--warning
      color: rgba(var(--v-theme-hero-overlay-soft), 0.95)
</style>
