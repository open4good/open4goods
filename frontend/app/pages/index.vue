<script setup lang="ts">
import { computed, ref } from 'vue'
import { usePreferredReducedMotion } from '@vueuse/core'
import { storeToRefs } from 'pinia'
import { useTheme } from 'vuetify'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'
import type {
  AffiliationPartnerDto,
  BlogPostDto,
  CategoriesStatsDto,
  IpQuotaStatusDto,
} from '~~/shared/api-client'
import HomeHeroSection from '~/components/home/sections/HomeHeroSection.vue'
import HomeProblemsSection from '~/components/home/sections/HomeProblemsSection.vue'
import HomeSolutionSection from '~/components/home/sections/HomeSolutionSection.vue'
import HomeBlogSection from '~/components/home/sections/HomeBlogSection.vue'
import HomeFaqSection from '~/components/home/sections/HomeFaqSection.vue'
import HomeCtaSection from '~/components/home/sections/HomeCtaSection.vue'
import ParallaxWidget from '~/components/shared/ui/ParallaxWidget.vue'
import SectionReveal from '~/components/shared/ui/SectionReveal.vue'
import type {
  CategorySuggestionItem,
  ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'
import { useCategories } from '~/composables/categories/useCategories'
import { useBlog } from '~/composables/blog/useBlog'
import { useParallaxConfig } from '~/composables/useParallaxConfig'
import { useSeasonalEventPack } from '~/composables/useSeasonalEventPack'
import {
  resolveThemedAssetUrl,
  useThemeAsset,
} from '~/composables/useThemedAsset'
import { useThemedParallaxBackgrounds } from '~/composables/useThemedParallaxBackgrounds'
import { useAccessibilityStore } from '~/stores/useAccessibilityStore'
import {
  PARALLAX_SECTION_KEYS,
  THEME_ASSETS_FALLBACK,
  type ParallaxLayerConfig,
  type ParallaxSectionKey,
} from '~~/config/theme/assets'
import { EVENT_PACK_I18N_BASE_KEY } from '~~/config/theme/event-packs'
import { resolveThemeName } from '~~/shared/constants/theme'
import PwaMobileLanding from '~/components/pwa/PwaMobileLanding.vue'

definePageMeta({
  ssr: true,
})

const { t, locale, availableLocales } = useI18n()
const router = useRouter()
const localePath = useLocalePath()
const requestURL = useRequestURL()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const searchQuery = ref('')

const MIN_SUGGESTION_QUERY_LENGTH = 2

type HomeBlogItem = BlogPostDto & { formattedDate?: string; slug?: string }
type EnrichedBlogItem = HomeBlogItem & { link: string; hasImage: boolean }

const { categories: rawCategories, fetchCategories } = useCategories()
const { paginatedArticles, fetchArticles, loading: blogLoading } = useBlog()

const BLOG_ARTICLES_LIMIT = 3

const { data: categoriesStats } = await useAsyncData<CategoriesStatsDto | null>(
  'home-categories-stats',
  () =>
    $fetch<CategoriesStatsDto>('/api/stats/categories', {
      headers: requestHeaders,
    }).catch(() => null),
  {
    default: () => null,
    server: true,
  }
)

const { data: affiliationPartners } = await useAsyncData<
  AffiliationPartnerDto[]
>(
  'home-affiliation-partners',
  () =>
    $fetch<AffiliationPartnerDto[]>('/api/partners/affiliation', {
      headers: requestHeaders,
    }).catch(() => []),
  {
    default: () => [],
    server: true,
  }
)

const { data: reviewQuotaStatus } = await useAsyncData<IpQuotaStatusDto | null>(
  'home-review-quota',
  () =>
    $fetch<IpQuotaStatusDto>('/api/quotas/REVIEW_GENERATION', {
      headers: requestHeaders,
    }).catch(() => null),
  {
    default: () => null,
    server: true,
  }
)

const heroPartnersCount = computed(() => {
  const statsCount = categoriesStats.value?.affiliationPartnersCount

  if (typeof statsCount === 'number' && Number.isFinite(statsCount)) {
    return statsCount
  }

  return affiliationPartners.value?.length ?? 0
})

const openDataMillions = computed(() => {
  const gtinCount = categoriesStats.value?.gtinOpenDataItemsCount
  const isbnCount = categoriesStats.value?.isbnOpenDataItemsCount
  const resolvedGtinCount =
    typeof gtinCount === 'number' && Number.isFinite(gtinCount) ? gtinCount : 0
  const resolvedIsbnCount =
    typeof isbnCount === 'number' && Number.isFinite(isbnCount) ? isbnCount : 0
  const totalCount = resolvedGtinCount + resolvedIsbnCount

  if (totalCount <= 0) {
    return null
  }

  const roundedMillions = Math.round(totalCount / 1_000_000)

  return roundedMillions > 0 ? roundedMillions : null
})

const impactScoreProductsCount = computed(() => {
  const count = categoriesStats.value?.impactScoreProductsCount

  if (typeof count !== 'number' || !Number.isFinite(count) || count < 0) {
    return null
  }

  return count
})

const impactScoreCategoriesCount = computed(() => {
  const detailedStats = categoriesStats.value?.detailedStats

  if (!detailedStats) {
    return null
  }

  const count = Object.values(detailedStats).filter(
    stats => (stats.ratedProducts ?? 0) > 0
  ).length

  return count
})

const productsWithoutVerticalCount = computed(() => {
  const count = categoriesStats.value?.productsWithoutVerticalCount

  if (typeof count !== 'number' || !Number.isFinite(count) || count <= 0) {
    return null
  }

  return count
})

const reviewedProductsCount = computed(() => {
  const count = categoriesStats.value?.reviewedProductsCount

  if (typeof count !== 'number' || !Number.isFinite(count) || count < 0) {
    return null
  }

  return count
})

const productsCount = computed(() => {
  const count = categoriesStats.value?.productsCountSum

  if (typeof count !== 'number' || !Number.isFinite(count) || count <= 0) {
    return null
  }

  return count
})

const categoriesCount = computed(() => {
  const count = rawCategories.value?.length ?? 0

  if (count > 0) {
    return count
  }

  const fallbackCount = categoriesStats.value?.enabledVerticalConfigs ?? 0

  return fallbackCount > 0 ? fallbackCount : null
})

const aiSummaryRemainingCredits = computed(() => {
  const remaining = reviewQuotaStatus.value?.remaining

  if (typeof remaining !== 'number' || !Number.isFinite(remaining)) {
    return null
  }

  return remaining
})

const seasonalEventPack = useSeasonalEventPack()
const theme = useTheme()
const parallaxAplatFallback = useThemeAsset('parallaxAplat')

const parallaxConfig = useParallaxConfig()
const parallaxLayers = useThemedParallaxBackgrounds(seasonalEventPack)

type ParallaxSectionRenderConfig = {
  backgrounds: ParallaxLayerConfig[]
  overlayOpacity: number
  parallaxAmount: number
  maxOffsetRatio: number | null
}

const parallaxBackgrounds = computed<
  Record<ParallaxSectionKey, ParallaxSectionRenderConfig>
>(() =>
  PARALLAX_SECTION_KEYS.reduce<
    Record<ParallaxSectionKey, ParallaxSectionRenderConfig>
  >(
    (acc, section) => ({
      ...acc,
      [section]: {
        backgrounds: parallaxLayers.value[section] || [],
        overlayOpacity: parallaxConfig.value[section].overlay,
        parallaxAmount: parallaxConfig.value[section].parallaxAmount,
        maxOffsetRatio: parallaxConfig.value[section].maxOffsetRatio,
      },
    }),
    {} as Record<ParallaxSectionKey, ParallaxSectionRenderConfig>
  )
)

const themeName = computed(() =>
  resolveThemeName(theme.global.name.value, THEME_ASSETS_FALLBACK)
)

const parallaxBackgroundKeys = computed<Record<ParallaxSectionKey, string>>(
  () =>
    PARALLAX_SECTION_KEYS.reduce<Record<ParallaxSectionKey, string>>(
      (acc, section) => ({
        ...acc,
        [section]: `${EVENT_PACK_I18N_BASE_KEY}.${seasonalEventPack.value}.parallax.${section}`,
      }),
      {} as Record<ParallaxSectionKey, string>
    )
)

const parallaxAplatKey = computed(
  () => `${EVENT_PACK_I18N_BASE_KEY}.${seasonalEventPack.value}.parallax.aplat`
)

const aplatSuffixes = ['left', 'center', 'right']

const getRandomSuffix = () =>
  aplatSuffixes[Math.floor(Math.random() * aplatSuffixes.length)]

const essentialsAplatSuffix = useState('home-aplat-essentials', getRandomSuffix)
const blogAplatSuffix = useState('home-aplat-blog', getRandomSuffix)

const resolveAplatVariant = (suffix: string) => {
  const name = `parallax/parallax-aplats-${suffix}.svg`
  return (
    resolveThemedAssetUrl(name, themeName.value, seasonalEventPack.value) ??
    parallaxAplatFallback.value
  )
}

const parallaxAplatEssentials = computed(() =>
  resolveAplatVariant(essentialsAplatSuffix.value)
)
const parallaxAplatBlog = computed(() =>
  resolveAplatVariant(blogAplatSuffix.value)
)

const prefersReducedMotion = usePreferredReducedMotion()
const accessibilityStore = useAccessibilityStore()
const { prefersReducedMotionOverride } = storeToRefs(accessibilityStore)
const shouldReduceMotion = computed(
  () =>
    prefersReducedMotionOverride.value ||
    prefersReducedMotion.value === 'reduce'
)

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

const sanitizeBlogSummary = (value: unknown) =>
  stripHtmlComments(toSafeString(value)).trim()

const hasRenderableImage = (value: unknown): value is string =>
  typeof value === 'string' && value.trim().length > 0

const { execute: _executeCategoriesFetch } = await useAsyncData(
  'home-categories-data',
  async () => {
    if (rawCategories.value.length === 0) {
      await fetchCategories()
    }
    return true
  },
  {
    server: true,
  }
)

const { execute: _executeBlogFetch } = await useAsyncData(
  'home-blog-data',
  async () => {
    if (paginatedArticles.value.length === 0) {
      await fetchArticles(1, BLOG_ARTICLES_LIMIT, null)
    }
    return true
  },
  {
    lazy: true,
    server: false,
  }
)

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
    question: String(t('home.faq.items.impactScore.question')),
    answer: String(t('home.faq.items.impactScore.answer')),
    ctaLabel: String(t('home.faq.items.impactScore.cta')),
    ctaAria: String(t('home.faq.items.impactScore.ctaAria')),
    isImpactScore: true,
  },
  {
    question: String(t('home.faq.items.dataFreshness.question')),
    answer: String(t('home.faq.items.dataFreshness.answer')),
  },
  {
    question: String(t('home.faq.items.contact.question')),
    answer: '',
    isContact: true,
  },
])

const faqPanels = computed(() =>
  faqItems.value.map((item, index) => ({
    ...item,
    blocId: `HOME:FAQ:${index + 1}`,
  }))
)

const faqJsonLd = computed(() => ({
  '@context': 'https://schema.org',
  '@type': 'FAQPage',
  mainEntity: faqItems.value.map(item => ({
    '@type': 'Question',
    name: item.question,
    acceptedAnswer: {
      '@type': 'Answer',
      text: item.answer,
    },
  })),
}))

const canonicalUrl = computed(() =>
  new URL(
    resolveLocalizedRoutePath('index', locale.value),
    requestURL.origin
  ).toString()
)

const alternateLinks = computed(() =>
  availableLocales.map(availableLocale => ({
    rel: 'alternate' as const,
    hreflang: availableLocale,
    href: new URL(
      resolveLocalizedRoutePath('index', availableLocale),
      requestURL.origin
    ).toString(),
  }))
)

const siteName = computed(() => String(t('siteIdentity.siteName')))
const logoUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)

const localizedHomeUrls = computed(() =>
  Array.from(
    new Set([
      canonicalUrl.value,
      ...alternateLinks.value.map(link => link.href),
    ])
  )
)

const linkedinUrl = computed(() =>
  toTrimmedString(t('siteIdentity.links.linkedin'))
)

const organizationJsonLd = computed(() => ({
  '@context': 'https://schema.org',
  '@type': 'Organization',
  name: siteName.value,
  url: canonicalUrl.value,
  logo: logoUrl.value,
  sameAs: Array.from(
    new Set([linkedinUrl.value, ...localizedHomeUrls.value].filter(Boolean))
  ),
}))

const searchPageUrl = computed(() =>
  new URL(
    resolveLocalizedRoutePath('search', locale.value),
    requestURL.origin
  ).toString()
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

const normalizeVerticalHomeUrl = (
  raw: string | null | undefined
): string | null => {
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
  () => new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium' })
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
    url: localePath({
      name: 'blog-slug',
      params: { slug: 'prioriser-durabilite' },
    }),
    title: String(t('home.blog.items.second.title')),
    summary: String(t('home.blog.items.second.excerpt')),
    formattedDate: String(t('home.blog.items.second.date')),
    slug: 'prioriser-durabilite',
  },
  {
    url: localePath({
      name: 'blog-slug',
      params: { slug: 'equilibrer-prix-impact' },
    }),
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

  return articles.map(article => {
    const formattedDate = article.createdMs
      ? blogDateFormatter.value.format(new Date(article.createdMs))
      : ''

    const title = toTrimmedString(article.title)
    const summary = sanitizeBlogSummary(article.summary)
    const image =
      typeof article.image === 'string' ? article.image.trim() : null

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

  return (
    pickSlugSegment((article as { slug?: string }).slug) ??
    pickSlugSegment(article.url)
  )
}

const resolveBlogArticleLink = (article: BlogPostDto) => {
  const slug = extractBlogSlug(article)

  if (!slug) {
    return localePath({ name: 'blog' })
  }

  return localePath({ name: 'blog-slug', params: { slug } })
}

const enrichedBlogItems = computed<EnrichedBlogItem[]>(() =>
  blogListItems.value.map(item => {
    const link = resolveBlogArticleLink(item)
    const hasImage = hasRenderableImage(item.image)

    return {
      ...item,
      link,
      hasImage,
    }
  })
)

const ogImageUrl = computed(() => logoUrl.value)

const navigateToSearch = (query?: string) => {
  const normalizedQuery = query?.trim() ?? ''

  router.push(
    localePath({
      name: 'search',
      query: normalizedQuery ? { q: normalizedQuery } : undefined,
    })
  )
}

const handleSearchSubmit = () => {
  const trimmedQuery = searchQuery.value.trim()

  if (
    trimmedQuery.length > 0 &&
    trimmedQuery.length < MIN_SUGGESTION_QUERY_LENGTH
  ) {
    return
  }

  navigateToSearch(trimmedQuery)
}

const resolveCategorySuggestionUrl = (
  suggestion: CategorySuggestionItem
): string | null => {
  const normalizedUrl = normalizeVerticalHomeUrl(suggestion.url)

  if (normalizedUrl) {
    return normalizedUrl
  }

  const verticalId = suggestion.verticalId?.trim()

  if (!verticalId) {
    return null
  }

  const matchedCategory = rawCategories.value.find(
    category => category.id === verticalId
  )

  return normalizeVerticalHomeUrl(matchedCategory?.verticalHomeUrl)
}

const handleCategorySuggestion = (suggestion: CategorySuggestionItem) => {
  searchQuery.value = suggestion.title

  const normalizedUrl = resolveCategorySuggestionUrl(suggestion)

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
    router.push(localePath(`/${gtin}`))
    return
  }

  navigateToSearch(suggestion.title)
}

const seoTitle = computed(() => String(t('home.seo.title')))

const seoDescription = computed(() => String(t('home.seo.description')))

const seoImageAlt = computed(() => String(t('home.seo.imageAlt')))

useSeoMeta({
  title: () => seoTitle.value,
  description: () => seoDescription.value,
  ogTitle: () => seoTitle.value,
  ogDescription: () => seoDescription.value,
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
  ogSiteName: () => siteName.value,
  ogLocale: () => locale.value.replace('-', '_'),
  ogImageAlt: () => seoImageAlt.value,
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
  <div>
    <PwaMobileLanding
      v-model:search-query="searchQuery"
      class="d-md-none"
      :verticals="rawCategories"
      :min-suggestion-query-length="MIN_SUGGESTION_QUERY_LENGTH"
      @submit="handleSearchSubmit"
      @select-category="handleCategorySuggestion"
      @select-product="handleProductSuggestion"
    />
    <div class="home-page">
      <HomeHeroSection
        v-model:search-query="searchQuery"
        class="d-none d-md-block"
        :min-suggestion-query-length="MIN_SUGGESTION_QUERY_LENGTH"
        :verticals="rawCategories"
        :partners-count="heroPartnersCount"
        :open-data-millions="openDataMillions"
        :products-count="productsCount"
        :categories-count="categoriesCount"
        :impact-score-products-count="impactScoreProductsCount"
        :impact-score-categories-count="impactScoreCategoriesCount"
        :products-without-vertical-count="productsWithoutVerticalCount"
        :reviewed-products-count="reviewedProductsCount"
        :ai-summary-remaining-credits="aiSummaryRemainingCredits"
        :should-reduce-motion="shouldReduceMotion"
        hero-background-i18n-key="hero.background"
        @submit="handleSearchSubmit"
        @select-category="handleCategorySuggestion"
        @select-product="handleProductSuggestion"
      />
      <div class="home-page__sections">
        <ParallaxWidget
          class="home-page__parallax"
          reverse
          :gapless="true"
          :backgrounds="parallaxBackgrounds.essentials.backgrounds"
          :overlay-opacity="parallaxBackgrounds.essentials.overlayOpacity"
          :parallax-amount="parallaxBackgrounds.essentials.parallaxAmount"
          :aria-label="t('home.parallax.essentials.ariaLabel')"
          :max-offset-ratio="parallaxBackgrounds.essentials.maxOffsetRatio"
          :enable-aplats="true"
          :aplat-svg="parallaxAplatEssentials"
          :data-i18n-pack-key="parallaxBackgroundKeys.essentials"
          :data-i18n-aplat-key="parallaxAplatKey"
        >
          <section
            id="home-essentials"
            class="home-page__section-wrapper home-page__stack"
          >
            <SectionReveal class="home-page__section" transition="slide-y">
              <template #default="{ reveal }">
                <HomeProblemsSection :items="problemItems" :reveal="reveal" />
              </template>
            </SectionReveal>

            <SectionReveal
              class="home-page__section"
              transition="slide-y-reverse"
            >
              <template #default="{ reveal }">
                <HomeSolutionSection
                  :benefits="solutionBenefits"
                  :reveal="reveal"
                />
              </template>
            </SectionReveal>
          </section>
        </ParallaxWidget>

        <section
          id="home-promises"
          class="home-page__section-wrapper home-page__stack home-page__promises"
          :aria-label="t('home.promises.ariaLabel')"
        >
          <SectionReveal class="home-page__section" transition="slide-y">
            <template #default="{ reveal }">
              <div
                class="home-page__promises-content"
                :class="{ 'home-page__promises-content--visible': reveal }"
              >
                <HomeHeroHighlights
                  :partners-count="heroPartnersCount"
                  :open-data-millions="openDataMillions"
                  :products-count="productsCount"
                  :categories-count="categoriesCount"
                  :impact-score-products-count="impactScoreProductsCount"
                  :impact-score-categories-count="impactScoreCategoriesCount"
                  :products-without-vertical-count="
                    productsWithoutVerticalCount
                  "
                  :reviewed-products-count="reviewedProductsCount"
                  :ai-summary-remaining-credits="aiSummaryRemainingCredits"
                  variant="section"
                />
              </div>
            </template>
          </SectionReveal>
        </section>

        <ParallaxWidget
          class="home-page__parallax"
          reverse
          :backgrounds="parallaxBackgrounds.blog.backgrounds"
          :overlay-opacity="parallaxBackgrounds.blog.overlayOpacity"
          :parallax-amount="parallaxBackgrounds.blog.parallaxAmount"
          :aria-label="t('home.parallax.knowledge.ariaLabel')"
          :max-offset-ratio="parallaxBackgrounds.blog.maxOffsetRatio"
          :enable-aplats="true"
          :aplat-svg="parallaxAplatBlog"
          :data-i18n-pack-key="parallaxBackgroundKeys.blog"
          :data-i18n-aplat-key="parallaxAplatKey"
        >
          <section
            id="home-knowledge-blog"
            class="home-page__section-wrapper home-page__stack"
          >
            <SectionReveal class="home-page__section" transition="slide-x">
              <template #default="{ reveal }">
                <HomeBlogSection
                  :loading="blogLoading"
                  :items="enrichedBlogItems"
                  :reveal="reveal"
                />
              </template>
            </SectionReveal>
          </section>
        </ParallaxWidget>

        <ParallaxWidget
          class="home-page__parallax home-page__parallax--centered"
          reverse
          :backgrounds="parallaxBackgrounds.cta.backgrounds"
          :overlay-opacity="parallaxBackgrounds.cta.overlayOpacity"
          :parallax-amount="parallaxBackgrounds.cta.parallaxAmount"
          :aria-label="t('home.parallax.cta.ariaLabel')"
          :max-offset-ratio="parallaxBackgrounds.cta.maxOffsetRatio"
          :data-i18n-pack-key="parallaxBackgroundKeys.cta"
          content-align="center"
        >
          <v-container fluid class="max_large mx-auto px-4">
            <v-row>
              <v-col cols="12" md="6">
                <SectionReveal class="home-page__section" transition="fade">
                  <template #default="{ reveal }">
                    <HomeFaqSection :items="faqPanels" :reveal="reveal" />
                  </template>
                </SectionReveal>
              </v-col>

              <v-col cols="12" md="6">
                <SectionReveal class="home-page__section" transition="slide-y">
                  <template #default="{ reveal }">
                    <HomeCtaSection
                      v-model:search-query="searchQuery"
                      :categories-landing-url="categoriesLandingUrl"
                      :min-suggestion-query-length="MIN_SUGGESTION_QUERY_LENGTH"
                      :reveal="reveal"
                      @submit="handleSearchSubmit"
                      @select-category="handleCategorySuggestion"
                      @select-product="handleProductSuggestion"
                    />
                  </template>
                </SectionReveal>
              </v-col>
            </v-row>
          </v-container>
        </ParallaxWidget>
      </div>
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
  background-color: transparent

.home-page__sections
  display: flex
  flex-direction: column
  gap: 0
  padding-top: var(--cat-overlap)
  background: transparent

.home-page__parallax
  border-radius: 0
  box-shadow: none
  overflow: hidden

.home-page__parallax--centered :deep(.home-section:not(.home-faq))
  text-align: center

.home-page__stack
  display: flex
  flex-direction: column
  gap: clamp(1.5rem, 4vw, 2.25rem)

.home-page__stack--compact
  gap: clamp(1.25rem, 3vw, 2rem)

@media (min-width: 1100px)
  .home-page__stack--two-columns
    grid-template-columns: repeat(2, 1fr)

.home-page__section
  width: 100%

.home-page__promises
  padding: clamp(2rem, 6vw, 4rem) clamp(1.5rem, 6vw, 4rem)
  background: linear-gradient(
    160deg,
    rgba(var(--v-theme-surface-ice-050), 0.85) 0%,
    rgba(var(--v-theme-surface-default), 0.95) 60%,
    rgba(var(--v-theme-surface-ice-100), 0.95) 100%
  )

.home-page__promises-content
  max-width: 1100px
  margin: 0 auto
  opacity: 0
  transform: translateY(20px)
  transition: opacity 300ms ease, transform 300ms ease

.home-page__promises-content--visible
  opacity: 1
  transform: translateY(0)

.home-contact-redirect__card
  border: 1px solid rgb(var(--v-theme-surface-primary-080))
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-glass), 0.35) 0%, rgb(var(--v-theme-surface-default)) 100%)

.home-contact-redirect__content
  display: grid
  grid-template-columns: 1fr
  gap: 1.25rem
  align-items: center
  padding: clamp(1.5rem, 4vw, 2.25rem)

.home-contact-redirect__texts
  display: flex
  flex-direction: column
  gap: 0.5rem

.home-contact-redirect__eyebrow
  align-self: flex-start
  padding: 0.3rem 0.9rem
  border-radius: 999px
  background: rgb(var(--v-theme-surface-primary-120))
  color: rgb(var(--v-theme-primary))
  letter-spacing: 0.08em
  text-transform: uppercase
  font-weight: 700
  font-size: 0.85rem
  margin: 0

.home-contact-redirect__title
  font-size: clamp(1.6rem, 3vw, 2rem)
  margin: 0
  color: rgb(var(--v-theme-text-neutral-strong))

.home-contact-redirect__subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-size: 1.05rem
  line-height: 1.5

.home-contact-redirect__form
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-contact-redirect__actions
  display: flex
  gap: 0.75rem
  flex-wrap: wrap
  align-items: center

.home-contact-redirect__helper
  margin: 0
  color: rgba(var(--v-theme-text-neutral-strong), 0.72)
  font-size: 0.95rem

@media (min-width: 960px)
  .home-contact-redirect__content
    grid-template-columns: 1.1fr 1fr
</style>
