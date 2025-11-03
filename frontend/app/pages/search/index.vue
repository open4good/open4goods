<template>
  <div class="search-page">
    <v-container class="search-page__hero" fluid>
      <div class="search-page__hero-content">
        <p class="search-page__eyebrow">{{ t('search.hero.eyebrow') }}</p>
        <h1 class="search-page__title">{{ t('search.hero.title') }}</h1>
        <p class="search-page__subtitle">{{ t('search.hero.subtitle') }}</p>
      </div>

      <form class="search-page__form" @submit.prevent="handleSearchSubmit">
        <v-text-field
          v-model="searchInput"
          :label="t('search.form.label')"
          :placeholder="t('search.form.placeholder')"
          :aria-label="t('search.form.ariaLabel')"
          prepend-inner-icon="mdi-magnify"
          variant="solo"
          density="comfortable"
          clearable
          hide-details
          class="search-page__field"
          @click:clear="handleClear"
        />
        <v-btn class="search-page__submit" type="submit" color="primary" size="large">
          {{ t('search.form.submit') }}
        </v-btn>
      </form>

      <p v-if="showInitialState" class="search-page__helper">
        {{ t('search.states.initial') }}
      </p>
      <p v-else-if="showMinimumNotice" class="search-page__helper search-page__helper--warning">
        {{ t('search.states.minimum', { min: MIN_QUERY_LENGTH }) }}
      </p>
    </v-container>

    <v-progress-linear
      v-if="pending"
      class="search-page__loader"
      indeterminate
      color="primary"
      :aria-label="t('search.states.loadingAria')"
      role="progressbar"
    />

    <v-container v-if="hasMinimumLength" class="search-page__results" fluid>
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
            {{ t('search.results.summary', { count: totalResults, query: normalizedQuery }) }}
          </p>
          <p v-if="usingFallback" class="search-page__fallback">
            {{ t('search.notice.fallback') }}
          </p>

          <SearchResultGroup
            v-for="group in displayGroups"
            :key="group.key"
            :title="group.title"
            :eyebrow="group.eyebrow"
            :count-label="group.countLabel"
            :products="group.products"
            :popular-attributes="group.popularAttributes"
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
import SearchResultGroup from '~/components/search/SearchResultGroup.vue'

const MIN_QUERY_LENGTH = 2

definePageMeta({
  ssr: true,
})

const { t, locale, availableLocales } = useI18n()
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
  eyebrow: string | null
  countLabel: string | null
  products: ProductDto[]
  popularAttributes: AttributeConfigDto[]
}

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

      const eyebrow = vertical?.verticalHomeDescription ?? null
      const countLabel = t('search.groups.count', {
        count: products.length,
      })

      return {
        key: `primary-${verticalId ?? index}`,
        title,
        eyebrow,
        countLabel,
        products,
        popularAttributes: vertical?.popularAttributes ?? [],
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
      const eyebrow = vertical?.verticalHomeDescription ?? null
      const countLabel = t('search.groups.count', {
        count: products.length,
      })

      return {
        key: `fallback-${verticalId ?? index}`,
        title,
        eyebrow,
        countLabel,
        products,
        popularAttributes: vertical?.popularAttributes ?? [],
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

const handleSearchSubmit = () => {
  const value = searchInput.value.trim()

  if (value.length > 0 && value.length < MIN_QUERY_LENGTH) {
    return
  }

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

  &__hero
    padding: clamp(2rem, 5vw, 4rem) 1.5rem
    background: linear-gradient(
      135deg,
      rgba(var(--v-theme-surface-primary-080), 0.9),
      rgba(var(--v-theme-surface-glass), 0.9)
    )

    @media (min-width: 960px)
      padding-inline: clamp(2rem, 10vw, 6rem)

  &__hero-content
    display: flex
    flex-direction: column
    gap: 0.75rem
    margin-bottom: 1.5rem

  &__eyebrow
    margin: 0
    font-size: 0.875rem
    letter-spacing: 0.1em
    text-transform: uppercase
    color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

  &__title
    margin: 0
    font-size: clamp(2rem, 1.5rem + 1.5vw, 3rem)
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))

  &__subtitle
    margin: 0
    max-width: 42rem
    font-size: clamp(1rem, 0.95rem + 0.4vw, 1.25rem)
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__form
    display: flex
    flex-direction: column
    gap: 0.75rem

    @media (min-width: 640px)
      flex-direction: row
      align-items: center
      gap: 1rem

  &__field
    flex: 1 1 auto

  &__submit
    align-self: stretch

    @media (min-width: 640px)
      align-self: center
      min-width: 8rem

  &__helper
    margin: 0
    margin-top: 1rem
    font-size: 0.95rem
    color: rgb(var(--v-theme-text-neutral-secondary))

    &--warning
      color: rgb(var(--v-theme-accent-supporting))

  &__loader
    margin-top: -1px

  &__results
    padding: clamp(2rem, 5vw, 4rem) 1.5rem
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
</style>
