<script setup lang="ts">
import { computed, ref } from 'vue'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'
import type { BlogPostDto, VerticalConfigDto } from '~~/shared/api-client'
import SearchSuggestField, {
  type CategorySuggestionItem,
  type ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'
import HomeCategoryCarousel from '~/components/home/HomeCategoryCarousel.vue'
import HomeBlogCarousel from '~/components/home/HomeBlogCarousel.vue'
import TextContent from '~/components/domains/content/TextContent.vue'
import { useCategories } from '~/composables/categories/useCategories'
import { useBlog } from '~/composables/blog/useBlog'

definePageMeta({
  ssr: true,
})

const { t, locale, availableLocales } = useI18n()
const router = useRouter()
const localePath = useLocalePath()
const requestURL = useRequestURL()

const searchQuery = ref('')

const MIN_SUGGESTION_QUERY_LENGTH = 2
const heroVideoSrc = '/videos/video-concept1.mp4'
const heroVideoPoster = '/images/home/hero-placeholder.svg'

type HomeBlogItem = BlogPostDto & { formattedDate?: string }

const { categories: rawCategories, fetchCategories, loading: categoriesLoading } = useCategories()
const { paginatedArticles, fetchArticles, loading: blogLoading } = useBlog()

if (import.meta.server) {
  await fetchCategories(true)
  await fetchArticles(1, 6, null)
} else {
  if (rawCategories.value.length === 0) {
    await fetchCategories(true)
  }

  if (paginatedArticles.value.length === 0) {
    await fetchArticles(1, 6, null)
  }
}

const problemItems = computed(() => [
  {
    icon: 'mdi-sign-direction',
    text: String(t('home.problems.items.labelsOverload')),
  },
  {
    icon: 'mdi-scale-balance',
    text: String(t('home.problems.items.budgetVsEcology')),
  },
  {
    icon: 'mdi-tab-multiple',
    text: String(t('home.problems.items.tooManyTabs')),
  },
])

const solutionBenefits = computed(() => [
  {
    emoji: '⏱️',
    label: String(t('home.solution.benefits.time')),
  },
  {
    emoji: '💰',
    label: String(t('home.solution.benefits.savings')),
  },
  {
    emoji: '🌍',
    label: String(t('home.solution.benefits.planet')),
  },
  {
    emoji: '🛡️',
    label: String(t('home.solution.benefits.trust')),
  },
])

const featureCards = computed(() => [
  {
    icon: 'mdi-leaf-circle',
    title: String(t('home.features.cards.impactScore.title')),
    description: String(t('home.features.cards.impactScore.description')),
  },
  {
    icon: 'mdi-chart-line',
    title: String(t('home.features.cards.priceComparison.title')),
    description: String(t('home.features.cards.priceComparison.description')),
  },
  {
    icon: 'mdi-source-branch',
    title: String(t('home.features.cards.openIndependent.title')),
    description: String(t('home.features.cards.openIndependent.description')),
  },
  {
    icon: 'mdi-shield-off-outline',
    title: String(t('home.features.cards.noTracking.title')),
    description: String(t('home.features.cards.noTracking.description')),
  },
  {
    icon: 'mdi-database',
    title: String(t('home.features.cards.massiveBase.title')),
    description: String(t('home.features.cards.massiveBase.description')),
  },
])

const objectionItems = computed(() => [
  {
    icon: 'mdi-lightning-bolt',
    question: String(t('home.objections.items.aiEnergy.question')),
    answer: String(t('home.objections.items.aiEnergy.answer')),
  },
  {
    icon: 'mdi-recycle',
    question: String(t('home.objections.items.reuse.question')),
    answer: String(t('home.objections.items.reuse.answer')),
  },
  {
    icon: 'mdi-scale-balance',
    question: String(t('home.objections.items.independence.question')),
    answer: String(t('home.objections.items.independence.answer')),
  },
])

const faqItems = computed(() => [
  {
    question: String(t('home.faq.items.free.question')),
    answer: String(t('home.faq.items.free.answer')),
  },
  {
    question: String(t('home.faq.items.account.question')),
    answer: String(t('home.faq.items.account.answer')),
  },
  {
    question: String(t('home.faq.items.categories.question')),
    answer: String(t('home.faq.items.categories.answer')),
  },
  {
    question: String(t('home.faq.items.impactScore.question')),
    answer: String(t('home.faq.items.impactScore.answer')),
  },
  {
    question: String(t('home.faq.items.dataFreshness.question')),
    answer: String(t('home.faq.items.dataFreshness.answer')),
  },
  {
    question: String(t('home.faq.items.suggestProduct.question')),
    answer: String(t('home.faq.items.suggestProduct.answer')),
  },
])

const faqPanels = computed(() =>
  faqItems.value.map((item, index) => ({
    ...item,
    blocId: `HOME:FAQ:${index + 1}`,
  })),
)

const faqJsonLd = computed(() => ({
  '@context': 'https://schema.org',
  '@type': 'FAQPage',
  mainEntity: faqItems.value.map((item) => ({
    '@type': 'Question',
    name: item.question,
    acceptedAnswer: {
      '@type': 'Answer',
      text: item.answer,
    },
  })),
}))

const canonicalUrl = computed(
  () => new URL(resolveLocalizedRoutePath('index', locale.value), requestURL.origin).toString(),
)

const alternateLinks = computed(() =>
  availableLocales.map((availableLocale) => ({
    rel: 'alternate' as const,
    hreflang: availableLocale,
    href: new URL(resolveLocalizedRoutePath('index', availableLocale), requestURL.origin).toString(),
  })),
)

const searchLandingUrl = computed(() => localePath({ name: 'search' }))
const categoriesLandingUrl = computed(() => localePath({ name: 'categories' }))

const resolveCategoryHref = (category: VerticalConfigDto) => {
  const rawSlug = category.verticalHomeUrl?.trim()

  if (rawSlug) {
    if (/^https?:\/\//i.test(rawSlug)) {
      return rawSlug
    }

    if (rawSlug.startsWith('/')) {
      return rawSlug
    }

    return localePath({ name: 'categories-slug', params: { slug: rawSlug } })
  }

  const normalizedTitle = category.verticalHomeTitle?.trim()

  if (normalizedTitle) {
    return localePath({ name: 'search', query: { q: normalizedTitle } })
  }

  return searchLandingUrl.value
}

const categoryCarouselItems = computed(() => {
  const categories = [...rawCategories.value]
    .filter((category) => category.enabled !== false)
    .sort((a, b) => {
      if (a.popular !== b.popular) {
        return (b.popular ? 1 : 0) - (a.popular ? 1 : 0)
      }

      const orderA = a.order ?? Number.MAX_SAFE_INTEGER
      const orderB = b.order ?? Number.MAX_SAFE_INTEGER

      if (orderA !== orderB) {
        return orderA - orderB
      }

      return (a.verticalHomeTitle ?? '').localeCompare(b.verticalHomeTitle ?? '')
    })

  if (categories.length === 0) {
    return [] as {
      id: string
      title: string
      description: string
      href: string
      image?: string | null
    }[]
  }

  return categories.map((category, index) => {
    const title = category.verticalHomeTitle?.trim() || String(t('home.categories.fallbackTitle'))
    const description =
      category.verticalHomeDescription?.trim() || String(t('home.categories.fallbackDescription'))

    return {
      id: category.id ?? category.verticalHomeUrl ?? `${title}-${index}`,
      title,
      description,
      href: resolveCategoryHref(category),
      image: category.imageLarge ?? category.imageMedium ?? category.imageSmall ?? null,
    }
  })
})

const blogDateFormatter = computed(
  () => new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium' }),
)

const fallbackBlogEntries = computed<HomeBlogItem[]>(() => [
  {
    url: localePath({ name: 'blog' }),
    title: String(t('home.blog.items.first.title')),
    summary: String(t('home.blog.items.first.excerpt')),
    formattedDate: String(t('home.blog.items.first.date')),
  },
  {
    url: localePath({ name: 'blog' }),
    title: String(t('home.blog.items.second.title')),
    summary: String(t('home.blog.items.second.excerpt')),
    formattedDate: String(t('home.blog.items.second.date')),
  },
  {
    url: localePath({ name: 'blog' }),
    title: String(t('home.blog.items.third.title')),
    summary: String(t('home.blog.items.third.excerpt')),
    formattedDate: String(t('home.blog.items.third.date')),
  },
])

const blogCarouselItems = computed<HomeBlogItem[]>(() => {
  const articles = paginatedArticles.value.slice(0, 6)

  if (!articles.length) {
    return fallbackBlogEntries.value
  }

  return articles.map((article) => ({
    ...article,
    summary: article.summary ?? '',
    formattedDate: article.createdMs
      ? blogDateFormatter.value.format(new Date(article.createdMs))
      : '',
  }))
})

const ogImageUrl = computed(() => new URL('/nudger-icon-512x512.png', requestURL.origin).toString())

const navigateToSearch = (query?: string) => {
  const normalizedQuery = query?.trim() ?? ''

  router.push(
    localePath({
      name: 'search',
      query: normalizedQuery ? { q: normalizedQuery } : undefined,
    }),
  )
}

const handleSearchSubmit = () => {
  const trimmedQuery = searchQuery.value.trim()

  if (trimmedQuery.length > 0 && trimmedQuery.length < MIN_SUGGESTION_QUERY_LENGTH) {
    return
  }

  navigateToSearch(trimmedQuery)
}

const handleCategorySuggestion = (suggestion: CategorySuggestionItem) => {
  searchQuery.value = suggestion.title

  const normalizedUrl = suggestion.url?.trim()

  if (normalizedUrl) {
    router.push(localePath(normalizedUrl))
    return
  }

  navigateToSearch(suggestion.title)
}

const handleProductSuggestion = (suggestion: ProductSuggestionItem) => {
  searchQuery.value = suggestion.title
  const gtin = suggestion.gtin?.trim()

  if (gtin) {
    router.push(
      localePath({
        name: 'gtin',
        params: { gtin },
      }),
    )
    return
  }

  navigateToSearch(suggestion.title)
}

useSeoMeta({
  title: () => String(t('home.seo.title')),
  description: () => String(t('home.seo.description')),
  ogTitle: () => String(t('home.seo.title')),
  ogDescription: () => String(t('home.seo.description')),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
  ogSiteName: () => String(t('siteIdentity.siteName')),
  ogLocale: () => locale.value.replace('-', '_'),
  ogImageAlt: () => String(t('home.seo.imageAlt')),
})

useHead(() => ({
  link: [
    { rel: 'canonical', href: canonicalUrl.value },
    ...alternateLinks.value,
  ],
  script: [
    {
      key: 'home-faq-jsonld',
      type: 'application/ld+json',
      children: JSON.stringify(faqJsonLd.value),
    },
  ],
}))
</script>

<template>
  <div class="home-page">
    <section class="home-hero" aria-labelledby="home-hero-title">
      <v-container class="home-hero__container" max-width="lg">
        <div class="home-hero__layout">
          <div class="home-hero__content">
            <p class="home-hero__eyebrow">{{ t('home.hero.eyebrow') }}</p>
            <h1 id="home-hero-title" class="home-hero__title">
              {{ t('home.hero.title') }}
            </h1>
            <p class="home-hero__subtitle">{{ t('home.hero.subtitle') }}</p>

            <form class="home-hero__search" role="search" @submit.prevent="handleSearchSubmit">
              <SearchSuggestField
                v-model="searchQuery"
                class="home-hero__search-input"
                :label="t('home.hero.search.label')"
                :placeholder="t('home.hero.search.placeholder')"
                :aria-label="t('home.hero.search.ariaLabel')"
                :min-chars="MIN_SUGGESTION_QUERY_LENGTH"
                @submit="handleSearchSubmit"
                @select-category="handleCategorySuggestion"
                @select-product="handleProductSuggestion"
              >
                <template #append-inner>
                  <v-btn
                    class="home-hero__search-submit"
                    icon="mdi-arrow-right"
                    variant="flat"
                    color="primary"
                    size="small"
                    type="submit"
                    :aria-label="t('home.hero.search.cta')"
                  />
                </template>
              </SearchSuggestField>
            </form>

            <p class="home-hero__helper">
              <span aria-hidden="true">⚡</span>
              {{ t('home.hero.search.helper') }}
            </p>
          </div>

          <div class="home-hero__media" aria-hidden="true">
            <v-sheet rounded="xl" elevation="6" class="home-hero__media-sheet">
              <div class="home-hero__video-wrapper">
                <video
                  class="home-hero__video"
                  :poster="heroVideoPoster"
                  autoplay
                  muted
                  loop
                  playsinline
                  preload="metadata"
                >
                  <source :src="heroVideoSrc" type="video/mp4" />
                </video>
                <div class="home-hero__video-overlay" />
              </div>
            </v-sheet>
          </div>
        </div>
      </v-container>
    </section>

    <section class="home-section home-problems" aria-labelledby="home-problems-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-problems-title">{{ t('home.problems.title') }}</h2>
        </header>
        <ul class="home-problems__list">
          <li
            v-for="(item, index) in problemItems"
            :key="item.text"
            class="home-problems__item"
            :class="{ 'home-problems__item--reverse': index % 2 === 1 }"
          >
            <v-card class="home-problems__card" variant="tonal">
              <div class="home-problems__icon-wrapper" aria-hidden="true">
                <v-icon class="home-problems__icon" :icon="item.icon" size="48" />
              </div>
              <p class="home-problems__text">{{ item.text }}</p>
            </v-card>
          </li>
        </ul>
      </v-container>
    </section>

    <section class="home-section home-solution" aria-labelledby="home-solution-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-solution-title">{{ t('home.solution.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.solution.description') }}</p>
        </header>
        <v-row class="home-solution__grid" align="stretch" no-gutters>
          <v-col
            v-for="item in solutionBenefits"
            :key="item.label"
            cols="12"
            sm="6"
          >
            <v-card class="home-solution__card" variant="outlined">
              <span class="home-solution__emoji" aria-hidden="true">{{ item.emoji }}</span>
              <p class="home-solution__label">{{ item.label }}</p>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </section>

    <section class="home-section home-features" aria-labelledby="home-features-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-features-title">{{ t('home.features.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.features.subtitle') }}</p>
        </header>
        <v-row class="home-features__grid" align="stretch">
          <v-col v-for="card in featureCards" :key="card.title" cols="12" sm="6" md="4">
            <v-card class="home-features__card" variant="outlined">
              <v-icon class="home-features__icon" :icon="card.icon" size="32" />
              <h3 class="home-features__card-title">{{ card.title }}</h3>
              <p class="home-features__card-description">{{ card.description }}</p>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </section>

    <section class="home-section home-categories" aria-labelledby="home-categories-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-categories-title">{{ t('home.categories.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.categories.subtitle') }}</p>
        </header>
        <HomeCategoryCarousel :items="categoryCarouselItems" :loading="categoriesLoading" />
      </v-container>
    </section>

    <section class="home-section home-blog" aria-labelledby="home-blog-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-blog-title">{{ t('home.blog.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.blog.subtitle') }}</p>
        </header>
        <HomeBlogCarousel :items="blogCarouselItems" :loading="blogLoading" />
        <div class="home-blog__actions">
          <v-btn
            class="home-blog__cta"
            :to="localePath('blog')"
            color="primary"
            variant="tonal"
            size="large"
          >
            {{ t('home.blog.cta') }}
          </v-btn>
        </div>
      </v-container>
    </section>

    <section class="home-section home-objections" aria-labelledby="home-objections-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-objections-title">{{ t('home.objections.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.objections.subtitle') }}</p>
        </header>
        <v-row class="home-objections__grid">
          <v-col v-for="item in objectionItems" :key="item.question" cols="12" md="4">
            <v-card class="home-objections__card" variant="outlined">
              <div class="home-objections__card-header">
                <v-icon class="home-objections__icon" :icon="item.icon" size="28" />
                <h3 class="home-objections__question">{{ item.question }}</h3>
              </div>
              <p class="home-objections__answer">{{ item.answer }}</p>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </section>

    <section class="home-section home-faq" aria-labelledby="home-faq-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-faq-title">{{ t('home.faq.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.faq.subtitle') }}</p>
        </header>
        <v-expansion-panels class="home-faq__panels" multiple variant="accordion">
          <v-expansion-panel v-for="panel in faqPanels" :key="panel.question">
            <v-expansion-panel-title class="home-faq__panel-title">
              {{ panel.question }}
            </v-expansion-panel-title>
            <v-expansion-panel-text class="home-faq__panel-text">
              <TextContent
                class="home-faq__text-content"
                :bloc-id="panel.blocId"
                :fallback-text="panel.answer"
                :ipsum-length="panel.answer.length"
              />
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-container>
    </section>

    <section class="home-cta" aria-labelledby="home-cta-title">
      <v-container class="home-cta__container" max-width="lg">
        <div class="home-cta__content">
          <h2 id="home-cta-title" class="home-cta__title">{{ t('home.cta.title') }}</h2>
          <p class="home-cta__subtitle">{{ t('home.cta.subtitle') }}</p>
          <div class="home-cta__actions">
            <form class="home-cta__form" role="search" @submit.prevent="handleSearchSubmit">
              <v-text-field
                v-model="searchQuery"
                class="home-cta__search-input"
                variant="outlined"
                density="comfortable"
                :label="t('home.hero.search.label')"
                :placeholder="t('home.hero.search.placeholder')"
                :aria-label="t('home.hero.search.ariaLabel')"
                prepend-inner-icon="mdi-magnify"
                hide-details="auto"
              >
                <template #append-inner>
                  <v-btn
                    class="home-cta__submit"
                    icon="mdi-arrow-right"
                    variant="flat"
                    color="primary"
                    size="small"
                    type="submit"
                    :aria-label="t('home.cta.searchSubmit')"
                  />
                </template>
              </v-text-field>
            </form>
            <div class="home-cta__links">
              <v-btn
                class="home-cta__browse"
                :to="categoriesLandingUrl"
                color="primary"
                variant="tonal"
                size="large"
              >
                {{ t('home.cta.browseTaxonomy') }}
              </v-btn>
              <NuxtLink class="home-cta__link" :to="searchLandingUrl">
                {{ t('home.cta.altLink') }}
              </NuxtLink>
            </div>
          </div>
        </div>
      </v-container>
    </section>
  </div>
</template>

<style scoped lang="sass">
.home-page
  display: flex
  flex-direction: column

.home-section
  padding-block: clamp(3rem, 6vw, 6rem)
  background: rgb(var(--v-theme-surface-default))

.home-section:nth-of-type(even)
  background: rgba(var(--v-theme-surface-muted), 0.6)

.home-section__container
  display: flex
  flex-direction: column
  gap: clamp(2rem, 5vw, 3.5rem)

.home-section__header
  max-width: 720px
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-section__subtitle
  color: rgb(var(--v-theme-text-neutral-secondary))
  margin: 0

.home-hero
  padding-block: clamp(4rem, 10vw, 7rem)
  background: radial-gradient(circle at top left, rgba(var(--v-theme-hero-gradient-start), 0.28), transparent 55%), radial-gradient(circle at bottom right, rgba(var(--v-theme-hero-gradient-end), 0.25), transparent 60%), rgb(var(--v-theme-surface-default))

.home-hero__container
  display: flex
  flex-direction: column
  gap: clamp(2rem, 5vw, 3rem)

.home-hero__layout
  display: grid
  gap: clamp(2rem, 5vw, 3rem)

.home-hero__content
  display: flex
  flex-direction: column
  gap: 1.5rem

.home-hero__eyebrow
  font-weight: 600
  letter-spacing: 0.08em
  text-transform: uppercase
  color: rgba(var(--v-theme-hero-gradient-end), 0.9)
  margin: 0

.home-hero__title
  font-size: clamp(2rem, 5vw, 3.5rem)
  line-height: 1.1
  margin: 0

.home-hero__subtitle
  font-size: clamp(1.05rem, 2.5vw, 1.35rem)
  color: rgb(var(--v-theme-text-neutral-secondary))
  margin: 0

.home-hero__search
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-hero__search-input :deep(.v-field)
  border-radius: 999px

.home-hero__search-input :deep(input)
  font-size: 1.05rem

.home-hero__search-input :deep(.v-field__append-inner)
  padding-inline-end: 0.5rem

.home-hero__search-input :deep(.home-hero__search-submit)
  border-radius: 999px
  box-shadow: none

.home-hero__search-input :deep(.home-hero__search-submit .v-icon)
  color: rgb(var(--v-theme-surface-default))

.home-hero__helper
  display: flex
  align-items: center
  gap: 0.5rem
  font-size: 0.95rem
  color: rgb(var(--v-theme-text-neutral-soft))
  margin: 0

.home-hero__media
  display: flex
  justify-content: center

.home-hero__media-sheet
  padding: clamp(1rem, 3vw, 2rem)
  background: rgba(var(--v-theme-surface-glass-strong), 0.6)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)
  backdrop-filter: blur(14px)

.home-hero__video-wrapper
  position: relative
  overflow: hidden
  border-radius: clamp(1.25rem, 4vw, 1.75rem)
  aspect-ratio: 4 / 5
  min-height: 280px
  max-height: 360px
  box-shadow: 0 24px 40px -28px rgba(15, 23, 42, 0.55)

.home-hero__video
  width: 100%
  height: 100%
  display: block
  object-fit: cover
  filter: saturate(1.05)

.home-hero__video-overlay
  position: absolute
  inset: 0
  background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.28), rgba(var(--v-theme-hero-gradient-end), 0.26))
  mix-blend-mode: soft-light
  pointer-events: none

.home-hero__video-overlay::after
  content: ''
  position: absolute
  inset: 0
  border-radius: inherit
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)

.home-problems__list
  list-style: none
  display: flex
  flex-direction: column
  gap: 1.5rem
  padding: 0
  margin: 0

.home-problems__item
  display: flex
  justify-content: flex-start

.home-problems__item--reverse
  justify-content: flex-end

.home-problems__card
  max-width: 560px
  width: 100%
  padding: clamp(1.5rem, 4vw, 2rem)
  display: flex
  align-items: center
  gap: 1.25rem
  border-radius: clamp(1.25rem, 3vw, 1.75rem)
  background: rgba(var(--v-theme-surface-primary-080), 0.65)
  backdrop-filter: blur(12px)
  box-shadow: 0 18px 30px rgba(var(--v-theme-shadow-primary-600), 0.12)

.home-problems__icon-wrapper
  display: flex
  align-items: center
  justify-content: center
  width: 72px
  height: 72px
  border-radius: 24px
  background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.25), rgba(var(--v-theme-hero-gradient-end), 0.25))

.home-problems__icon
  color: rgba(var(--v-theme-hero-gradient-start), 0.95)

.home-problems__text
  margin: 0
  font-size: 1.1rem
  line-height: 1.5
  color: rgb(var(--v-theme-text-neutral-strong))

.home-solution__grid
  gap: 1.5rem

.home-solution__card
  height: 100%
  padding: clamp(1.5rem, 4vw, 2rem)
  border-radius: clamp(1.25rem, 3vw, 1.75rem)
  display: flex
  flex-direction: column
  gap: 0.75rem
  align-items: flex-start
  background: rgba(var(--v-theme-surface-glass-strong), 0.85)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2)
  box-shadow: 0 12px 24px rgba(var(--v-theme-shadow-primary-600), 0.12)

.home-solution__emoji
  font-size: 2rem

.home-solution__label
  margin: 0
  font-size: 1.1rem
  color: rgb(var(--v-theme-text-neutral-strong))

.home-features__grid
  row-gap: 2rem
  column-gap: 1.5rem

.home-features__card
  padding: clamp(1.75rem, 4vw, 2.25rem)
  height: 100%
  display: flex
  flex-direction: column
  gap: 1rem
  border-radius: clamp(1.25rem, 3vw, 1.75rem)
  background: rgba(var(--v-theme-surface-glass-strong), 0.85)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2)
  box-shadow: 0 16px 28px rgba(var(--v-theme-shadow-primary-600), 0.12)

.home-features__icon
  color: rgba(var(--v-theme-hero-gradient-start), 0.9)

.home-features__card-title
  font-size: 1.2rem
  margin: 0

.home-features__card-description
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-categories
  background: rgba(var(--v-theme-surface-primary-050), 0.8)

.home-categories :deep(.home-category-carousel__carousel)
  margin-top: 1.5rem

.home-blog
  background: rgba(var(--v-theme-surface-default), 0.92)

.home-blog :deep(.home-blog-carousel__carousel)
  margin-top: 1.5rem

.home-blog__actions
  display: flex
  justify-content: center
  margin-top: clamp(1.5rem, 4vw, 2.5rem)

.home-blog__cta
  border-radius: 999px
  padding-inline: clamp(1.75rem, 5vw, 2.5rem)

.home-objections__grid
  row-gap: 1.5rem

.home-objections__card
  padding: 1.5rem
  height: 100%
  display: flex
  flex-direction: column
  gap: 0.75rem
  border-radius: 1.25rem
  background: rgba(var(--v-theme-surface-glass-strong), 0.8)

.home-objections__card-header
  display: flex
  gap: 0.75rem
  align-items: center

.home-objections__question
  margin: 0
  font-size: 1.05rem

.home-objections__answer
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-faq__panels
  border-radius: 1.25rem
  background: rgba(var(--v-theme-surface-glass-strong), 0.7)
  padding: 0.5rem

.home-faq__panel-title
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))

.home-faq__panel-text
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-cta
  padding-block: clamp(3rem, 8vw, 5rem)
  background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.16), rgba(var(--v-theme-hero-gradient-end), 0.18))

.home-cta__container
  display: flex
  justify-content: center

.home-cta__content
  background: rgba(var(--v-theme-surface-default), 0.9)
  border-radius: clamp(1.5rem, 4vw, 2rem)
  padding: clamp(2rem, 5vw, 3rem)
  box-shadow: 0 24px 40px rgba(var(--v-theme-shadow-primary-600), 0.12)
  display: flex
  flex-direction: column
  gap: 1.5rem
  max-width: 720px
  width: 100%
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2)

.home-cta__title
  margin: 0
  text-align: center
  font-size: clamp(1.8rem, 4vw, 2.5rem)

.home-cta__subtitle
  margin: 0
  text-align: center
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-cta__actions
  display: flex
  flex-direction: column
  gap: 1.25rem
  align-items: center

.home-cta__links
  display: flex
  flex-direction: column
  gap: 1rem
  align-items: center

.home-cta__form
  width: 100%
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-cta__search-input :deep(.v-field)
  border-radius: 999px

.home-cta__submit
  border-radius: 999px
  box-shadow: none

.home-cta__browse
  border-radius: 999px
  padding-inline: clamp(1.75rem, 5vw, 2.5rem)

.home-cta__link
  text-decoration: none
  font-weight: 600
  color: rgba(var(--v-theme-hero-gradient-start), 0.9)

@media (min-width: 960px)
  .home-hero__layout
    grid-template-columns: repeat(2, minmax(0, 1fr))
    align-items: center

  .home-hero__search
    flex-direction: row
    align-items: center

  .home-hero__helper
    font-size: 1rem

  .home-solution__grid
    column-gap: 1.5rem

  .home-blog__actions
    justify-content: flex-end

  .home-cta__links
    flex-direction: row
    align-items: center
    gap: 1.5rem

  .home-cta__form
    flex-direction: row
    align-items: center
    max-width: 500px
</style>
