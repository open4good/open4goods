<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { usePreferredReducedMotion } from '@vueuse/core'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'
import type { AffiliationPartnerDto, BlogPostDto } from '~~/shared/api-client'
import HomeHeroSection from '~/components/home/sections/HomeHeroSection.vue'
import HomeProblemsSection from '~/components/home/sections/HomeProblemsSection.vue'
import HomeSolutionSection from '~/components/home/sections/HomeSolutionSection.vue'
import HomeFeaturesSection from '~/components/home/sections/HomeFeaturesSection.vue'
import HomeBlogSection from '~/components/home/sections/HomeBlogSection.vue'
import HomeObjectionsSection from '~/components/home/sections/HomeObjectionsSection.vue'
import HomeFaqSection from '~/components/home/sections/HomeFaqSection.vue'
import HomeCtaSection from '~/components/home/sections/HomeCtaSection.vue'
import ParallaxSection from '~/components/shared/ui/ParallaxSection.vue'
import type {
  CategorySuggestionItem,
  ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'
import { useCategories } from '~/composables/categories/useCategories'
import { useBlog } from '~/composables/blog/useBlog'
import { useParallaxConfig } from '~/composables/useParallaxConfig'
import { useSeasonalParallaxPack } from '~/composables/useSeasonalParallaxPack'
import { useThemedParallaxBackgrounds } from '~/composables/useThemedParallaxBackgrounds'
import {
  PARALLAX_SECTION_KEYS,
  type ParallaxLayerConfig,
  type ParallaxSectionKey,
} from '~~/config/theme/assets'
import PwaMobileLanding from '~/components/pwa/PwaMobileLanding.vue'
import { useDisplay } from 'vuetify'

definePageMeta({
  ssr: true,
})

const { t, locale, availableLocales } = useI18n()
const router = useRouter()
const localePath = useLocalePath()
const requestURL = useRequestURL()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])
const display = useDisplay()

const device = useDevice()
const isMobileLanding = computed(() => device.isMobileOrTablet)

const searchQuery = ref('')

const MIN_SUGGESTION_QUERY_LENGTH = 2

type HomeBlogItem = BlogPostDto & { formattedDate?: string; slug?: string }
type EnrichedBlogItem = HomeBlogItem & { link: string; hasImage: boolean }

const { categories: rawCategories, fetchCategories } = useCategories()
const { paginatedArticles, fetchArticles, loading: blogLoading } = useBlog()

const BLOG_ARTICLES_LIMIT = 4

const { data: affiliationPartners } = await useAsyncData<AffiliationPartnerDto[]>(
  'home-affiliation-partners',
  () =>
    $fetch<AffiliationPartnerDto[]>('/api/partners/affiliation', {
      headers: requestHeaders,
    }).catch(() => []),
  {
    default: () => [],
    server: true,
    lazy: true,
  }
)

const heroPartnersCount = computed(
  () => affiliationPartners.value?.length ?? 0
)

const seasonalParallaxPack = useSeasonalParallaxPack()

const parallaxConfig = useParallaxConfig()
const parallaxLayers = useThemedParallaxBackgrounds(seasonalParallaxPack)

type ParallaxSectionRenderConfig = {
  backgrounds: ParallaxLayerConfig[]
  overlayOpacity: number
  parallaxAmount: number
  ariaLabel: string
  maxOffsetRatio: number | null
}

const parallaxBackgrounds = computed<
  Record<ParallaxSectionKey, ParallaxSectionRenderConfig>
>(() =>
  PARALLAX_SECTION_KEYS.reduce<Record<ParallaxSectionKey, ParallaxSectionRenderConfig>>(
    (acc, section) => ({
      ...acc,
      [section]: {
        backgrounds: parallaxLayers.value[section] || [],
        overlayOpacity: parallaxConfig.value[section].overlay,
        parallaxAmount: parallaxConfig.value[section].parallaxAmount,
        ariaLabel: parallaxConfig.value[section].ariaLabel,
        maxOffsetRatio: parallaxConfig.value[section].maxOffsetRatio,
      },
    }),
    {} as Record<ParallaxSectionKey, ParallaxSectionRenderConfig>
  )
)

type AnimatedSectionKey =
  | 'problems'
  | 'solution'
  | 'features'
  | 'blog'
  | 'objections'
  | 'faq'
  | 'cta'

const prefersReducedMotion = usePreferredReducedMotion()
const shouldReduceMotion = computed(() => prefersReducedMotion.value === 'reduce')

const animatedSections = reactive<Record<AnimatedSectionKey, boolean>>({
  problems: false,
  solution: false,
  features: false,
  blog: false,
  objections: false,
  faq: false,
  cta: false,
})

const markAllSectionsVisible = () => {
  ;(Object.keys(animatedSections) as AnimatedSectionKey[]).forEach(key => {
    animatedSections[key] = true
  })
}

watch(
  shouldReduceMotion,
  shouldReduce => {
    if (shouldReduce) {
      markAllSectionsVisible()
    }
  },
  { immediate: true }
)

const createIntersectHandler =
  (key: AnimatedSectionKey) =>
  (
    _entries: IntersectionObserverEntry[],
    _observer: IntersectionObserver,
    isIntersecting: boolean
  ) => {
    if (animatedSections[key]) {
      return
    }

    if (shouldReduceMotion.value || isIntersecting) {
      animatedSections[key] = true
    }
  }

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

useAsyncData(
  'home-heavy-data',
  async () => {
    if (rawCategories.value.length === 0) {
      await fetchCategories(true)
    }

    if (paginatedArticles.value.length === 0) {
      await fetchArticles(1, BLOG_ARTICLES_LIMIT, null)
    }
    return true
  },
  {
    lazy: true,
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

const featuredBlogItem = computed(() => enrichedBlogItems.value[0] ?? null)
const secondaryBlogItems = computed(() => enrichedBlogItems.value.slice(1))

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
      })
    )
    return
  }

  navigateToSearch(suggestion.title)
}

const seoTitle = computed(() =>
  String(
    t(
      isMobileLanding.value
        ? 'pwa.landing.hero.meta.title'
        : 'home.seo.title'
    )
  )
)

const seoDescription = computed(() =>
  String(
    t(
      isMobileLanding.value
        ? 'pwa.landing.hero.meta.description'
        : 'home.seo.description'
    )
  )
)

const seoImageAlt = computed(() =>
  String(
    t(
      isMobileLanding.value
        ? 'pwa.landing.hero.meta.imageAlt'
        : 'home.seo.imageAlt'
    )
  )
)

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
  <PwaMobileLanding v-if="isMobileLanding" :verticals="rawCategories" />
  <div v-else class="home-page">
    <HomeHeroSection
      v-model:search-query="searchQuery"
      :min-suggestion-query-length="MIN_SUGGESTION_QUERY_LENGTH"
      :verticals="rawCategories"
      :partners-count="heroPartnersCount"
      @submit="handleSearchSubmit"
      @select-category="handleCategorySuggestion"
      @select-product="handleProductSuggestion"
    />
    <div class="home-page__sections">
      <ParallaxSection
        id="home-essentials"
        class="home-page__parallax"
        :backgrounds="parallaxBackgrounds.essentials.backgrounds"
        :overlay-opacity="parallaxBackgrounds.essentials.overlayOpacity"
        :parallax-amount="parallaxBackgrounds.essentials.parallaxAmount"
        :aria-label="parallaxBackgrounds.essentials.ariaLabel"
        :max-offset-ratio="parallaxBackgrounds.essentials.maxOffsetRatio"
        :enable-aplats="true"
      >
        <div class="home-page__stack">
          <div
            v-intersect="createIntersectHandler('problems')"
            class="home-page__section"
          >
            <v-slide-y-transition :disabled="shouldReduceMotion">
              <div v-show="animatedSections.problems">
                <HomeProblemsSection :items="problemItems" />
              </div>
            </v-slide-y-transition>
          </div>

          <div
            v-intersect="createIntersectHandler('solution')"
            class="home-page__section"
          >
            <v-slide-y-reverse-transition :disabled="shouldReduceMotion">
              <div v-show="animatedSections.solution">
                <HomeSolutionSection :benefits="solutionBenefits" />
              </div>
            </v-slide-y-reverse-transition>
          </div>
        </div>
      </ParallaxSection>

      <ParallaxSection
        id="home-features"
        class="home-page__parallax home-page__parallax--centered"
        :backgrounds="parallaxBackgrounds.features.backgrounds"
        :overlay-opacity="parallaxBackgrounds.features.overlayOpacity"
        :parallax-amount="parallaxBackgrounds.features.parallaxAmount"
        :aria-label="parallaxBackgrounds.features.ariaLabel"
        :max-offset-ratio="parallaxBackgrounds.features.maxOffsetRatio"
        content-align="center"
      >
        <div
          v-intersect="createIntersectHandler('features')"
          class="home-page__section"
        >
          <v-scale-transition :disabled="shouldReduceMotion">
            <div v-show="animatedSections.features">
              <HomeFeaturesSection :features="featureCards" />
            </div>
          </v-scale-transition>
        </div>
      </ParallaxSection>

      <ParallaxSection
        id="home-knowledge-blog"
        class="home-page__parallax"
        :backgrounds="parallaxBackgrounds.blog.backgrounds"
        :overlay-opacity="parallaxBackgrounds.blog.overlayOpacity"
        :parallax-amount="parallaxBackgrounds.blog.parallaxAmount"
        :aria-label="parallaxBackgrounds.blog.ariaLabel"
        :max-offset-ratio="parallaxBackgrounds.blog.maxOffsetRatio"
        :enable-aplats="true"
      >
        <div class="home-page__stack">
          <div
            v-intersect="createIntersectHandler('blog')"
            class="home-page__section"
          >
            <v-slide-x-transition :disabled="shouldReduceMotion">
              <div v-show="animatedSections.blog">
                <HomeBlogSection
                  :loading="blogLoading"
                  :featured-item="featuredBlogItem"
                  :secondary-items="secondaryBlogItems"
                />
              </div>
            </v-slide-x-transition>
          </div>
        </div>
      </ParallaxSection>

      <ParallaxSection
        id="home-knowledge-objections"
        class="home-page__parallax"
        :backgrounds="parallaxBackgrounds.objections.backgrounds"
        :overlay-opacity="parallaxBackgrounds.objections.overlayOpacity"
        :parallax-amount="parallaxBackgrounds.objections.parallaxAmount"
        :aria-label="parallaxBackgrounds.objections.ariaLabel"
        :max-offset-ratio="parallaxBackgrounds.objections.maxOffsetRatio"
        :enable-aplats="true"
      >
        <div class="home-page__stack">
          <div
            v-intersect="createIntersectHandler('objections')"
            class="home-page__section"
          >
            <v-slide-x-reverse-transition :disabled="prefersReducedMotion">
              <div v-show="animatedSections.objections">
                <HomeObjectionsSection :items="objectionItems" />
              </div>
            </v-slide-x-reverse-transition>
          </div>
        </div>
      </ParallaxSection>

      <ParallaxSection
        id="home-cta"
        class="home-page__parallax home-page__parallax--centered"
        :backgrounds="parallaxBackgrounds.cta.backgrounds"
        :overlay-opacity="parallaxBackgrounds.cta.overlayOpacity"
        :parallax-amount="parallaxBackgrounds.cta.parallaxAmount"
        :aria-label="parallaxBackgrounds.cta.ariaLabel"
        :max-offset-ratio="parallaxBackgrounds.cta.maxOffsetRatio"
        content-align="center"
      >
        <div class="home-page__stack home-page__stack--compact">
          <div
            v-intersect="createIntersectHandler('faq')"
            class="home-page__section"
          >
            <v-fade-transition :disabled="prefersReducedMotion">
              <div v-show="animatedSections.faq">
                <HomeFaqSection :items="faqPanels" />
              </div>
            </v-fade-transition>
          </div>

          <div
            v-intersect="createIntersectHandler('cta')"
            class="home-page__section"
          >
            <v-slide-y-transition :disabled="shouldReduceMotion">
              <div v-show="animatedSections.cta">
                <HomeCtaSection
                  v-model:search-query="searchQuery"
                  :categories-landing-url="categoriesLandingUrl"
                  :min-suggestion-query-length="MIN_SUGGESTION_QUERY_LENGTH"
                  @submit="handleSearchSubmit"
                  @select-category="handleCategorySuggestion"
                  @select-product="handleProductSuggestion"
                />
              </div>
            </v-slide-y-transition>
          </div>
        </div>
      </ParallaxSection>

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

.home-page__parallax--centered :deep(.home-section)
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
</style>
