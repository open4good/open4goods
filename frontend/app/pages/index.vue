<script setup lang="ts">
import { computed, ref } from 'vue'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'
import type { BlogPostDto } from '~~/shared/api-client'
import HomeHeroSection from '~/components/home/sections/HomeHeroSection.vue'
import HomeProblemsSection from '~/components/home/sections/HomeProblemsSection.vue'
import HomeSolutionSection from '~/components/home/sections/HomeSolutionSection.vue'
import HomeFeaturesSection from '~/components/home/sections/HomeFeaturesSection.vue'
import HomeBlogSection from '~/components/home/sections/HomeBlogSection.vue'
import HomeObjectionsSection from '~/components/home/sections/HomeObjectionsSection.vue'
import HomeFaqSection from '~/components/home/sections/HomeFaqSection.vue'
import HomeCtaSection from '~/components/home/sections/HomeCtaSection.vue'
import NudgeToolWizard from '~/components/nudge-tool/NudgeToolWizard.vue'
import type { CategorySuggestionItem, ProductSuggestionItem } from '~/components/search/SearchSuggestField.vue'
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
const heroVideoSrc = useState<string>('home-hero-video-src', () => '/videos/video-concept1.mp4')
const heroVideoPoster = '/images/home/hero-placeholder.svg'

type HomeBlogItem = BlogPostDto & { formattedDate?: string; slug?: string }
type EnrichedBlogItem = HomeBlogItem & { link: string; hasImage: boolean }

const { categories: rawCategories, fetchCategories } = useCategories()
const { paginatedArticles, fetchArticles, loading: blogLoading } = useBlog()

const BLOG_ARTICLES_LIMIT = 4

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
  let previous
  let input = value
  do {
    previous = input
    input = input.replace(/<!--[\s\S]*?-->/g, '')
  } while (input !== previous)
  return input
}

const sanitizeBlogSummary = (value: unknown) => stripHtmlComments(toSafeString(value)).trim()

const hasRenderableImage = (value: unknown): value is string =>
  typeof value === 'string' && value.trim().length > 0

if (import.meta.server) {
  await fetchCategories(true)
  await fetchArticles(1, BLOG_ARTICLES_LIMIT, null)

  const { getHeroVideoSources } = await import('~~/server/utils/hero-videos')
  const videoCandidates = await getHeroVideoSources()

  if (videoCandidates.length > 0) {
    const randomIndex = Math.floor(Math.random() * videoCandidates.length)
    heroVideoSrc.value = videoCandidates[randomIndex]
  }
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
    icon: 'mdi-table-multiple',
    text: String(t('home.problems.items.tooManyTabs')),
  },
])

const solutionBenefits = computed(() => [
  {
    emoji: '⏱️',
    label: String(t('home.solution.benefits.time.title')),
    description: String(t('home.solution.benefits.time.description')),
  },
  {
    emoji: '💰',
    label: String(t('home.solution.benefits.savings.title')),
    description: String(t('home.solution.benefits.savings.description')),
  },
  {
    emoji: '🌍',
    label: String(t('home.solution.benefits.planet.title')),
    description: String(t('home.solution.benefits.planet.description')),
  },
  {
    emoji: '🛡️',
    label: String(t('home.solution.benefits.trust.title')),
    description: String(t('home.solution.benefits.trust.description')),
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

const siteName = computed(() => String(t('siteIdentity.siteName')))
const logoUrl = computed(() => new URL('/nudger-icon-512x512.png', requestURL.origin).toString())

const localizedHomeUrls = computed(() =>
  Array.from(new Set([canonicalUrl.value, ...alternateLinks.value.map((link) => link.href)])),
)

const linkedinUrl = computed(() => toTrimmedString(t('siteIdentity.links.linkedin')))

const organizationJsonLd = computed(() => ({
  '@context': 'https://schema.org',
  '@type': 'Organization',
  name: siteName.value,
  url: canonicalUrl.value,
  logo: logoUrl.value,
  sameAs: Array.from(new Set([linkedinUrl.value, ...localizedHomeUrls.value].filter(Boolean))),
}))

const searchPageUrl = computed(
  () => new URL(resolveLocalizedRoutePath('search', locale.value), requestURL.origin).toString(),
)

const websiteJsonLd = computed(() => ({
  '@context': 'https://schema.org',
  '@type': 'WebSite',
  name: siteName.value,
  url: canonicalUrl.value,
  inLanguage: locale.value,
  potentialAction: {
    '@type': 'SearchAction',
    target: `${searchPageUrl.value}?q={search_term_string}`,
    'query-input': 'required name=q',
  },
  sameAs: localizedHomeUrls.value,
}))

const categoriesLandingUrl = computed(() => localePath({ name: 'categories' }))

const normalizeVerticalHomeUrl = (raw: string | null | undefined): string | null => {
  if (!raw) {
    return null
  }

  const trimmed = raw.trim()

  if (!trimmed) {
    return null
  }

  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed
  }

  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`
}

const blogDateFormatter = computed(
  () => new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium' }),
)

const fallbackBlogEntries = computed<HomeBlogItem[]>(() => [
  {
    url: localePath({ name: 'blog-slug', params: { slug: 'impact-score-ia' } }),
    title: String(t('home.blog.items.first.title')),
    summary: String(t('home.blog.items.first.excerpt')),
    formattedDate: String(t('home.blog.items.first.date')),
    slug: 'impact-score-ia',
  },
  {
    url: localePath({ name: 'blog-slug', params: { slug: 'prioriser-durabilite' } }),
    title: String(t('home.blog.items.second.title')),
    summary: String(t('home.blog.items.second.excerpt')),
    formattedDate: String(t('home.blog.items.second.date')),
    slug: 'prioriser-durabilite',
  },
  {
    url: localePath({ name: 'blog-slug', params: { slug: 'equilibrer-prix-impact' } }),
    title: String(t('home.blog.items.third.title')),
    summary: String(t('home.blog.items.third.excerpt')),
    formattedDate: String(t('home.blog.items.third.date')),
    slug: 'equilibrer-prix-impact',
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
      slug: toTrimmedString((article as { slug?: string }).slug) || undefined,
    }
  })
})

const extractBlogSlug = (article: BlogPostDto): string | null => {
  const pickSlugSegment = (value: unknown): string | null => {
    if (typeof value !== 'string') {
      return null
    }

    const trimmed = value.trim()

    if (!trimmed) {
      return null
    }

    let working = trimmed

    if (/^https?:\/\//i.test(trimmed)) {
      try {
        const parsedUrl = new URL(trimmed)
        working = parsedUrl.pathname
      } catch {
        return null
      }
    }

    const sanitized = working.replace(/[?#].*$/, '').replace(/^\/+/, '')

    if (!sanitized) {
      return null
    }

    const segments = sanitized.split('/').filter(Boolean)
    const candidate = segments.pop()

    if (!candidate || candidate.toLowerCase() === 'blog') {
      return null
    }

    return candidate
  }

  return pickSlugSegment((article as { slug?: string }).slug) ?? pickSlugSegment(article.url)
}

const resolveBlogArticleLink = (article: BlogPostDto) => {
  const slug = extractBlogSlug(article)

  if (!slug) {
    return localePath({ name: 'blog' })
  }

  return localePath({ name: 'blog-slug', params: { slug } })
}

const enrichedBlogItems = computed<EnrichedBlogItem[]>(() =>
  blogListItems.value.map((item) => {
    const link = resolveBlogArticleLink(item)
    const hasImage = hasRenderableImage(item.image)

    return {
      ...item,
      link,
      hasImage,
    }
  }),
)

const featuredBlogItem = computed(() => enrichedBlogItems.value[0] ?? null)
const secondaryBlogItems = computed(() => enrichedBlogItems.value.slice(1))

const ogImageUrl = computed(() => logoUrl.value)

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

  const normalizedUrl = normalizeVerticalHomeUrl(suggestion.url)

  if (normalizedUrl) {
    router.push(normalizedUrl)
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
  ogSiteName: () => siteName.value,
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
    {
      key: 'home-organization-jsonld',
      type: 'application/ld+json',
      children: JSON.stringify(organizationJsonLd.value),
    },
    {
      key: 'home-website-jsonld',
      type: 'application/ld+json',
      children: JSON.stringify(websiteJsonLd.value),
    },
  ],
}))
</script>

<template>
  <div class="home-page">
    <HomeHeroSection
      v-model:search-query="searchQuery"
      :min-suggestion-query-length="MIN_SUGGESTION_QUERY_LENGTH"
      :hero-video-src="heroVideoSrc"
      :hero-video-poster="heroVideoPoster"
      @submit="handleSearchSubmit"
      @select-category="handleCategorySuggestion"
      @select-product="handleProductSuggestion"
    />
    <section class="home-page__nudge-tool">
      <v-container class="pa-0 pa-md-6">
        <v-row class="align-center" :gutter="0">
          <v-col cols="12" md="4" class="pe-md-6 pb-6 pb-md-0">
            <p class="home-page__nudge-eyebrow">{{ t('home.nudgeTool.eyebrow') }}</p>
            <h2 class="home-page__nudge-title">{{ t('home.nudgeTool.title') }}</h2>
            <p class="home-page__nudge-subtitle">{{ t('home.nudgeTool.subtitle') }}</p>
          </v-col>
          <v-col cols="12" md="8">
            <NudgeToolWizard :verticals="rawCategories" />
          </v-col>
        </v-row>
      </v-container>
    </section>
    <div class="home-page__sections">
      <HomeProblemsSection :items="problemItems" />

      <HomeSolutionSection :benefits="solutionBenefits" />

      <HomeFeaturesSection :features="featureCards" />

      <HomeBlogSection
        :loading="blogLoading"
        :featured-item="featuredBlogItem"
        :secondary-items="secondaryBlogItems"
      />

      <HomeObjectionsSection :items="objectionItems" />

      <HomeFaqSection :items="faqPanels" />

      <HomeCtaSection
        v-model:search-query="searchQuery"
        :categories-landing-url="categoriesLandingUrl"
        :min-suggestion-query-length="MIN_SUGGESTION_QUERY_LENGTH"
        @submit="handleSearchSubmit"
        @select-category="handleCategorySuggestion"
        @select-product="handleProductSuggestion"
      />
    </div>
  </div>
</template>

<style scoped lang="sass">
.home-page
  --cat-height: 150px
  --cat-in-hero: calc(var(--cat-height) / 2)
  --cat-overlap: calc(var(--cat-height) - var(--cat-in-hero))
  display: flex
  flex-direction: column
  gap: 0
  background-color: rgb(var(--v-theme-surface-default))

.home-page__nudge-tool
  background-color: rgb(var(--v-theme-surface-primary-050))
  padding: clamp(2.5rem, 6vw, 4rem) 0

.home-page__nudge-eyebrow
  display: inline-flex
  align-items: center
  gap: 8px
  margin: 0 0 8px
  padding: 6px 12px
  font-weight: 700
  color: rgb(var(--v-theme-text-on-accent))
  background: linear-gradient(90deg, rgba(var(--v-theme-hero-gradient-start), 0.8), rgba(var(--v-theme-hero-gradient-end), 0.9))
  border-radius: 999px
  width: fit-content

.home-page__nudge-title
  margin: 0 0 8px
  font-size: clamp(1.6rem, 2.5vw, 2rem)
  color: rgb(var(--v-theme-text-neutral-strong))
  font-weight: 800

.home-page__nudge-subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-size: clamp(1rem, 1.6vw, 1.1rem)

.home-page__sections
  display: flex
  flex-direction: column
  gap: clamp(3rem, 7vw, 5rem)
  padding-top: var(--cat-overlap)
</style>
