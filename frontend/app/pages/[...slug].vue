<template>
  <div class="product-page">
    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      border="start"
      class="mb-6"
    >
      {{ errorMessage }}
    </v-alert>

    <v-skeleton-loader
      v-else-if="pending"
      type="article"
      class="mb-6"
    />

    <div v-else-if="product" class="product-page__layout">
      <aside class="product-page__nav" :class="{ 'product-page__nav--mobile': orientation === 'horizontal' }">
        <ProductSummaryNavigation
          :sections="navigableSections"
          :active-section="activeSection"
          :orientation="orientation"
          :aria-label="$t('product.navigation.label')"
          @navigate="scrollToSection"
        />
      </aside>

      <main class="product-page__content">
        <section :id="sectionIds.hero" class="product-page__section">
          <ProductHero :product="product" :breadcrumbs="productBreadcrumbs" />
        </section>

        <section
          v-if="impactScores.length"
          :id="sectionIds.impact"
          class="product-page__section"
        >
          <ProductImpactSection
            :scores="impactScores"
            :radar-values="radarValues"
            :ranking="rankingInfo"
            :country="countryInfo"
            :loading="loadingAggregations"
            :product-name="productTitle"
          />
        </section>

        <section :id="sectionIds.ai" class="product-page__section">
          <ProductAiReviewSection
            :gtin="product.gtin ?? gtin"
            :initial-review="product.aiReview?.review ?? null"
            :review-created-at="product.aiReview?.createdMs ?? undefined"
            :site-key="hcaptchaSiteKey"
          />
        </section>

        <section :id="sectionIds.price" class="product-page__section">
          <ProductPriceSection
            v-if="product.offers"
            :offers="product.offers"
            :commercial-events="commercialEvents"
          />
        </section>

        <section
          v-if="product.attributes"
          :id="sectionIds.attributes"
          class="product-page__section"
        >
          <ProductAttributesSection :attributes="product.attributes" />
        </section>

        <section
          v-if="product.resources?.pdfs?.length"
          :id="sectionIds.docs"
          class="product-page__section"
        >
          <ProductDocumentationSection :pdfs="product.resources.pdfs" />
        </section>

        <section
          v-if="showAdminSection"
          :id="sectionIds.admin"
          class="product-page__section"
        >
          <ProductAdminSection :product="product" />
        </section>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { createError } from 'h3'
import {
  AggTypeEnum,
  type Agg,
  type AggregationBucketDto,
  type AggregationResponseDto,
  type CommercialEvent,
  type ProductDto,
  type ProductScoreDto,
  type ProductSearchResponseDto,
} from '~~/shared/api-client'
import { matchProductRouteFromSegments, isBackendNotFoundError } from '~~/shared/utils/_product-route'
import ProductSummaryNavigation from '~/components/product/ProductSummaryNavigation.vue'
import ProductHero from '~/components/product/ProductHero.vue'
import ProductImpactSection from '~/components/product/ProductImpactSection.vue'
import ProductAiReviewSection from '~/components/product/ProductAiReviewSection.vue'
import ProductPriceSection from '~/components/product/ProductPriceSection.vue'
import ProductAttributesSection from '~/components/product/ProductAttributesSection.vue'
import ProductDocumentationSection from '~/components/product/ProductDocumentationSection.vue'
import ProductAdminSection from '~/components/product/ProductAdminSection.vue'
import { useCategories } from '~/composables/categories/useCategories'
import { useAuth } from '~/composables/useAuth'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'

const route = useRoute()
const requestURL = useRequestURL()
const runtimeConfig = useRuntimeConfig()
const { t } = useI18n()
const { isLoggedIn } = useAuth()
const display = useDisplay()

const slugParam = route.params.slug
const segments = Array.isArray(slugParam)
  ? slugParam.filter((segment): segment is string => typeof segment === 'string')
  : typeof slugParam === 'string'
    ? [slugParam]
    : []

const productRoute = matchProductRouteFromSegments(segments)

if (!productRoute) {
  throw createError({ statusCode: 404, statusMessage: 'Page not found' })
}

const { categorySlug, gtin } = productRoute

const {
  data: productData,
  pending,
  error,
} = await useAsyncData<ProductDto | null>(
  `product-${gtin}`,
  async () => {
    try {
      return await $fetch<ProductDto>(`/api/products/${gtin}`)
    } catch (fetchError) {
      if (isBackendNotFoundError(fetchError)) {
        throw createError({
          statusCode: 404,
          statusMessage: 'Product not found',
          cause: fetchError,
        })
      }

      throw fetchError
    }
  },
  { server: true, immediate: true },
)

const product = computed(() => productData.value)

if (product.value?.fullSlug) {
  const currentPath = route.path.startsWith('/') ? route.path : `/${route.path}`
  const targetPath = product.value.fullSlug.startsWith('/')
    ? product.value.fullSlug
    : `/${product.value.fullSlug}`

  if (targetPath !== currentPath) {
    await navigateTo(targetPath, { replace: true, redirectCode: 301 })
  }
}

const { selectCategoryBySlug } = useCategories()

const categoryDetail = ref<Awaited<ReturnType<typeof selectCategoryBySlug>> | null>(null)
const loadingAggregations = ref(false)
const aggregations = ref<Record<string, AggregationResponseDto>>({})

if (categorySlug) {
  try {
    categoryDetail.value = await selectCategoryBySlug(categorySlug)
  } catch (categoryError) {
    console.error('Failed to resolve category detail for product page.', categoryError)
  }
}

const scoreAggregations = async () => {
  if (!product.value || !categoryDetail.value?.id) {
    return
  }

  const scores = Object.keys(product.value.scores?.scores ?? {})
  if (!scores.length) {
    return
  }

  loadingAggregations.value = true

  const aggs: Agg[] = scores.map((scoreId) => ({
    name: `score_${scoreId}`,
    field: `scores.${scoreId}.value`,
    type: AggTypeEnum.Range,
    step: 0.5,
  }))

  try {
    const response = await $fetch<ProductSearchResponseDto>('/api/products/search', {
      method: 'POST',
      body: {
        verticalId: categoryDetail.value.id,
        pageSize: 0,
        aggs: { aggs },
      },
    })

    const resolved: Record<string, AggregationResponseDto> = {}
    ;(response.aggregations ?? []).forEach((aggregation) => {
      if (aggregation.name) {
        resolved[aggregation.name] = aggregation
      }
    })

    aggregations.value = resolved
  } catch (aggregationError) {
    console.error('Failed to fetch impact aggregations', aggregationError)
  } finally {
    loadingAggregations.value = false
  }
}

await scoreAggregations()

const productTitle = computed(() => {
  return (
    product.value?.names?.h1Title ??
    product.value?.identity?.bestName ??
    product.value?.slug ??
    `GTIN ${product.value?.gtin ?? gtin}`
  )
})

const productBreadcrumbs = computed(() => {
  const categoryItems = (categoryDetail.value?.breadCrumb ?? [])
    .map((item) => ({
      title: item.title ?? item.link ?? '',
      link: item.link ?? undefined,
    }))
    .filter((item) => (item.title?.toString().trim().length ?? 0) > 0 || (item.link?.toString().trim().length ?? 0) > 0)

  if (!product.value) {
    return categoryItems
  }

  const productLinkRaw = product.value.fullSlug ?? product.value.slug ?? route.fullPath
  const normalizedLink = productLinkRaw
    ? productLinkRaw.startsWith('http')
      ? productLinkRaw
      : productLinkRaw.startsWith('/')
        ? productLinkRaw
        : `/${productLinkRaw}`
    : route.fullPath

  return [
    ...categoryItems,
    {
      title: productTitle.value,
      link: normalizedLink,
    },
  ]
})

const productSubtitle = computed(() => {
  if (!product.value?.identity?.brand) {
    return null
  }

  return `${product.value.identity.brand}`
})

const productMetaDescription = computed(() => {
  return (
    product.value?.names?.metaDescription ??
    productSubtitle.value ??
    productTitle.value
  )
})

const canonicalUrl = computed(() => new URL(route.fullPath, requestURL.origin).toString())

useSeoMeta({
  title: () => productTitle.value,
  description: () => productMetaDescription.value,
  ogTitle: () => product.value?.names?.ogTitle ?? productTitle.value,
  ogDescription: () => product.value?.names?.ogDescription ?? productMetaDescription.value,
  ogUrl: () => canonicalUrl.value,
  ogType: 'website',
})

useHead(() => ({
  link: [
    { rel: 'canonical', href: canonicalUrl.value },
  ],
}))

const isRenderableScore = (score: ProductScoreDto | undefined | null): score is ProductScoreDto & { id: string; name: string } => {
  return typeof score?.id === 'string' && score.id.length > 0 && typeof score.name === 'string' && score.name.length > 0
}

type ImpactRankingInfo = {
  position: number
  total: number
  globalBest?: { fullSlug: string; bestName: string }
  globalBetter?: { fullSlug: string; bestName: string }
}

const impactScores = computed(() => {
  const scoreMap = product.value?.scores?.scores ?? {}

  return Object.values(scoreMap)
    .filter(isRenderableScore)
    .map((score) => {
      const aggregation = aggregations.value[`score_${score.id}`]
      const distribution = (aggregation?.buckets ?? [])
        .map((bucket: AggregationBucketDto) => ({
          label: bucket.key != null ? String(bucket.key) : '',
          value: Number(bucket.count ?? 0),
        }))
        .filter((bucket) => bucket.label.length > 0)

      return {
        id: score.id,
        label: score.name,
        description: score.description ?? null,
        relativeValue: typeof score.relativ?.value === 'number' ? score.relativ.value : null,
        absoluteValue: score.absoluteValue ?? null,
        percent: score.percent ?? null,
        ranking: score.ranking ?? null,
        letter: score.letter ?? null,
        distribution,
        energyLetter: score.id === 'CLASSE_ENERGY' && score.letter ? score.letter : null,
      }
    })
})

const radarValues = computed(() =>
  impactScores.value
    .map((score) => ({ name: score.label, value: score.relativeValue ?? 0 }))
    .filter((entry): entry is { name: string; value: number } => Boolean(entry.name) && Number.isFinite(entry.value)),
)

const rankingInfo = computed(() => {
  const ranking = product.value?.scores?.ranking
  if (!ranking) {
    return null
  }

  const info: ImpactRankingInfo = {
    position: ranking.globalPosition ?? 0,
    total: ranking.globalCount ?? 0,
  }

  if (ranking.globalBest?.fullSlug && ranking.globalBest?.bestName) {
    info.globalBest = {
      fullSlug: ranking.globalBest.fullSlug,
      bestName: ranking.globalBest.bestName,
    }
  }

  if (ranking.globalBetter?.fullSlug && ranking.globalBetter?.bestName) {
    info.globalBetter = {
      fullSlug: ranking.globalBetter.fullSlug,
      bestName: ranking.globalBetter.bestName,
    }
  }

  return info
})

const countryInfo = computed(() => {
  const info = product.value?.base?.gtinInfo
  if (!info) {
    return null
  }

  return {
    name: info.countryName ?? '',
    flag: info.countryFlagUrl ?? undefined,
  }
})

const { data: commercialEventsData } = await useAsyncData<CommercialEvent[] | null>(
  'commercial-events',
  async () => {
    try {
      return await $fetch<CommercialEvent[]>('/api/commercial-events')
    } catch (eventError) {
      console.error('Failed to load commercial events', eventError)
      return []
    }
  },
  { server: true },
)

const commercialEvents = computed(() => commercialEventsData.value ?? [])

const hcaptchaSiteKey = computed(() => runtimeConfig.public.hcaptchaSiteKey ?? '')

const showAdminSection = computed(() => isLoggedIn.value)

const sectionIds = {
  hero: 'hero',
  impact: 'impact',
  ai: 'synthese',
  price: 'prix',
  attributes: 'caracteristiques',
  docs: 'documentation',
  admin: 'admin',
} as const

const sections = computed(() => {
  const baseSections: Array<{ id: string; label: string; icon: string; condition?: boolean }>
    = [
      { id: sectionIds.hero, label: t('product.navigation.overview'), icon: 'mdi-information-outline', condition: true },
      { id: sectionIds.impact, label: t('product.navigation.impact'), icon: 'mdi-leaf', condition: impactScores.value.length > 0 },
      { id: sectionIds.ai, label: t('product.navigation.ai'), icon: 'mdi-robot-outline', condition: true },
      { id: sectionIds.price, label: t('product.navigation.price'), icon: 'mdi-currency-eur', condition: !!product.value?.offers },
      { id: sectionIds.attributes, label: t('product.navigation.attributes'), icon: 'mdi-format-list-bulleted', condition: !!product.value?.attributes },
      { id: sectionIds.docs, label: t('product.navigation.docs'), icon: 'mdi-file-document-outline', condition: (product.value?.resources?.pdfs?.length ?? 0) > 0 },
      { id: sectionIds.admin, label: t('product.navigation.admin'), icon: 'mdi-shield-account-outline', condition: showAdminSection.value },
    ]

  return baseSections.filter((section) => section.condition)
})

const navigableSections = computed(() => sections.value.map(({ condition, ...rest }) => rest))

const orientation = computed<'vertical' | 'horizontal'>(() => (display.mdAndDown.value ? 'horizontal' : 'vertical'))

const activeSection = ref<string>(sectionIds.hero)

const observer = ref<IntersectionObserver | null>(null)
const visibleSectionRatios = new Map<string, number>()
const MIN_SECTION_RATIO = 0.6

const refreshActiveSection = () => {
  if (!visibleSectionRatios.size) {
    activeSection.value = sections.value[0]?.id ?? sectionIds.hero
    return
  }

  const sorted = [...visibleSectionRatios.entries()].sort((a, b) => b[1] - a[1])
  const [nextActive] = sorted.find(([, ratio]) => ratio >= MIN_SECTION_RATIO) ?? sorted[0] ?? []

  if (nextActive) {
    activeSection.value = nextActive
  }
}

const observeSections = () => {
  if (!import.meta.client) {
    return
  }

  observer.value?.disconnect()
  visibleSectionRatios.clear()
  refreshActiveSection()

  observer.value = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        const ratio = entry.intersectionRatio
        if (ratio > 0) {
          visibleSectionRatios.set(entry.target.id, ratio)
        } else {
          visibleSectionRatios.delete(entry.target.id)
        }
      })

      refreshActiveSection()
    },
    {
      rootMargin: '-15% 0px -35% 0px',
      threshold: Array.from({ length: 11 }, (_, index) => index / 10),
    },
  )

  nextTick(() => {
    sections.value.forEach((section) => {
      const element = document.getElementById(section.id)
      if (element) {
        observer.value?.observe(element)
      }
    })
  })
}

onMounted(() => {
  observeSections()
})

watch(
  sections,
  () => {
    observeSections()
  },
  { flush: 'post' },
)

onBeforeUnmount(() => {
  observer.value?.disconnect()
  visibleSectionRatios.clear()
})

const scrollToSection = (sectionId: string) => {
  if (!import.meta.client) {
    return
  }

  const element = document.getElementById(sectionId)
  if (!element) {
    return
  }

  activeSection.value = sectionId

  const offset = orientation.value === 'horizontal' ? 96 : 120
  const top = element.getBoundingClientRect().top + window.scrollY - offset
  window.scrollTo({ top, behavior: 'smooth' })
}

const errorMessage = computed(() => {
  if (!error.value) {
    return null
  }

  if (error.value instanceof Error) {
    return error.value.message
  }

  return String(error.value)
})

const structuredOffers = computed(() => {
  const offers = product.value?.offers?.offersByCondition ?? {}
  return Object.values(offers)
    .flat()
    .filter((offer) => typeof offer?.price === 'number' && Boolean(offer?.url))
    .map((offer) => ({
      '@type': 'Offer',
      price: offer.price as number,
      priceCurrency: offer.currency ?? 'EUR',
      url: offer.url as string,
      availability: 'https://schema.org/InStock',
      itemCondition:
        offer.condition === 'NEW'
          ? 'https://schema.org/NewCondition'
          : 'https://schema.org/UsedCondition',
      seller: {
        '@type': 'Organization',
        name: offer.datasourceName ?? 'Unknown',
      },
    }))
})

const reviewStructuredData = computed(() => {
  const review = product.value?.aiReview?.review
  if (!review?.summary) {
    return null
  }

  const createdTimestamp = product.value?.aiReview?.createdMs

  return {
    '@type': 'Review',
    reviewBody: review.summary,
    name: review.shortTitle ?? productTitle.value,
    author: {
      '@type': 'Organization',
      name: 'Nudger IA',
    },
    dateCreated: typeof createdTimestamp === 'number' ? new Date(createdTimestamp).toISOString() : undefined,
  }
})

const productStructuredData = computed(() => {
  if (!product.value) {
    return null
  }

  const aggregateRatingValue =
    typeof product.value.scores?.ecoscore?.relativ?.value === 'number'
      ? product.value.scores.ecoscore.relativ.value
      : null

  return {
    '@context': 'https://schema.org',
    '@type': 'Product',
    name: productTitle.value,
    description: productMetaDescription.value,
    sku: product.value.base?.gtin ?? String(product.value.gtin ?? ''),
    brand: {
      '@type': 'Brand',
      name: product.value.identity?.brand ?? '',
    },
    offers: structuredOffers.value,
    aggregateRating: aggregateRatingValue
      ? {
          '@type': 'AggregateRating',
          ratingValue: aggregateRatingValue,
          reviewCount: product.value.scores?.ranking?.globalCount ?? 1,
        }
      : undefined,
    review: reviewStructuredData.value ?? undefined,
  }
})

useHead(() => {
  const scripts = [] as { type: string; key: string; children: string }[]

  if (productStructuredData.value) {
    scripts.push({
      key: 'product-structured-data',
      type: 'application/ld+json',
      children: JSON.stringify(productStructuredData.value),
    })
  }

  return {
    script: scripts,
  }
})
</script>

<style scoped>
.product-page {
  padding: 2rem 0;
}

.product-page__layout {
  display: grid;
  grid-template-columns: minmax(240px, 280px) minmax(0, 1fr);
  gap: 2rem;
}

.product-page__nav {
  position: sticky;
  top: 96px;
  align-self: start;
  height: fit-content;
}

.product-page__nav--mobile {
  position: static;
}

.product-page__content {
  display: flex;
  flex-direction: column;
  gap: 3rem;
}

.product-page__section {
  scroll-margin-top: 96px;
}

@media (max-width: 1280px) {
  .product-page__layout {
    grid-template-columns: 1fr;
  }

  .product-page__nav {
    position: sticky;
    top: 0;
    z-index: 20;
  }

  .product-page__section {
    scroll-margin-top: 140px;
  }
}
</style>
