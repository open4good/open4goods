<template>
  <div class="search-page" data-testid="search-page">
    <section class="search-page__hero" aria-labelledby="search-hero-title">
      <v-container class="search-page__hero-container" max-width="xl">
        <div class="search-page__hero-content">
          <p class="search-page__hero-eyebrow">{{ t('search.hero.eyebrow') }}</p>
          <h1 id="search-hero-title" class="search-page__hero-title">
            {{ t('search.hero.title') }}
          </h1>
          <p class="search-page__hero-subtitle">
            {{ t('search.hero.subtitle') }}
          </p>

          <form class="search-page__form" @submit.prevent="submitSearch">
            <v-text-field
              v-model="searchTerm"
              :label="t('search.hero.form.label')"
              :placeholder="t('search.hero.form.placeholder')"
              prepend-inner-icon="mdi-magnify"
              variant="solo"
              density="comfortable"
              type="search"
              clearable
              :aria-label="t('search.hero.form.label')"
              class="search-page__input"
              @click:clear="clearSearch"
            />
            <v-btn
              type="submit"
              color="primary"
              size="large"
              class="search-page__submit"
            >
              {{ t('search.hero.form.submit') }}
            </v-btn>
          </form>

          <p
            v-if="hasActiveQuery && !pending"
            class="search-page__summary"
            aria-live="polite"
          >
            {{ resultSummary }}
          </p>
        </div>
      </v-container>
    </section>

    <v-progress-linear
      v-if="pending"
      indeterminate
      color="primary"
      class="search-page__loader"
      :aria-label="t('search.loadingAriaLabel')"
      role="status"
    />

    <v-container
      v-if="error && hasActiveQuery"
      class="search-page__state"
      max-width="lg"
    >
      <v-alert
        type="error"
        variant="tonal"
        border="start"
        prominent
        role="alert"
        class="mb-4"
      >
        {{ t('search.results.error.title') }}
      </v-alert>
      <v-btn color="primary" variant="flat" @click="refresh">
        {{ t('common.actions.retry') }}
      </v-btn>
    </v-container>

    <v-container
      v-else-if="!hasActiveQuery && !pending"
      class="search-page__state"
      max-width="lg"
    >
      <div class="search-page__empty">
        <h2 class="search-page__state-title">
          {{ t('search.results.start.title') }}
        </h2>
        <p class="search-page__state-description">
          {{ t('search.results.start.description') }}
        </p>
      </div>
    </v-container>

    <v-container
      v-else-if="showNoResults"
      class="search-page__state"
      max-width="lg"
    >
      <div class="search-page__empty">
        <h2 class="search-page__state-title">
          {{ t('search.results.noResults.title', { query: activeQuery }) }}
        </h2>
        <p class="search-page__state-description">
          {{ t('search.results.noResults.description') }}
        </p>
      </div>
    </v-container>

    <section
      v-for="section in verticalSectionsWithMetadata"
      :key="section.key"
      class="search-page__section"
      :aria-labelledby="`search-section-${section.key}`"
    >
      <v-container max-width="xl">
        <div class="search-page__section-header">
          <div>
            <p class="search-page__section-eyebrow">
              {{ t('search.sections.verticalEyebrow') }}
            </p>
            <h2
              :id="`search-section-${section.key}`"
              class="search-page__section-title"
            >
              {{ section.title }}
            </h2>
            <p v-if="section.description" class="search-page__section-description">
              {{ section.description }}
            </p>
          </div>
          <v-btn
            v-if="section.link"
            :to="section.link"
            variant="text"
            color="primary"
            class="search-page__section-link"
          >
            {{ t('search.results.verticalLinkLabel', { vertical: section.title }) }}
          </v-btn>
        </div>
        <CategoryProductCardGrid :products="section.products" size="compact" />
      </v-container>
    </section>

    <section
      v-if="shouldShowFallback"
      class="search-page__section"
      aria-labelledby="search-fallback-heading"
    >
      <v-container max-width="xl">
        <div class="search-page__section-header">
          <div>
            <p class="search-page__section-eyebrow">
              {{ t('search.sections.fallbackEyebrow') }}
            </p>
            <h2 id="search-fallback-heading" class="search-page__section-title">
              {{ t('search.sections.fallbackTitle') }}
            </h2>
            <p class="search-page__section-description">
              {{ t('search.sections.fallbackDescription') }}
            </p>
          </div>
        </div>
        <CategoryProductCardGrid :products="fallbackProducts" size="compact" />
      </v-container>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from '#imports'
import type { GlobalSearchResponseDto, ProductDto, VerticalConfigFullDto } from '~~/shared/api-client'

import CategoryProductCardGrid from '~/components/category/products/CategoryProductCardGrid.vue'
import { usePluralizedTranslation } from '~/composables/usePluralizedTranslation'

definePageMeta({
  ssr: true,
})

const { t } = useI18n()
const { translatePlural } = usePluralizedTranslation()
const route = useRoute()
const router = useRouter()
const requestURL = useRequestURL()
const requestHeaders = import.meta.server
  ? useRequestHeaders(['host', 'x-forwarded-host'])
  : undefined

const resolveQueryParam = (input: string | string[] | undefined): string => {
  if (Array.isArray(input)) {
    return input[0] ?? ''
  }

  return input ?? ''
}

const rawQuery = computed(() => resolveQueryParam(route.query.q as string | string[] | undefined))
const activeQuery = computed(() => rawQuery.value.trim())
const hasActiveQuery = computed(() => activeQuery.value.length > 0)
const searchTerm = ref(rawQuery.value)

watch(rawQuery, (value) => {
  if (value !== searchTerm.value) {
    searchTerm.value = value
  }
})

const {
  data,
  pending,
  refresh,
  error,
} = await useAsyncData<GlobalSearchResponseDto | null>(
  'global-search-results',
  async () => {
    if (!activeQuery.value) {
      return null
    }

    return await $fetch<GlobalSearchResponseDto>('/api/search', {
      params: { q: activeQuery.value },
      headers: requestHeaders,
    })
  },
  {
    watch: [() => activeQuery.value],
  },
)

watch(
  () => activeQuery.value,
  (value) => {
    if (!value) {
      data.value = null
      if (error.value) {
        error.value = null
      }
    }
  },
)

const searchResponse = computed(() => data.value)

const verticalSections = computed(() => {
  const groups = searchResponse.value?.verticalGroups ?? []

  return groups
    .map((group, index) => {
      const products = (group.results ?? [])
        .map((entry) => entry.product)
        .filter((product): product is ProductDto => Boolean(product))

      if (!products.length) {
        return null
      }

      return {
        key: group.verticalId ?? `group-${index}`,
        verticalId: group.verticalId ?? null,
        products,
      }
    })
    .filter((group): group is { key: string; verticalId: string | null; products: ProductDto[] } => group !== null)
})

const fallbackProducts = computed(() => {
  if (verticalSections.value.length) {
    return [] as ProductDto[]
  }

  return (searchResponse.value?.fallbackResults ?? [])
    .map((entry) => entry.product)
    .filter((product): product is ProductDto => Boolean(product))
})

const shouldShowFallback = computed(() => fallbackProducts.value.length > 0 && !verticalSections.value.length)

const totalProducts = computed(() => {
  if (verticalSections.value.length) {
    return verticalSections.value.reduce((count, section) => count + section.products.length, 0)
  }

  return fallbackProducts.value.length
})

const resultSummary = computed(() => {
  if (!hasActiveQuery.value) {
    return ''
  }

  return translatePlural('search.results.summary', totalProducts.value, {
    query: activeQuery.value,
  })
})

const verticalMetadata = ref<Record<string, VerticalConfigFullDto>>({})
const failedVerticals = new Set<string>()

const resolveVerticalLink = (rawUrl: string | undefined): string | undefined => {
  if (!rawUrl) {
    return undefined
  }

  const trimmed = rawUrl.trim()

  if (!trimmed) {
    return undefined
  }

  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`
}

const loadVerticalMetadata = async (identifiers: Array<string | null | undefined>) => {
  const validIds = identifiers.filter((id): id is string => typeof id === 'string' && id.length > 0)
  const missing = validIds.filter((id) => !verticalMetadata.value[id] && !failedVerticals.has(id))

  if (!missing.length) {
    return
  }

  const entries = await Promise.allSettled(
    missing.map(async (id) => {
      const payload = await $fetch<VerticalConfigFullDto>(`/api/categories/${encodeURIComponent(id)}`, {
        headers: requestHeaders,
      })
      return { id, payload }
    }),
  )

  const nextMetadata: Record<string, VerticalConfigFullDto> = { ...verticalMetadata.value }

  entries.forEach((result, index) => {
    if (result.status === 'fulfilled') {
      nextMetadata[result.value.id] = result.value.payload
    } else if (result.status === 'rejected') {
      const failedId = missing[index]
      if (failedId) {
        failedVerticals.add(failedId)
        console.error('Failed to load vertical metadata for search section:', failedId, result.reason)
      } else {
        console.error('Failed to load vertical metadata for search section:', result.reason)
      }
    }
  })

  verticalMetadata.value = nextMetadata
}

if (import.meta.server) {
  await loadVerticalMetadata(verticalSections.value.map((section) => section.verticalId))
}

if (import.meta.client) {
  watch(
    () => verticalSections.value.map((section) => section.verticalId),
    (ids) => {
      loadVerticalMetadata(ids)
    },
    { immediate: true },
  )
}

const verticalSectionsWithMetadata = computed(() =>
  verticalSections.value.map((section) => {
    const metadata = section.verticalId ? verticalMetadata.value[section.verticalId] : undefined
    const title =
      metadata?.verticalHomeTitle ??
      metadata?.verticalMetaTitle ??
      metadata?.verticalMetaOpenGraphTitle ??
      section.verticalId ??
      t('search.results.unknownVertical')

    return {
      key: section.key,
      title,
      description: metadata?.verticalHomeDescription ?? null,
      link: resolveVerticalLink(metadata?.verticalHomeUrl),
      products: section.products,
    }
  }),
)

const showNoResults = computed(
  () => hasActiveQuery.value && !pending.value && !error.value && totalProducts.value === 0,
)

const submitSearch = () => {
  const trimmed = searchTerm.value.trim()

  router.replace({
    path: route.path,
    query: trimmed ? { q: trimmed } : {},
  })
}

const clearSearch = () => {
  searchTerm.value = ''
  router.replace({ path: route.path, query: {} })
}

const canonicalUrl = computed(() => new URL(route.fullPath, requestURL.origin).toString())
const seoTitle = computed(() =>
  hasActiveQuery.value
    ? t('search.seo.titleWithQuery', { query: activeQuery.value })
    : t('search.seo.title'),
)
const seoDescription = computed(() =>
  hasActiveQuery.value
    ? t('search.seo.descriptionWithQuery', { query: activeQuery.value })
    : t('search.seo.description'),
)

useSeoMeta({
  title: () => String(seoTitle.value),
  description: () => String(seoDescription.value),
  ogTitle: () => String(seoTitle.value),
  ogDescription: () => String(seoDescription.value),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
})
</script>

<style scoped lang="sass">
.search-page__hero
  background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.95), rgba(var(--v-theme-hero-gradient-end), 0.85)), url('/images/backgrounds/texture-grid.svg')
  color: rgb(var(--v-theme-hero-overlay-strong))
  padding-block: 2.5rem

.search-page__hero-content
  display: flex
  flex-direction: column
  gap: 1rem
  max-width: 56rem

.search-page__hero-eyebrow
  font-size: 0.875rem
  text-transform: uppercase
  letter-spacing: 0.08em
  margin: 0
  color: rgba(var(--v-theme-hero-overlay-strong), 0.9)

.search-page__hero-title
  font-size: clamp(2rem, 5vw, 2.75rem)
  margin: 0
  font-weight: 700

.search-page__hero-subtitle
  margin: 0
  font-size: 1.0625rem
  color: rgba(var(--v-theme-hero-overlay-strong), 0.88)

.search-page__form
  display: flex
  flex-direction: column
  gap: 0.75rem
  width: 100%

.search-page__input
  flex: 1 1 auto
  --v-field-border-opacity: 0
  --v-field-background: rgba(255, 255, 255, 0.14)
  border-radius: 1rem
  backdrop-filter: blur(12px)

.search-page__submit
  align-self: flex-start

.search-page__summary
  margin: 0
  font-size: 0.95rem
  color: rgba(var(--v-theme-hero-overlay-strong), 0.9)

@media (min-width: 600px)
  .search-page__form
    flex-direction: row
    align-items: center

  .search-page__submit
    align-self: center

.search-page__loader
  margin: 0

.search-page__state
  padding-block: 3rem
  text-align: center

.search-page__state-title
  font-size: 1.5rem
  font-weight: 600
  margin-bottom: 0.75rem

.search-page__state-description
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

.search-page__section
  padding-block: 3rem

.search-page__section-header
  display: flex
  flex-direction: column
  gap: 1rem
  margin-bottom: 1.75rem

.search-page__section-eyebrow
  font-size: 0.75rem
  text-transform: uppercase
  letter-spacing: 0.08em
  margin: 0 0 0.25rem
  color: rgb(var(--v-theme-text-neutral-soft))

.search-page__section-title
  margin: 0
  font-size: clamp(1.5rem, 4vw, 2rem)
  font-weight: 700
  color: rgb(var(--v-theme-text-neutral-strong))

.search-page__section-description
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  max-width: 60ch

.search-page__section-link
  align-self: flex-start

@media (min-width: 960px)
  .search-page__section-header
    flex-direction: row
    justify-content: space-between
    align-items: center

  .search-page__section-description
    max-width: 70ch
</style>
