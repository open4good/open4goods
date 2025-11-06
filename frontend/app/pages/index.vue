<script setup lang="ts">
import { computed, ref } from 'vue'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'
import type { BlogPostDto, VerticalConfigDto } from '~~/shared/api-client'
import SearchSuggestField, {
  type CategorySuggestionItem,
  type ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'
import HomeCategoryCarousel from '~/components/home/HomeCategoryCarousel.vue'
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
type EnrichedBlogItem = HomeBlogItem & { link: string; isExternal: boolean }

const { categories: rawCategories, fetchCategories, loading: categoriesLoading } = useCategories()
const { paginatedArticles, fetchArticles, loading: blogLoading } = useBlog()

const BLOG_ARTICLES_LIMIT = 10

const toSafeString = (value: unknown) => {
  if (typeof value === 'string') {
    return value
  }

  if (value == null) {
    return ''
  }

  return String(value)
}

const toTrimmedString = (value: unknown) => toSafeString(value).trim()

const stripHtmlComments = (value: string) => {
  let previous;
  let input = value;
  do {
    previous = input;
    input = input.replace(/<!--[\s\S]*?-->/g, '');
  } while (input !== previous);
  return input;
};

const sanitizeBlogSummary = (value: unknown) => stripHtmlComments(toSafeString(value)).trim()

if (import.meta.server) {
  await fetchCategories(true)
  await fetchArticles(1, BLOG_ARTICLES_LIMIT, null)
} else {
  if (rawCategories.value.length === 0) {
    await fetchCategories(true)
  }

  if (paginatedArticles.value.length === 0) {
    await fetchArticles(1, BLOG_ARTICLES_LIMIT, null)
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

const resolveCategoryImage = (category: VerticalConfigDto) => {
  const candidates = [
    category.imageLarge,
    category.imageMedium,
    category.imageSmall,
  ]

  for (const candidate of candidates) {
    if (typeof candidate === 'string') {
      const trimmed = candidate.trim()
      if (trimmed.length > 0) {
        return trimmed
      }
    }
  }

  return null
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
      image: resolveCategoryImage(category),
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

const blogListItems = computed<HomeBlogItem[]>(() => {
  const articles = paginatedArticles.value.slice(0, BLOG_ARTICLES_LIMIT)

  if (!articles.length) {
    return fallbackBlogEntries.value
  }

  return articles.map((article) => {
    const formattedDate = article.createdMs
      ? blogDateFormatter.value.format(new Date(article.createdMs))
      : ''

    const title = toTrimmedString(article.title)
    const summary = sanitizeBlogSummary(article.summary)
    const image = typeof article.image === 'string' ? article.image.trim() : null

    return {
      ...article,
      title,
      summary,
      image,
      formattedDate,
    }
  })
})

const resolveBlogArticleLink = (article: BlogPostDto) => {
  const rawUrl = toTrimmedString(article.url)

  if (!rawUrl) {
    return localePath({ name: 'blog' })
  }

  if (/^https?:\/\//i.test(rawUrl)) {
    try {
      const parsedUrl = new URL(rawUrl)
      const slug = parsedUrl.pathname.split('/').filter(Boolean).pop()
      if (slug) {
        return localePath({ name: 'blog-slug', params: { slug } })
      }
    } catch (error) {
      console.warn('Failed to parse blog article URL', rawUrl, error)
    }

    return rawUrl
  }

  if (rawUrl.startsWith('/')) {
    return rawUrl
  }

  return localePath({ name: 'blog-slug', params: { slug: rawUrl } })
}

const enrichedBlogItems = computed<EnrichedBlogItem[]>(() =>
  blogListItems.value.map((item) => {
    const link = resolveBlogArticleLink(item)
    const isExternal = /^https?:\/\//i.test(link)

    return {
      ...item,
      link,
      isExternal,
    }
  }),
)

const featuredBlogItem = computed(() => enrichedBlogItems.value[0] ?? null)
const secondaryBlogItems = computed(() => enrichedBlogItems.value.slice(1))

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
      <v-container class="home-hero__container" fluid>
        <div class="home-hero__inner">
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
        </div>
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

    <section class="home-section home-problems" aria-labelledby="home-problems-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-problems-title">{{ t('home.problems.title') }}</h2>
        </header>
        <div class="home-problems__grid">
          <article
            v-for="(item, index) in problemItems"
            :key="item.text"
            class="home-problems__card"
            :class="{ 'home-problems__card--reverse': index % 2 === 1 }"
          >
            <div class="home-problems__card-inner">
              <div class="home-problems__icon-wrapper" aria-hidden="true">
                <v-icon class="home-problems__icon" :icon="item.icon" size="56" />
              </div>
              <p class="home-problems__text">{{ item.text }}</p>
            </div>
          </article>
        </div>
      </v-container>
    </section>

    <section class="home-section home-solution" aria-labelledby="home-solution-title">
      <v-container class="home-section__container home-solution__container" fluid>
        <div class="home-section__inner">
          <header class="home-section__header">
            <h2 id="home-solution-title">{{ t('home.solution.title') }}</h2>
            <p class="home-section__subtitle">{{ t('home.solution.description') }}</p>
          </header>
          <ul class="home-solution__list">
            <li
              v-for="(item, index) in solutionBenefits"
              :key="item.label"
              class="home-solution__item"
              :class="{ 'home-solution__item--reverse': index % 2 === 1 }"
            >
              <v-card class="home-solution__card" variant="outlined">
                <span class="home-solution__emoji" aria-hidden="true">{{ item.emoji }}</span>
                <p class="home-solution__label">{{ item.label }}</p>
              </v-card>
            </li>
          </ul>
        </div>
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

    <section class="home-section home-blog" aria-labelledby="home-blog-title">
      <v-container class="home-section__container" max-width="lg">
        <header class="home-section__header">
          <h2 id="home-blog-title">{{ t('home.blog.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.blog.subtitle') }}</p>
        </header>
        <div v-if="blogLoading" class="home-blog__skeletons" aria-hidden="true">
          <v-skeleton-loader
            v-for="index in 4"
            :key="`blog-skeleton-${index}`"
            type="image, list-item"
            class="home-blog__skeleton"
          />
        </div>
        <div v-else-if="featuredBlogItem || secondaryBlogItems.length" class="home-blog__grid">
          <component
            :is="featuredBlogItem?.isExternal ? 'a' : NuxtLink"
            v-if="featuredBlogItem"
            :key="'featured-article'"
            class="home-blog__item home-blog__item--featured"
            :href="featuredBlogItem.isExternal ? featuredBlogItem.link : undefined"
            :to="!featuredBlogItem.isExternal ? featuredBlogItem.link : undefined"
            :target="featuredBlogItem.isExternal ? '_blank' : undefined"
            :rel="featuredBlogItem.isExternal ? 'noopener' : undefined"
          >
            <article class="home-blog__card">
              <div class="home-blog__media" aria-hidden="true">
                <v-img
                  v-if="featuredBlogItem.image"
                  :src="featuredBlogItem.image"
                  :alt="featuredBlogItem.title ?? ''"
                  cover
                />
                <div v-else class="home-blog__placeholder">
                  <v-icon icon="mdi-post-outline" size="68" />
                </div>
              </div>
              <div class="home-blog__content">
                <p class="home-blog__date">{{ featuredBlogItem.formattedDate }}</p>
                <h3 class="home-blog__title">{{ featuredBlogItem.title }}</h3>
                <p class="home-blog__summary">{{ featuredBlogItem.summary }}</p>
                <span class="home-blog__link-label">{{ t('home.blog.readMore') }}</span>
              </div>
            </article>
          </component>
          <component
            :is="article.isExternal ? 'a' : NuxtLink"
            v-for="article in secondaryBlogItems"
            :key="article.url ?? article.title ?? article.link"
            class="home-blog__item"
            :href="article.isExternal ? article.link : undefined"
            :to="!article.isExternal ? article.link : undefined"
            :target="article.isExternal ? '_blank' : undefined"
            :rel="article.isExternal ? 'noopener' : undefined"
          >
            <article class="home-blog__card">
              <div class="home-blog__media" aria-hidden="true">
                <v-img
                  v-if="article.image"
                  :src="article.image"
                  :alt="article.title ?? ''"
                  cover
                />
                <div v-else class="home-blog__placeholder">
                  <v-icon icon="mdi-post-outline" size="48" />
                </div>
              </div>
              <div class="home-blog__content">
                <p class="home-blog__date">{{ article.formattedDate }}</p>
                <h3 class="home-blog__title">{{ article.title }}</h3>
                <p class="home-blog__summary">{{ article.summary }}</p>
                <span class="home-blog__link-label">{{ t('home.blog.readMore') }}</span>
              </div>
            </article>
          </component>
        </div>
        <v-alert
          v-else
          type="info"
          variant="tonal"
          border="start"
          class="home-blog__empty"
        >
          {{ t('home.blog.emptyState') }}
        </v-alert>
        <div class="home-blog__actions">
          <v-btn
            class="home-blog__cta"
            :to="localePath({ name: 'blog' })"
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
  padding-inline: clamp(1.5rem, 4vw, 3rem)

.home-section__inner
  max-width: 1120px
  margin: 0 auto
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
  padding-inline: clamp(1.5rem, 4vw, 4rem)

.home-hero__inner
  max-width: 1120px
  margin: 0 auto
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

.home-problems__grid
  display: grid
  gap: clamp(1.5rem, 4vw, 2.5rem)
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr))

@media (min-width: 960px)
  .home-problems__grid
    grid-template-columns: repeat(2, minmax(0, 1fr))

  .home-problems__card
    max-width: 520px

  .home-problems__card:not(.home-problems__card--reverse)
    margin-inline-end: auto

  .home-problems__card--reverse
    margin-inline-start: auto

.home-problems__card
  position: relative
  transition: transform 0.25s ease, box-shadow 0.25s ease

.home-problems__card::before
  content: ''
  position: absolute
  inset: 0
  border-radius: clamp(1.5rem, 4vw, 2.25rem)
  background: rgba(var(--v-theme-surface-primary-080), 0.52)
  transform: translate(12px, 12px)
  z-index: 0

.home-problems__card--reverse::before
  transform: translate(-12px, 12px)

.home-problems__card-inner
  position: relative
  z-index: 1
  display: flex
  align-items: center
  gap: 1.5rem
  padding: clamp(1.75rem, 4vw, 2.5rem)
  border-radius: clamp(1.5rem, 4vw, 2.25rem)
  background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.18), rgba(var(--v-theme-hero-gradient-end), 0.16))
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)
  box-shadow: 0 20px 36px rgba(var(--v-theme-shadow-primary-600), 0.16)

.home-problems__card:hover
  transform: translateY(-6px)
  box-shadow: 0 26px 40px rgba(var(--v-theme-shadow-primary-600), 0.18)

.home-problems__icon-wrapper
  display: flex
  align-items: center
  justify-content: center
  width: clamp(72px, 12vw, 88px)
  height: clamp(72px, 12vw, 88px)
  border-radius: 28px
  background: radial-gradient(circle at top, rgba(var(--v-theme-hero-gradient-start), 0.38), rgba(var(--v-theme-hero-gradient-end), 0.22))
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.4)

.home-problems__icon
  color: rgba(var(--v-theme-hero-gradient-start), 0.95)

.home-problems__text
  margin: 0
  font-size: clamp(1.05rem, 2.4vw, 1.3rem)
  line-height: 1.45
  color: rgb(var(--v-theme-text-neutral-strong))

.home-solution__list
  list-style: none
  display: grid
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr))
  gap: clamp(1.5rem, 4vw, 2.5rem)
  margin: 0
  padding: 0

.home-solution__item
  position: relative

.home-solution__item::before
  content: ''
  position: absolute
  inset: 10px 16px 0
  border-radius: clamp(1.5rem, 4vw, 2.25rem)
  background: rgba(var(--v-theme-surface-primary-050), 0.6)
  z-index: 0

.home-solution__item--reverse::before
  inset-inline: 16px 10px

@media (max-width: 599px)
  .home-solution__item::before
    display: none

.home-solution__card
  position: relative
  z-index: 1
  height: 100%
  display: flex
  flex-direction: column
  gap: 1rem
  padding: clamp(1.75rem, 4vw, 2.5rem)
  border-radius: clamp(1.5rem, 4vw, 2.25rem)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.32)
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-080), 0.92), rgba(var(--v-theme-surface-default), 0.96))
  box-shadow: 0 18px 30px rgba(var(--v-theme-shadow-primary-600), 0.14)

.home-solution__item--reverse .home-solution__card
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-default), 0.96), rgba(var(--v-theme-surface-primary-080), 0.92))

.home-solution__emoji
  font-size: clamp(2.5rem, 7vw, 3.6rem)
  line-height: 1

.home-solution__label
  margin: 0
  font-size: clamp(1.1rem, 2.6vw, 1.45rem)
  font-weight: 600
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

.home-blog__skeletons
  display: grid
  gap: 1.5rem
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr))
  margin-top: clamp(1rem, 3vw, 1.5rem)

.home-blog__skeleton
  border-radius: clamp(1.25rem, 3vw, 1.75rem)

.home-blog__grid
  display: grid
  gap: clamp(1.5rem, 4vw, 2.5rem)
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr))
  margin-top: clamp(1rem, 3vw, 1.75rem)

.home-blog__item
  text-decoration: none
  color: inherit

.home-blog__empty
  border-radius: clamp(1.25rem, 3vw, 1.75rem)
  margin-top: clamp(1rem, 3vw, 1.5rem)

@media (min-width: 960px)
  .home-blog__item--featured
    grid-column: span 2
    grid-row: span 2

.home-blog__card
  height: 100%
  display: flex
  flex-direction: column
  border-radius: clamp(1.25rem, 3vw, 1.85rem)
  overflow: hidden
  background: rgba(var(--v-theme-surface-default), 0.96)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.28)
  box-shadow: 0 18px 28px rgba(var(--v-theme-shadow-primary-600), 0.12)
  transition: transform 0.25s ease, box-shadow 0.25s ease

.home-blog__card:hover
  transform: translateY(-6px)
  box-shadow: 0 26px 40px rgba(var(--v-theme-shadow-primary-600), 0.16)

.home-blog__media
  position: relative
  aspect-ratio: 16 / 10
  background: rgba(var(--v-theme-surface-primary-050), 0.8)

.home-blog__media :deep(.v-img)
  width: 100%
  height: 100%

.home-blog__media :deep(img)
  object-fit: cover

.home-blog__placeholder
  display: flex
  align-items: center
  justify-content: center
  height: 100%
  color: rgba(var(--v-theme-hero-gradient-start), 0.85)

.home-blog__content
  display: flex
  flex-direction: column
  gap: 0.75rem
  padding: clamp(1.5rem, 4vw, 2.25rem)

.home-blog__date
  margin: 0
  font-size: 0.9rem
  letter-spacing: 0.04em
  text-transform: uppercase
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

.home-blog__title
  margin: 0
  font-size: clamp(1.1rem, 2.5vw, 1.4rem)
  color: rgb(var(--v-theme-text-neutral-strong))
  font-weight: 600

.home-blog__summary
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-size: clamp(0.95rem, 2.2vw, 1.05rem)
  line-height: 1.6

.home-blog__link-label
  margin-top: auto
  font-weight: 600
  color: rgba(var(--v-theme-hero-gradient-end), 0.9)

.home-blog__actions
  display: flex
  justify-content: center
  margin-top: clamp(1.75rem, 4vw, 2.75rem)

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
