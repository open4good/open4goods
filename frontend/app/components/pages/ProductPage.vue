<template>
  <div class="product-page">
    <ClientOnly>
      <ProductStickyPriceBanner
        :open="isStickyBannerOpen"
        :product="product ?? undefined"
        :offers-count-label="bannerOffersCountLabel"
        @scroll-to-offers="scrollToSection(sectionIds.price)"
      />
    </ClientOnly>

    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      border="start"
      class="mb-6"
    >
      {{ errorMessage }}
    </v-alert>

    <v-skeleton-loader v-else-if="pending" type="article" class="mb-6" />

    <div v-else-if="product" class="product-page__layout">
      <aside
        class="product-page__nav"
        :class="{ 'product-page__nav--mobile': orientation === 'horizontal' }"
      >
        <ProductSummaryNavigation
          :sections="navigableSections"
          :admin-sections="adminNavigableSections"
          :admin-title="$t('product.navigation.adminPanel.title')"
          :admin-helper="$t('product.navigation.adminPanel.helper')"
          :active-section="activeSection"
          :orientation="orientation"
          :aria-label="$t('product.navigation.label')"
          @navigate="scrollToSection"
        />
      </aside>

      <main class="product-page__content">
        <section
          :id="sectionIds.hero"
          ref="heroSectionRef"
          class="product-page__section"
        >
          <div class="product-page__hero">
            <ProductHero
              :product="product"
              :breadcrumbs="productBreadcrumbs"
              :impact-score="impactScoreOutOf20"
              :impact-score-min="impactScoreMin"
              :impact-score-max="impactScoreMax"
              :popular-attributes="heroPopularAttributes"
              :has-category="!!categoryDetail"
            />
          </div>
        </section>

        <section
          v-if="categoryDetail"
          :id="sectionIds.impact"
          ref="impactSectionRef"
          class="product-page__section"
        >
          <ProductImpactSection
            :scores="impactScores"
            :radar-data="radarData"
            :product-name="productTitle"
            :product-brand="productBrand"
            :product-model="productModel"
            :product-image="resolvedProductImageSource"
            :vertical-home-url="verticalHomeUrl"
            :vertical-title="normalizedVerticalTitle"
            :subtitle-params="impactSubtitleParams"
            :expanded-score-id="expandedScoreId"
            :ai-impact-text="product.aiReview?.ecologicalOneline"
            :on-market-end-date="product.eprel?.onMarketEndDate"
            :score-min="impactScoreMin"
            :score-max="impactScoreMax"
          />
        </section>

        <section
          v-if="showAiReviewSection"
          :id="sectionIds.ai"
          class="product-page__section"
        >
          <ProductAiReviewSection
            :gtin="product.gtin ?? gtin"
            :initial-review="product.aiReview?.review ?? null"
            :review-created-at="product.aiReview?.createdMs ?? undefined"
            :site-key="hcaptchaSiteKey"
            :title-params="aiTitleParams"
            :product-name="productTitle"
            :product-image="resolvedProductImageSource"
            :failure-reason="product.aiReview?.failureReason ?? null"
            :enough-data="product.aiReview?.enoughData ?? true"
          />
        </section>

        <section :id="sectionIds.price" class="product-page__section">
          <ProductPriceSection
            v-if="product.offers"
            :offers="product.offers"
            :commercial-events="commercialEvents"
            :title-params="priceTitleParams"
          />
        </section>

        <section
          v-if="product.timeline"
          :id="sectionIds.timeline"
          class="product-page__section"
        >
          <ProductLifeTimeline :timeline="product.timeline" />
        </section>

        <section
          v-if="showAlternativesSection"
          :id="sectionIds.alternatives"
          class="product-page__section"
        >
          <ProductAlternatives
            :product="product"
            :vertical-id="categoryDetail?.id ?? ''"
            :popular-attributes="categoryDetail?.popularAttributes ?? []"
            :subtitle-params="alternativesSubtitleParams"
            @alternatives-updated="handleAlternativesUpdated"
          />
        </section>

        <section
          v-if="showVigilanceSection"
          :id="sectionIds.vigilance"
          class="product-page__section"
        >
          <ProductVigilanceSection
            :product="product"
            :on-market-end-date="product.eprel?.onMarketEndDate"
            @click:offers="scrollToSection(subSectionIds.priceOffers)"
          />
        </section>

        <section
          v-if="showAttributesSection"
          :id="sectionIds.attributes"
          class="product-page__section"
        >
          <ProductAttributesSection
            :product="product"
            :attribute-configs="categoryDetail?.attributesConfig?.configs ?? []"
            :title-params="attributesTitleParams"
          />
        </section>

        <section
          v-if="product.resources?.pdfs?.length"
          :id="sectionIds.docs"
          class="product-page__section"
        >
          <ProductDocumentationSection :pdfs="product.resources.pdfs" />
        </section>

        <section v-if="showAdminSection" class="product-page__section">
          <ProductAdminSection
            :product="product"
            :panel-id="sectionIds.adminPanel"
            :json-section-id="sectionIds.adminJson"
            :datasources-section-id="sectionIds.adminDatasources"
            :vertical-config="categoryDetail"
          />
        </section>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useElementBounding, useWindowScroll } from '@vueuse/core'
import {
  computed,
  defineAsyncComponent,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from 'vue'
import { createError } from 'h3'
import type {
  Agg,
  AggregationBucketDto,
  AggregationResponseDto,
  AttributeConfigDto,
  CommercialEvent,
  FilterRequestDto,
  ProductIndexedAttributeDto,
  ProductDto,
  ProductReferenceDto,
  ProductScoreDto,
  ProductSearchResponseDto,
} from '~~/shared/api-client'
import { AggTypeEnum } from '~~/shared/api-client/models/Agg'
import { normalizeTimestamp } from '~/utils/date-parsing'
import type { ProductRouteMatch } from '~~/shared/utils/_product-route'
import { isBackendNotFoundError } from '~~/shared/utils/_product-route'

import ProductSummaryNavigation from '~/components/product/ProductSummaryNavigation.vue'
import ProductHero from '~/components/product/ProductHero.vue'
import type { ProductHeroBreadcrumb } from '~/components/product/ProductHero.vue'
import { useCategories } from '~/composables/categories/useCategories'
import { useAuth } from '~/composables/useAuth'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { buildCategoryHash } from '~/utils/_category-filter-state'
import { resolveScoreNumericValue } from '~/utils/score-values'
import { formatBrandModelTitle, humanizeSlug } from '~/utils/_product-title'
import { resolveProductTitle } from '~/utils/_product-title-resolver'
import {
  buildImpactAggregateAnchorId,
  buildImpactScoreGroups,
} from '~/components/product/impact/impact-score-groups'
import { transformRadarValue } from '~/utils/radar-utils'

const ProductStickyPriceBanner = defineAsyncComponent(
  () => import('~/components/product/ProductStickyPriceBanner.vue')
)
const ProductAttributesSection = defineAsyncComponent(
  () => import('~/components/product/ProductAttributesSection.vue')
)
const ProductImpactSection = defineAsyncComponent(
  () => import('~/components/product/ProductImpactSection.vue')
)
const ProductAiReviewSection = defineAsyncComponent(
  () => import('~/components/product/ProductAiReviewSection.vue')
)
const ProductPriceSection = defineAsyncComponent(
  () => import('~/components/product/ProductPriceSection.vue')
)
const ProductAlternatives = defineAsyncComponent(
  () => import('~/components/product/impact/ProductAlternatives.vue')
)
const ProductDocumentationSection = defineAsyncComponent(
  () => import('~/components/product/ProductDocumentationSection.vue')
)
const ProductAdminSection = defineAsyncComponent(
  () => import('~/components/product/ProductAdminSection.vue')
)
const ProductLifeTimeline = defineAsyncComponent(
  () => import('~/components/product/ProductLifeTimeline.vue')
)
const ProductVigilanceSection = defineAsyncComponent(
  () => import('~/components/product/ProductVigilanceSection.vue')
)

const route = useRoute()
const requestURL = useRequestURL()
const runtimeConfig = useRuntimeConfig()
const { t, locale } = useI18n()
const { isLoggedIn } = useAuth()
const display = useDisplay()
const { y: scrollY } = useWindowScroll()

const isStickyBannerOpen = ref(false)

const heroSectionRef = ref<HTMLElement | null>(null)
const { bottom: _heroSectionBottom } = useElementBounding(heroSectionRef)

const PRODUCT_COMPONENTS = [
  'base',
  'identity',
  'names',
  'attributes',
  'resources',
  'scores',
  'aiReview',
  'offers',
  'timeline',
  'eprel',
].join(',')

const impactSectionRef = ref<HTMLElement | null>(null)
const { top: _impactSectionTop } = useElementBounding(impactSectionRef)

const bannerOffersCountLabel = computed(() => {
  const count = product.value?.offers?.offersCount ?? 0
  if (count <= 0) return undefined
  return t('product.banner.offersCount', { count })
})

// Update sticky banner visibility based on scroll position relative to hero section
watch(
  scrollY,
  () => {
    if (!heroSectionRef.value) {
      isStickyBannerOpen.value = false
      return
    }

    // Show banner when we've scrolled past the hero section
    // The banner should appear when the bottom of the hero section is nearing the top of the viewport
    // Adjust the offset (80px) as needed for the header height
    const rect = heroSectionRef.value.getBoundingClientRect()
    isStickyBannerOpen.value = rect.bottom <= 80
  },
  { immediate: true }
)

const props = defineProps<{
  productRoute: ProductRouteMatch
}>()

const productRoute = props.productRoute

if (!productRoute) {
  throw createError({ statusCode: 404, statusMessage: 'Page not found' })
}

const { categorySlug, gtin } = productRoute

const productLoadError = ref<Error | null>(null)

const {
  data: productData,
  pending,
  error,
} = await useAsyncData<ProductDto | null>(
  `product-${gtin}`,
  async () => {
    productLoadError.value = null
    try {
      const baseUrl = import.meta.server ? requestURL.origin : ''
      return await $fetch<ProductDto>(`${baseUrl}/api/products/${gtin}`, {
        query: { include: PRODUCT_COMPONENTS },
      })
    } catch (fetchError) {
      if (isBackendNotFoundError(fetchError)) {
        throw createError({
          statusCode: 404,
          statusMessage: 'Product not found',
          cause: fetchError,
        })
      }

      const fallbackMessage =
        fetchError instanceof Error && fetchError.message?.trim().length
          ? fetchError.message
          : String(t('error.page.generic.statusMessage'))

      productLoadError.value =
        fetchError instanceof Error ? fetchError : new Error(fallbackMessage)

      if (!productLoadError.value.message?.trim().length) {
        productLoadError.value.message = fallbackMessage
      }

      console.error('Failed to load product details', fetchError)

      return null
    }
  },
  { server: true, immediate: true }
)

const product = computed(() => productData.value)

if (product.value?.fullSlug) {
  const currentPath = route.path.startsWith('/') ? route.path : `/${route.path}`
  const targetPath = product.value.fullSlug.startsWith('/')
    ? product.value.fullSlug
    : `/${product.value.fullSlug}`

  // Only redirect if we are NOT on a valid uncategorized route (gtin-slug) that was intentionally requested
  // heuristic: if we have NO categorySlug matched in the route, we allow it (uncategorized view)
  const isUncategorizedRoute = !productRoute.categorySlug

  if (
    targetPath !== currentPath &&
    (!isUncategorizedRoute || !productRoute.slug)
  ) {
    await navigateTo(targetPath, { replace: true, redirectCode: 301 })
  }
}

const { selectCategoryBySlug } = useCategories()

const categoryDetail = ref<Awaited<
  ReturnType<typeof selectCategoryBySlug>
> | null>(null)
const loadingAggregations = ref(false)
const aggregations = ref<Record<string, AggregationResponseDto>>({})

const requestedScoreIds = computed(() => {
  const ids: string[] = []

  const pushId = (candidate: unknown) => {
    if (typeof candidate !== 'string') {
      return
    }

    const normalized = candidate.trim()
    if (!normalized.length || ids.includes(normalized)) {
      return
    }

    ids.push(normalized)
  }

  pushId('ECOSCORE')

  const ponderations =
    categoryDetail.value?.impactScoreConfig?.criteriasPonderation ?? {}
  Object.keys(ponderations).forEach(key => pushId(key))

  return ids
})

const scoreCoefficientMap = computed<Record<string, number>>(() => {
  const raw =
    categoryDetail.value?.impactScoreConfig?.criteriasPonderation ?? {}

  return Object.entries(raw).reduce<Record<string, number>>(
    (acc, [key, value]) => {
      const normalizedKey =
        typeof key === 'string' ? key.trim().toUpperCase() : ''
      if (!normalizedKey.length) {
        return acc
      }

      const numericValue = typeof value === 'number' ? value : Number(value)
      if (!Number.isFinite(numericValue)) {
        return acc
      }

      acc[normalizedKey] = Math.min(Math.max(numericValue, 0), 1)
      return acc
    },
    {}
  )
})

const attributeConfigMap = computed(() => {
  const configs = categoryDetail.value?.attributesConfig?.configs ?? []

  return configs.reduce((map, attribute) => {
    const normalizedKey = attribute.key?.toString().trim().toUpperCase()
    if (normalizedKey?.length) {
      map.set(normalizedKey, attribute as AttributeConfigDto)
    }

    return map
  }, new Map<string, AttributeConfigDto>())
})

const availableImpactCriteriaMap = computed(() => {
  const criterias = categoryDetail.value?.availableImpactScoreCriterias ?? []

  return criterias.reduce((map, key) => {
    const normalizedKey = key?.toString().trim().toUpperCase()
    if (normalizedKey?.length) {
      const attribute = attributeConfigMap.value.get(normalizedKey)
      map.set(normalizedKey, {
        title: attribute?.scoreTitle ?? attribute?.name ?? normalizedKey,
        description: attribute?.scoreDescription ?? null,
        utility: attribute?.scoreUtility ?? null,
      })
    }

    return map
  }, new Map<string, { title: string; description: string | null; utility: string | null }>())
})

if (categorySlug) {
  try {
    categoryDetail.value = await selectCategoryBySlug(categorySlug)
  } catch (categoryError) {
    console.error(
      'Failed to resolve category detail for product page.',
      categoryError
    )
  }
}

const scoreAggregations = async () => {
  if (!product.value || !categoryDetail.value?.id) {
    return
  }

  const scores = requestedScoreIds.value
  if (!scores.length) {
    return
  }

  loadingAggregations.value = true

  const aggs: Agg[] = scores.map(scoreId => ({
    name: `score_${scoreId}`,
    field: `scores.${scoreId}.value`,
    type: AggTypeEnum.Range,
    step: 0.5,
  }))

  try {
    const response = await $fetch<ProductSearchResponseDto>('/api/products', {
      method: 'POST',
      body: {
        verticalId: categoryDetail.value.id,
        pageSize: 0,
        aggs: { aggs },
      },
    })

    const resolved: Record<string, AggregationResponseDto> = {}
    ;(response.aggregations ?? []).forEach(aggregation => {
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
  const rawSlug = product.value?.slug ?? ''
  const normalizedSlug = rawSlug.trim()
  const slugFallback = normalizedSlug
    ? humanizeSlug(normalizedSlug, locale.value) || normalizedSlug
    : ''
  const gtinValue = product.value?.gtin ?? gtin
  const gtinFallback = gtinValue
    ? t('product.meta.gtinFallback', { gtin: gtinValue })
    : ''

  if (!product.value) {
    return slugFallback || gtinFallback
  }

  const resolvedTitle = resolveProductTitle(product.value, locale.value, {
    preferH1Title: true,
    uppercaseBrand: true,
    gtinFallback,
  })

  return resolvedTitle || slugFallback || gtinFallback
})

const heroPopularAttributes = computed(
  () => categoryDetail.value?.popularAttributes ?? []
)
const verticalHomeUrl = computed(
  () => categoryDetail.value?.verticalHomeUrl?.trim() ?? ''
)
const verticalTitle = computed(() => {
  const candidates = [
    categoryDetail.value?.verticalHomeTitle,
    categoryDetail.value?.verticalMetaTitle,
  ]

  for (const candidate of candidates) {
    if (typeof candidate === 'string' && candidate.trim().length) {
      return candidate.trim()
    }
  }

  return ''
})
const normalizedVerticalTitle = computed(() => {
  const title = verticalTitle.value
  if (!title.length) {
    return ''
  }

  try {
    return title.toLocaleLowerCase(locale.value)
  } catch (error) {
    if (import.meta.dev) {
      console.warn('Failed to localize vertical title casing.', error)
    }

    return title.toLowerCase()
  }
})

const BRAND_FILTER_FIELD = 'attributes.referentielAttributes.BRAND' as const

const normalizedCategoryPath = computed(() => {
  const raw =
    (categoryDetail.value as { fullSlug?: string | null } | null)?.fullSlug ??
    categoryDetail.value?.verticalHomeUrl ??
    null ??
    null

  if (!raw) {
    return null
  }

  const trimmed = raw.toString().trim()
  if (!trimmed.length) {
    return null
  }

  const withLeadingSlash = trimmed.startsWith('/') ? trimmed : `/${trimmed}`
  const sanitized = withLeadingSlash.split('?')[0]?.split('#')[0]

  return sanitized?.length ? sanitized : withLeadingSlash
})

const handleAlternativesUpdated = (payload: {
  hasAlternatives: boolean
  hydrated: boolean
}) => {
  alternativesHydrated.value = payload.hydrated
  hasAlternatives.value = payload.hasAlternatives
}

const productBrand = computed(() => {
  const brand = product.value?.identity?.brand
  return typeof brand === 'string' ? brand.trim() : ''
})

const productModel = computed(() => {
  const model = product.value?.identity?.model
  return typeof model === 'string' ? model.trim() : ''
})

const modelVariations = computed(() => {
  const identity = product.value?.identity
  const candidates: string[] = []

  if (identity?.model) {
    candidates.push(identity.model)
  }

  const akaModels = identity?.akaModels
  if (akaModels instanceof Set) {
    candidates.push(...Array.from(akaModels))
  } else if (Array.isArray(akaModels)) {
    candidates.push(...akaModels)
  }

  const deduped = new Map<string, string>()
  for (const candidate of candidates) {
    if (typeof candidate !== 'string') {
      continue
    }

    const normalized = candidate.trim()
    if (!normalized.length) {
      continue
    }

    const key = normalized.toLocaleLowerCase(locale.value)
    if (!deduped.has(key)) {
      deduped.set(key, normalized)
    }
  }

  return Array.from(deduped.values()).sort(
    (current, next) => current.length - next.length
  )
})

const sectionModelVariationCycle = computed(() => {
  const variations = modelVariations.value

  const sectionOrder = [
    'impactSubtitle',
    'aiTitle',
    'priceTitle',
    'alternativesSubtitle',
    'attributesTitle',
  ] as const

  const resolved: Record<(typeof sectionOrder)[number], string> = {
    impactSubtitle: '',
    aiTitle: '',
    priceTitle: '',
    alternativesSubtitle: '',
    attributesTitle: '',
  }

  if (!variations.length) {
    return resolved
  }

  let index = 0
  for (const key of sectionOrder) {
    resolved[key] = variations[index] ?? ''
    index = (index + 1) % variations.length
  }

  return resolved
})

const formatModelVariationLabel = (variation: string) => {
  const trimmedVariation = variation.trim()
  const brand = productBrand.value

  if (brand && trimmedVariation) {
    return `${brand} - ${trimmedVariation}`
  }

  return trimmedVariation || brand
}

const buildModelVariationParams = (options: {
  key:
    | 'impactSubtitle'
    | 'aiTitle'
    | 'priceTitle'
    | 'alternativesSubtitle'
    | 'attributesTitle'
}) => {
  const variation = sectionModelVariationCycle.value[options.key]
  const fallbackBrand = productBrand.value
  const resolvedVariation = variation
    ? formatModelVariationLabel(variation)
    : ''
  const modelVariation = resolvedVariation || fallbackBrand

  if (!modelVariation) {
    return undefined
  }

  return {
    modelVariation,
  }
}

const impactSubtitleParams = computed(() =>
  buildModelVariationParams({ key: 'impactSubtitle' })
)
const aiTitleParams = computed(() =>
  buildModelVariationParams({ key: 'aiTitle' })
)
const priceTitleParams = computed(() =>
  buildModelVariationParams({ key: 'priceTitle' })
)
const alternativesSubtitleParams = computed(() =>
  buildModelVariationParams({ key: 'alternativesSubtitle' })
)
const attributesTitleParams = computed(() =>
  buildModelVariationParams({ key: 'attributesTitle' })
)

const brandModelTitle = computed(() =>
  formatBrandModelTitle(productBrand.value, productModel.value, locale.value)
)

const productMetaTitle = computed(() => {
  return brandModelTitle.value.length
    ? brandModelTitle.value
    : productTitle.value
})

const brandBreadcrumb = computed<ProductHeroBreadcrumb | null>(() => {
  const brand = productBrand.value
  if (!brand.length) {
    return null
  }

  const basePath = normalizedCategoryPath.value
  if (!basePath) {
    return { title: brand }
  }

  const filters: FilterRequestDto = {
    filters: [
      {
        field: BRAND_FILTER_FIELD,
        operator: 'term',
        terms: [brand],
      },
    ],
  }

  const hash = buildCategoryHash({ filters })
  const link = `${basePath}${hash}`

  return {
    title: brand,
    link,
  }
})

const productBreadcrumbs = computed<ProductHeroBreadcrumb[]>(() => {
  const breadcrumbs = categoryDetail.value?.breadCrumb ?? []
  const categoryFullSlug =
    (categoryDetail.value as { fullSlug?: string | null } | null)?.fullSlug ??
    null

  const resolvedCategories = breadcrumbs.reduce<ProductHeroBreadcrumb[]>(
    (acc, crumb, index, array) => {
      const rawTitle = crumb?.title ?? crumb?.link ?? ''
      const title = rawTitle.toString().trim()
      if (!title.length) {
        return acc
      }

      const crumbFullSlug =
        (crumb as { fullSlug?: string | null }).fullSlug ?? null
      const rawLink =
        crumbFullSlug ??
        (index === array.length - 1
          ? (categoryFullSlug ?? crumb?.link)
          : crumb?.link) ??
        null

      const trimmed = rawLink?.toString().trim() ?? ''
      const normalized = trimmed
        ? trimmed.startsWith('http')
          ? trimmed
          : trimmed.startsWith('/')
            ? trimmed
            : `/${trimmed}`
        : undefined

      acc.push({
        title,
        link: normalized ?? undefined,
      })

      return acc
    },
    []
  )

  const brandCrumb = brandBreadcrumb.value
  if (brandCrumb) {
    const duplicateIndex = resolvedCategories.findIndex(
      entry =>
        entry.title.trim().toLowerCase() ===
        brandCrumb.title.trim().toLowerCase()
    )

    if (duplicateIndex >= 0) {
      resolvedCategories.splice(duplicateIndex, 1, brandCrumb)
    } else {
      resolvedCategories.push(brandCrumb)
    }
  }

  // Fallback: If no category breadcrumbs (uncategorized), ensure we have Home
  if (
    resolvedCategories.length &&
    (!resolvedCategories[0].link || resolvedCategories[0].link !== '/')
  ) {
    // Check if the first item effectively is Home (e.g. title is Home but link is something else? Unlikely)
    // Simply check if we started with category crumbs. If categoryDetail is null, resolvedCategories came from empty array.
    if (!categoryDetail.value) {
      resolvedCategories.unshift({
        title: t('navigation.home'), // Ensure key exists or use fallback
        link: '/',
      })
    }
  }

  return resolvedCategories
})

const productMetaDescription = computed(() => {
  const explicit = product.value?.names?.metaDescription?.trim()
  if (explicit) {
    return explicit
  }

  const brandModelSuffix =
    brandModelTitle.value && brandModelTitle.value !== productMetaTitle.value
      ? ` ${brandModelTitle.value}`
      : ''

  if (normalizedVerticalTitle.value) {
    return t('product.meta.defaultDescriptionWithVertical', {
      productTitle: productMetaTitle.value,
      brandModel: brandModelSuffix,
      verticalTitle: normalizedVerticalTitle.value,
    })
  }

  return t('product.meta.defaultDescription', {
    productTitle: productMetaTitle.value,
    brandModel: brandModelSuffix,
  })
})

const toAbsoluteUrl = (value?: string | null) => {
  if (!value) {
    return undefined
  }

  try {
    return new URL(value, requestURL.origin).toString()
  } catch (error) {
    if (import.meta.dev) {
      console.warn('Failed to build absolute URL for product asset.', error)
    }

    return undefined
  }
}

const canonicalPath = computed(() => {
  const fallbackPath = route.path.startsWith('/')
    ? route.path
    : `/${route.path}`
  const preferredSlug = product.value?.fullSlug ?? product.value?.slug ?? null
  const normalizedSlug = preferredSlug
    ? preferredSlug.startsWith('/')
      ? preferredSlug
      : `/${preferredSlug}`
    : fallbackPath

  const sanitized = normalizedSlug.split('#')[0]?.split('?')[0]

  return sanitized?.length ? sanitized : fallbackPath
})

const canonicalUrl = computed(() =>
  new URL(canonicalPath.value, requestURL.origin).toString()
)
const productRobotsContent = computed(() =>
  categoryDetail.value?.enabled === false ? 'noindex, nofollow' : undefined
)

const resolvedProductImageSource = computed(() => {
  const galleryImages = product.value?.resources?.images ?? []
  const firstGalleryImage =
    galleryImages.find(image => Boolean(image?.url))?.url ??
    galleryImages.find(image => Boolean(image?.originalUrl))?.originalUrl

  return (
    product.value?.resources?.coverImagePath ??
    product.value?.resources?.externalCover ??
    product.value?.base?.coverImagePath ??
    firstGalleryImage ??
    null
  )
})

const ogImageUrl = computed(() => {
  const source = resolvedProductImageSource.value ?? '/nudger-icon-512x512.png'
  return toAbsoluteUrl(source)
})

const ogImageAlt = computed(() => productTitle.value)

const internalProductImageSource = computed(() => {
  const galleryImages = product.value?.resources?.images ?? []
  const firstGalleryImage =
    galleryImages.find(image => Boolean(image?.url))?.url ??
    galleryImages.find(image => Boolean(image?.originalUrl))?.originalUrl

  return (
    product.value?.resources?.coverImagePath ??
    product.value?.base?.coverImagePath ??
    firstGalleryImage ??
    null
  )
})

const schemaImageUrl = computed(() =>
  internalProductImageSource.value
    ? toAbsoluteUrl(internalProductImageSource.value)
    : undefined
)

useSeoMeta({
  title: () => productMetaTitle.value,
  description: () => productMetaDescription.value,
  ogTitle: () => product.value?.names?.ogTitle ?? productMetaTitle.value,
  ogDescription: () =>
    product.value?.names?.ogDescription ?? productMetaDescription.value,
  ogUrl: () => canonicalUrl.value,
  ogType: 'product',
  ogImage: () => ogImageUrl.value,
  ogImageAlt: () => ogImageAlt.value,
})

useHead(() => ({
  meta: productRobotsContent.value
    ? [{ name: 'robots', content: productRobotsContent.value }]
    : [],
}))

useHead(() => ({
  link: [{ rel: 'canonical', href: canonicalUrl.value }],
}))
const productScoreMap = computed<Record<string, ProductScoreDto | undefined>>(
  () => {
    return (product.value?.scores?.scores ?? {}) as Record<
      string,
      ProductScoreDto | undefined
    >
  }
)

const resolveProductScoreById = (scoreId: string): ProductScoreDto | null => {
  const normalized = typeof scoreId === 'string' ? scoreId.trim() : ''
  if (!normalized.length) {
    return null
  }

  const scores = productScoreMap.value
  if (scores[normalized]) {
    return scores[normalized] ?? null
  }

  const upper = normalized.toUpperCase()
  if (upper !== normalized && scores[upper]) {
    return scores[upper] ?? null
  }

  const lower = normalized.toLowerCase()
  if (lower !== normalized && scores[lower]) {
    return scores[lower] ?? null
  }

  return null
}

const isRenderableScore = (
  score: ProductScoreDto | undefined | null
): score is ProductScoreDto & { id: string; name: string } => {
  return (
    typeof score?.id === 'string' &&
    score.id.length > 0 &&
    typeof score.name === 'string' &&
    score.name.length > 0
  )
}

const selectedProductScores = computed(() => {
  return requestedScoreIds.value
    .map(scoreId => resolveProductScoreById(scoreId))
    .filter(isRenderableScore)
})

const ENERGY_CLASS_SCORE_IDS = new Set([
  'CLASSE_ENERGY',
  'CLASSE_ENERGY_SDR',
  'CLASSE_ENERGY_HDR',
])

const normalizeEprelValue = (value: unknown): string | null => {
  if (typeof value !== 'string') {
    return null
  }

  const trimmed = value.trim()
  return trimmed.length ? trimmed : null
}

const resolveCategorySpecificAttribute = (key: string): string | null => {
  const attributes = product.value?.eprel?.categorySpecificAttributes
  if (!attributes || typeof attributes !== 'object') {
    return null
  }

  return normalizeEprelValue((attributes as Record<string, unknown>)[key])
}

const resolveEnergyClassDisplay = (
  scoreId: string,
  attributeValue: string | null
): string | null => {
  const normalizedAttribute = attributeValue?.trim() ?? ''
  if (normalizedAttribute.length) {
    return normalizedAttribute
  }

  if (scoreId === 'CLASSE_ENERGY_SDR') {
    return resolveCategorySpecificAttribute('energyClassSDR')
  }

  if (scoreId === 'CLASSE_ENERGY_HDR') {
    return resolveCategorySpecificAttribute('energyClassHDR')
  }

  return (
    normalizeEprelValue(product.value?.eprel?.energyClass) ??
    resolveCategorySpecificAttribute('energyClass') ??
    resolveCategorySpecificAttribute('energyClassSDR') ??
    resolveCategorySpecificAttribute('energyClassHDR')
  )
}

const resolveEnergyClassImage = (scoreId: string): string | null => {
  const categorySpecific = resolveCategorySpecificAttribute(
    'energyClassImageWithScale'
  )
  const generic = normalizeEprelValue(product.value?.eprel?.energyClassImage)

  if (scoreId === 'CLASSE_ENERGY') {
    return categorySpecific ?? generic
  }

  if (scoreId === 'CLASSE_ENERGY_SDR' || scoreId === 'CLASSE_ENERGY_HDR') {
    return categorySpecific ?? generic
  }

  return null
}

const impactScores = computed(() => {
  const desiredScores = selectedProductScores.value
  const coefficients = scoreCoefficientMap.value

  return desiredScores.map(score => {
    const normalizedScoreId = score.id?.toString().trim().toUpperCase() ?? ''
    const attributeConfig = normalizedScoreId
      ? attributeConfigMap.value.get(normalizedScoreId)
      : undefined
    const criterion = normalizedScoreId
      ? availableImpactCriteriaMap.value.get(normalizedScoreId)
      : undefined
    const aggregation = aggregations.value[`score_${score.id}`]
    const distribution = (aggregation?.buckets ?? [])
      .map((bucket: AggregationBucketDto) => ({
        label: bucket.key != null ? String(bucket.key) : '',
        value: Number(bucket.count ?? 0),
      }))
      .filter(bucket => bucket.label.length > 0)

    // For ECOSCORE, use the absolute value; for subscores, use relative value
    const isEcoscore = score.id?.toUpperCase() === 'ECOSCORE'
    const absoluteScoreValue =
      typeof score.value === 'number' && Number.isFinite(score.value)
        ? score.value
        : null
    const relativeScoreValue =
      typeof score.relativ?.value === 'number' &&
      Number.isFinite(score.relativ.value)
        ? score.relativ.value
        : null

    const participateInScores = attributeConfig?.participateInScores
      ? Array.from(attributeConfig.participateInScores)
      : []
    const participateInACV = attributeConfig?.participateInACV
      ? Array.from(attributeConfig.participateInACV)
      : []
    const attribute = normalizedScoreId
      ? findIndexedAttribute(normalizedScoreId)
      : null
    const attributeValue = attribute
      ? resolveIndexedAttributeValue(attribute)
      : null
    const attributeSourcing = attribute?.sourcing ?? null
    const attributeSuffix = attributeConfig?.suffix ?? null
    const aggregates =
      (
        score as ProductScoreDto & {
          aggregates?: Record<string, number> | null
        }
      ).aggregates ?? null
    const isEnergyClassScore = ENERGY_CLASS_SCORE_IDS.has(normalizedScoreId)
    const energyClassDisplay = isEnergyClassScore
      ? resolveEnergyClassDisplay(normalizedScoreId, attributeValue)
      : null
    const energyClassImage = isEnergyClassScore
      ? resolveEnergyClassImage(normalizedScoreId)
      : null

    return {
      id: score.id,
      label: score.name,
      description: score.description ?? null,
      icon: attributeConfig?.icon ?? null,
      // For subscores, relativeValue should contain relativ.value for radar/table display
      relativeValue: isEcoscore ? absoluteScoreValue : relativeScoreValue,
      // For ECOSCORE, value should be the absolute value; for subscores, use relative
      value: isEcoscore ? absoluteScoreValue : relativeScoreValue,
      participateInScores,
      participateInACV,
      attributeValue,
      attributeSourcing,
      attributeSuffix,
      absoluteValue: isEnergyClassScore
        ? (energyClassDisplay ?? score.absoluteValue ?? null)
        : (score.absoluteValue ?? null),
      absolute: score.absolute ?? null,
      coefficient: coefficients[normalizedScoreId] ?? null,
      percent: score.percent ?? null,
      ranking: score.ranking ?? null,
      letter: score.letter ?? null,
      on20: score.on20 ?? null,
      distribution,
      energyLetter:
        score.id === 'CLASSE_ENERGY' && score.letter ? score.letter : null,
      energyClassDisplay,
      energyClassImage,
      metadatas: score.metadatas ?? null,
      unit: attributeConfig?.unit ?? attributeConfig?.suffix ?? null,
      aggregates,
      userBetterIs: attributeConfig?.userBetterIs ?? null,
      impactBetterIs: attributeConfig?.impactBetterIs ?? null,
      scoring: attributeConfig?.scoring ?? null,
      importanceDescription: criterion?.utility ?? null,
      virtual: score.virtual ?? false,
      numericMapping: attributeConfig?.numericMapping ?? null,
    }
  })
})

const formatBrandModelLabel = (
  brand: string,
  model: string,
  fallback: string
): string => {
  const normalizedBrand = brand.trim()
  const normalizedModel = model.trim()
  if (normalizedBrand && normalizedModel) {
    return `${normalizedBrand} - ${normalizedModel}`
  }

  if (normalizedBrand) {
    return normalizedBrand
  }

  if (normalizedModel) {
    return normalizedModel
  }

  return fallback.trim()
}

const resolveReferenceFallbackName = (
  reference: ProductReferenceDto | null | undefined
): string => {
  if (!reference) {
    return ''
  }

  const bestName =
    typeof reference.bestName === 'string' ? reference.bestName.trim() : ''
  if (bestName.length) {
    return bestName
  }

  const slug =
    typeof reference.fullSlug === 'string' ? reference.fullSlug.trim() : ''
  if (slug.length) {
    const segments = slug.split('/').filter(segment => segment.trim().length)
    const lastSegment = segments[segments.length - 1]
    if (lastSegment?.length) {
      return lastSegment
    }
  }

  return ''
}

const findIndexedAttribute = (
  attributeId: string
): ProductIndexedAttributeDto | null => {
  const attributes = product.value?.attributes?.indexedAttributes ?? {}
  const normalizedId = attributeId.trim()
  const candidates = [
    normalizedId,
    normalizedId.toUpperCase(),
    normalizedId.toLowerCase(),
  ]

  for (const candidate of candidates) {
    const attribute = attributes[candidate]
    if (attribute) {
      return attribute
    }
  }

  return null
}

const resolveIndexedAttributeValue = (
  attribute?: ProductIndexedAttributeDto | null
): string | null => {
  if (!attribute) {
    return null
  }

  if (
    typeof attribute.value === 'string' &&
    attribute.value.trim().length > 0
  ) {
    return attribute.value
  }

  if (
    typeof attribute.numericValue === 'number' &&
    Number.isFinite(attribute.numericValue)
  ) {
    return String(attribute.numericValue)
  }

  if (typeof attribute.booleanValue === 'boolean') {
    return attribute.booleanValue ? 'true' : 'false'
  }

  return null
}

const findIndexedAttributeValue = (attributeId: string): string | null =>
  resolveIndexedAttributeValue(findIndexedAttribute(attributeId))

const extractAbsoluteScoreValue = (
  score: ProductScoreDto | null | undefined
): number | null => {
  if (
    typeof score?.absolute?.value === 'number' &&
    Number.isFinite(score.absolute.value)
  ) {
    return score.absolute.value
  }

  return resolveScoreNumericValue(score)?.value ?? null
}

const extractReferenceScoreValue = (
  reference: ProductReferenceDto | null | undefined,
  scoreId: string
): number | null => {
  if (!reference?.scores) {
    return null
  }

  const rawScores =
    (reference.scores as
      | { scores?: Record<string, unknown> }
      | Record<string, unknown>
      | null) ?? null
  if (!rawScores) {
    return null
  }

  const scoreContainer =
    'scores' in rawScores &&
    rawScores.scores &&
    typeof rawScores.scores === 'object'
      ? (rawScores.scores as Record<string, unknown>)
      : (rawScores as Record<string, unknown>)

  const normalizedId = scoreId.trim()
  const candidates = [
    normalizedId,
    normalizedId.toUpperCase(),
    normalizedId.toLowerCase(),
  ]

  for (const candidate of candidates) {
    if (!candidate) {
      continue
    }

    const entry = scoreContainer[candidate]
    if (entry && typeof entry === 'object') {
      const asScore = entry as ProductScoreDto
      const absoluteValue =
        typeof asScore?.absolute?.value === 'number' &&
        Number.isFinite(asScore.absolute.value)
          ? asScore.absolute.value
          : null
      const resolved = resolveScoreNumericValue(asScore)

      if (absoluteValue != null) {
        return absoluteValue
      }

      if (resolved) {
        return resolved.value
      }
    }
  }

  return null
}

type RadarSeriesKey = 'current' | 'best' | 'worst'

interface RadarSeriesEntry {
  key: RadarSeriesKey
  name: string
  values: Array<number | null>
  rawValues?: Array<number | null>
}

interface RadarDataset {
  axes: Array<{
    id: string
    name: string
    attributeValue: string | null
    min?: number
    max?: number
  }>
  series: RadarSeriesEntry[]
}

const ecoscoreScore = computed(() => resolveProductScoreById('ECOSCORE'))
const bestReferenceProduct = computed<ProductReferenceDto | null>(
  () => ecoscoreScore.value?.highestScore ?? null
)
const worstReferenceProduct = computed<ProductReferenceDto | null>(
  () => ecoscoreScore.value?.lowestScore ?? null
)

const radarData = computed<RadarDataset>(() => {
  const scores = selectedProductScores.value
  const filteredScores = scores.filter(
    score => score.id?.trim().toUpperCase() !== 'ECOSCORE'
  )

  const axesDetails = filteredScores
    .map(score => {
      const id = score.id?.trim()
      if (!id) {
        return null
      }

      const name = score.name?.trim() ?? id
      const productValue = extractAbsoluteScoreValue(score)

      if (
        !(typeof productValue === 'number' && Number.isFinite(productValue))
      ) {
        return null
      }

      const attributeValue = findIndexedAttributeValue(id)
      const abs = score.absolute
      const min = typeof abs?.min === 'number' ? abs.min : null
      const max = typeof abs?.max === 'number' ? abs.max : null

      return {
        id,
        name,
        productValue,
        attributeValue,
        bestValue: extractReferenceScoreValue(bestReferenceProduct.value, id),
        worstValue: extractReferenceScoreValue(worstReferenceProduct.value, id),
        min,
        max,
      }
    })
    .filter(
      (
        entry
      ): entry is {
        id: string
        name: string
        productValue: number
        attributeValue: string | null
        bestValue: number | null
        worstValue: number | null
        min: number | null
        max: number | null
      } => Boolean(entry)
    )

  if (!axesDetails.length) {
    return { axes: [], series: [] }
  }

  const axes = axesDetails.map(({ id, name, attributeValue }) => {
    return {
      id,
      name,
      attributeValue,
      min: 0,
      max: 100,
    }
  })
  const productValues = axesDetails.map(entry => entry.productValue)
  const bestValues = axesDetails.map(entry => entry.bestValue ?? null)
  const worstValues = axesDetails.map(entry => entry.worstValue ?? null)

  const transformValuesIfNeeded = (values: (number | null)[]) => {
    return values.map((val, index) => {
      const axisId = axesDetails[index].id

      return transformRadarValue(
        val,
        {
          axisId,
          productValue: productValues[index],
          bestValue: bestValues[index],
          worstValue: worstValues[index],
          scaleMin: axesDetails[index].min ?? undefined,
          scaleMax: axesDetails[index].max ?? undefined,
        },
        attributeConfigMap.value
      )
    })
  }

  const productPlottedValues = transformValuesIfNeeded(productValues)
  const bestPlottedValues = transformValuesIfNeeded(bestValues)
  const worstPlottedValues = transformValuesIfNeeded(worstValues)

  const series: RadarSeriesEntry[] = []

  const productSeriesLabel = formatBrandModelLabel(
    productBrand.value,
    productModel.value,
    productTitle.value
  )

  if (
    productValues.some(
      value => typeof value === 'number' && Number.isFinite(value)
    )
  ) {
    series.push({
      key: 'current',
      name: productSeriesLabel,
      values: productPlottedValues,
      rawValues: productValues,
    })
  }

  const bestLabel = formatBrandModelLabel(
    typeof bestReferenceProduct.value?.brand === 'string'
      ? bestReferenceProduct.value.brand
      : '',
    typeof bestReferenceProduct.value?.model === 'string'
      ? bestReferenceProduct.value.model
      : '',
    resolveReferenceFallbackName(bestReferenceProduct.value)
  )

  if (
    bestValues.some(
      value => typeof value === 'number' && Number.isFinite(value)
    )
  ) {
    series.push({
      key: 'best',
      name: bestLabel,
      values: bestPlottedValues,
      rawValues: bestValues,
    })
  }

  const worstLabel = formatBrandModelLabel(
    typeof worstReferenceProduct.value?.brand === 'string'
      ? worstReferenceProduct.value.brand
      : '',
    typeof worstReferenceProduct.value?.model === 'string'
      ? worstReferenceProduct.value.model
      : '',
    resolveReferenceFallbackName(worstReferenceProduct.value)
  )

  if (
    worstValues.some(
      value => typeof value === 'number' && Number.isFinite(value)
    )
  ) {
    series.push({
      key: 'worst',
      name: worstLabel,
      values: worstPlottedValues,
      rawValues: worstValues,
    })
  }

  return {
    axes,
    series,
  }
})

const { data: commercialEventsData } = await useAsyncData<
  CommercialEvent[] | null
>(
  'commercial-events',
  async () => {
    try {
      return await $fetch<CommercialEvent[]>('/api/commercial-events')
    } catch (eventError) {
      console.error('Failed to load commercial events', eventError)
      return []
    }
  },
  { server: true }
)

const commercialEvents = computed(() => commercialEventsData.value ?? [])

const hcaptchaSiteKey = computed(
  () => runtimeConfig.public.hcaptchaSiteKey ?? ''
)

const showAdminSection = computed(() => isLoggedIn.value)
const showAttributesSection = computed(() => {
  const currentProduct = product.value
  if (!currentProduct) {
    return false
  }

  const identity = currentProduct.identity
  const hasIdentityStrings = Boolean(
    identity &&
    [identity.brand, identity.model, identity.bestName]
      .filter((value): value is string => typeof value === 'string')
      .some(value => value.trim().length > 0)
  )

  const hasCollectionValues = (input: unknown): boolean => {
    if (input instanceof Set) {
      return input.size > 0
    }

    if (Array.isArray(input)) {
      return input.length > 0
    }

    return false
  }

  const hasIdentityCollections =
    hasCollectionValues(identity?.akaBrands) ||
    hasCollectionValues(identity?.akaModels)

  const attributes = currentProduct.attributes
  const hasIndexed = Object.keys(attributes?.indexedAttributes ?? {}).length > 0
  const hasClassified = (attributes?.classifiedAttributes ?? []).some(group => {
    if (!group) {
      return false
    }

    return (
      (group.attributes?.length ?? 0) > 0 ||
      (group.features?.length ?? 0) > 0 ||
      (group.unFeatures?.length ?? 0) > 0
    )
  })

  return (
    hasIdentityStrings || hasIdentityCollections || hasIndexed || hasClassified
  )
})
const showAlternativesSection = computed(() =>
  Boolean(product.value && (categoryDetail.value?.id?.length ?? 0) > 0)
)
const alternativesHydrated = ref(false)
const hasAlternatives = ref(true)
const showAiReviewSection = computed(() => Boolean(categoryDetail.value))

const showVigilanceSection = computed(() => {
  if (!product.value) return false

  // Check End of Life
  const onMarketEndDate = product.value.eprel?.onMarketEndDate
  if (onMarketEndDate) {
    const normalized = normalizeTimestamp(onMarketEndDate)
    if (normalized) {
      const date = new Date(normalized)
      if (!isNaN(date.getTime()) && date < new Date()) {
        return true
      }
    }
  }

  // Check Conflicts
  const allAttributes = product.value.attributes?.allAttributes ?? {}
  const hasConflicts = Object.values(allAttributes).some(
    attr => attr.sourcing?.conflicts === true
  )
  if (hasConflicts) return true

  // Check Data Quality
  const dqScore = product.value.scores?.scores?.['DATA_QUALITY']
  if (dqScore) {
    const val = dqScore.value ?? 0
    const avg = dqScore.relativ?.avg ?? dqScore.absolute?.avg ?? 0
    if (val < avg) return true
  }

  return false
})

const sectionIds = {
  hero: 'hero',
  impact: 'impact',
  ai: 'synthese',
  price: 'prix',
  timeline: 'cycle-de-vie',
  alternatives: 'alternatives',
  attributes: 'caracteristiques',
  vigilance: 'vigilance',
  docs: 'documentation',
  adminPanel: 'admin-panel',
  adminJson: 'admin-json',
  adminDatasources: 'admin-datasources',
  adminRawApi: 'admin-raw-api',
} as const

const subSectionIds = {
  priceOffers: 'offers-list',
  priceHistory: 'price-history',
  attributesMain: 'attributes-main',
  attributesTimeline: 'attributes-timeline',
  attributesDetails: 'attributes-details',
  aiTechnical: 'ai-review-technical',
  aiEcological: 'ai-review-ecological',
  aiCommunity: 'ai-review-community',
} as const

type NavigableSection = {
  id: string
  label: string
  icon: string
  subsections?: Array<{ id: string; label: string; icon?: string }>
}
type ConditionalSection = NavigableSection & { condition: boolean }
type AdminNavigableSection = NavigableSection & {
  href?: string
  target?: string
}

const impactSubsections = computed(() => {
  if (!impactScores.value.length) {
    return []
  }

  const { groups, divers } = buildImpactScoreGroups(impactScores.value, t)
  const entries = groups.map(group => ({
    id: buildImpactAggregateAnchorId(group.id),
    label: group.label,
  }))

  if (divers) {
    entries.push({
      id: buildImpactAggregateAnchorId(divers.id),
      label: divers.label,
    })
  }

  return entries
})

const attributesSubsections = computed(() => {
  const entries = [
    {
      id: subSectionIds.attributesMain,
      label: t('product.navigation.submenus.attributes.summary'),
    },
  ]

  entries.push({
    id: subSectionIds.attributesDetails,
    label: t('product.navigation.submenus.attributes.details'),
  })

  return entries
})

const aiSubsections = computed(() => {
  const review = product.value?.aiReview?.review
  if (!review) {
    return []
  }

  // We add all sections as they are generally available when a review exists
  return [
    {
      id: subSectionIds.aiTechnical,
      label: t('product.aiReview.sections.technical'),
    },
    {
      id: subSectionIds.aiEcological,
      label: t('product.aiReview.sections.ecological'),
    },
    {
      id: subSectionIds.aiCommunity,
      label: t('product.aiReview.sections.community'),
    },
  ]
})

const shouldShowAlternativesNavigation = computed(
  () =>
    showAlternativesSection.value &&
    (!alternativesHydrated.value || hasAlternatives.value)
)

const primarySectionDefinitions = computed<ConditionalSection[]>(() => [
  {
    id: sectionIds.hero,
    label: t('product.navigation.overview'),
    icon: 'mdi-information-outline',
    condition: true,
  },
  {
    id: sectionIds.impact,
    label: t('product.navigation.impact'),
    icon: 'mdi-leaf',
    condition: impactScores.value.length > 0,
    subsections: impactSubsections.value,
  },
  {
    id: sectionIds.ai,
    label: t('product.navigation.ai'),
    icon: 'mdi-robot-outline',
    condition: showAiReviewSection.value,
    subsections: aiSubsections.value,
  },
  {
    id: sectionIds.price,
    label: t('product.navigation.price'),
    icon: 'mdi-currency-eur',
    condition: !!product.value?.offers,
    subsections: [
      {
        id: subSectionIds.priceOffers,
        label: t('product.navigation.submenus.price.bestOffers'),
      },
      {
        id: subSectionIds.priceHistory,
        label: t('product.navigation.submenus.price.history'),
      },
      ...(product.value?.timeline
        ? [
            {
              id: sectionIds.timeline,
              label: t('product.navigation.timeline'),
            },
          ]
        : []),
    ],
  },

  {
    id: sectionIds.alternatives,
    label: t('product.navigation.alternatives'),
    icon: 'mdi-compare-horizontal',
    condition: shouldShowAlternativesNavigation.value,
  },
  {
    id: sectionIds.vigilance,
    label: t('product.vigilance.title'),
    icon: 'mdi-alert-circle-outline',
    condition: showVigilanceSection.value,
  },
  {
    id: sectionIds.attributes,
    label: t('product.navigation.attributes'),
    icon: 'mdi-format-list-bulleted',
    condition: showAttributesSection.value,
    subsections: attributesSubsections.value,
  },
  {
    id: sectionIds.docs,
    label: t('product.navigation.docs'),
    icon: 'mdi-file-document-outline',
    condition: (product.value?.resources?.pdfs?.length ?? 0) > 0,
  },
])

const primarySections = computed<NavigableSection[]>(() =>
  primarySectionDefinitions.value
    .filter(section => section.condition)
    .map(({ condition: _condition, ...rest }) => rest)
)

const adminApiPayloadUrl = computed(() => {
  const gtinValue = product.value?.gtin ?? gtin
  return gtinValue
    ? `https://api.nudger.fr/product/?gtin=${gtinValue}`
    : 'https://api.nudger.fr/product/'
})

const adminSections = computed<AdminNavigableSection[]>(() => {
  if (!showAdminSection.value) {
    return []
  }

  return [
    {
      id: sectionIds.attributes,
      label: t('product.navigation.attributes'),
      icon: 'mdi-format-list-bulleted',
    },
    {
      id: sectionIds.adminDatasources,
      label: t('product.navigation.adminPanel.items.datasources'),
      icon: 'mdi-database-outline',
    },
    {
      id: sectionIds.adminJson,
      label: t('product.navigation.adminPanel.items.productJson'),
      icon: 'mdi-code-json',
    },
    {
      id: sectionIds.adminRawApi,
      label: t('product.navigation.adminPanel.items.rawApi'),
      icon: 'mdi-open-in-new',
      href: adminApiPayloadUrl.value,
      target: '_blank',
    },
  ]
})

const adminScrollSections = computed<NavigableSection[]>(() =>
  adminSections.value.filter(section => !section.href)
)

const sections = computed<NavigableSection[]>(() => [
  ...primarySections.value,
  ...adminScrollSections.value,
])

const navigableSections = computed(() => primarySections.value)
const adminNavigableSections = computed(() => adminSections.value)

const orientation = computed<'vertical' | 'horizontal'>(() =>
  display.mdAndDown.value ? 'horizontal' : 'vertical'
)

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
  const [nextActive] =
    sorted.find(([, ratio]) => ratio >= MIN_SECTION_RATIO) ?? sorted[0] ?? []

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
    entries => {
      entries.forEach(entry => {
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
    }
  )

  nextTick(() => {
    sections.value.forEach(section => {
      const element = document.getElementById(section.id)
      if (element) {
        observer.value?.observe(element)
      }
    })
  })
}

/*
watch(
  () => scrollY.value,
  (current, previous) => {
    // ... removed redundant logic that was causing ReferenceError (stickyBannerThresholdRatio undefined)
    // Sticky banner toggle is already handled by IntersectionObserver on the impact section
  },
  { flush: 'post' }
)
*/

onMounted(() => {
  observeSections()
})

watch(
  sections,
  () => {
    observeSections()
  },
  { flush: 'post' }
)

onBeforeUnmount(() => {
  observer.value?.disconnect()
  visibleSectionRatios.clear()
})

const expandedScoreId = ref<string | null>(null)

const impactAnchorToScoreIdMap = computed(() => {
  const map = new Map<string, string>()
  const { groups, divers } = buildImpactScoreGroups(impactScores.value, t)

  groups.forEach(group => {
    map.set(buildImpactAggregateAnchorId(group.id), group.id)
  })

  if (divers) {
    map.set(buildImpactAggregateAnchorId(divers.id), divers.id)
  }

  return map
})

const scrollToSection = async (sectionId: string) => {
  if (!import.meta.client) {
    return
  }

  let targetId = sectionId
  const scoreId = impactAnchorToScoreIdMap.value.get(sectionId)

  if (scoreId) {
    expandedScoreId.value = scoreId
    await nextTick()
    if (document.getElementById(sectionId)) {
      targetId = sectionId
    } else {
      targetId = sectionIds.impact
    }
  }

  const element = document.getElementById(targetId)
  if (!element) {
    return
  }

  activeSection.value = targetId

  const offset = orientation.value === 'horizontal' ? 96 : 120
  const top = element.getBoundingClientRect().top + window.scrollY - offset
  window.scrollTo({ top, behavior: 'smooth' })
}

const errorMessage = computed(() => {
  if (error.value) {
    if (error.value instanceof Error) {
      return error.value.message
    }

    return String(error.value)
  }

  if (productLoadError.value) {
    return productLoadError.value.message
  }

  return null
})

const structuredOffers = computed(() => {
  const offersByCondition = product.value?.offers?.offersByCondition ?? {}
  const normalizedOffers = Object.values(offersByCondition)
    .flatMap(entries => entries ?? [])
    .filter(
      (
        offer
      ): offer is {
        price: number
        url: string
        currency?: string | null
        condition?: string | null
        datasourceName?: string | null
      } => typeof offer?.price === 'number' && Boolean(offer?.url)
    )
    .map(offer => ({
      '@type': 'Offer',
      price: offer.price,
      priceCurrency: offer.currency ?? 'EUR',
      url: offer.url,
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

  return normalizedOffers.length > 0 ? normalizedOffers : undefined
})

const impactScoreValue = computed(() => {
  if (!product.value) {
    return null
  }
  return resolvePrimaryImpactScore(product.value)
})

const impactScoreOutOf20 = computed(() => {
  const score = impactScoreValue.value

  if (typeof score !== 'number') {
    return null
  }

  return Number((score * 4).toFixed(1))
})

const impactScoreMin = computed(() => {
  const absolute = product.value?.scores?.scores?.ECOSCORE?.absolute
  return typeof absolute?.min === 'number' ? absolute.min : 0
})

const impactScoreMax = computed(() => {
  const absolute = product.value?.scores?.scores?.ECOSCORE?.absolute
  return typeof absolute?.max === 'number' ? absolute.max : 20
})

const reviewStructuredData = computed(() => {
  const review = product.value?.aiReview?.review
  const score = impactScoreValue.value

  if (!review?.summary && !score) {
    return null
  }

  const createdTimestamp = product.value?.aiReview?.createdMs

  return {
    '@type': 'Review',
    reviewBody: review?.summary ?? undefined,
    name: review?.shortTitle ?? productTitle.value,
    author: {
      '@type': 'Organization',
      name: 'Nudger IA',
    },
    reviewRating:
      typeof score === 'number'
        ? {
            '@type': 'Rating',
            ratingValue: score,
            bestRating: 5,
            worstRating: 0,
          }
        : undefined,
    dateCreated:
      typeof createdTimestamp === 'number'
        ? new Date(createdTimestamp).toISOString()
        : undefined,
  }
})

const breadcrumbStructuredData = computed(() => {
  if (!productBreadcrumbs.value.length) {
    return null
  }

  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: productBreadcrumbs.value.map((crumb, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      name: crumb.title,
      item: crumb.link ? toAbsoluteUrl(crumb.link) : undefined,
    })),
  }
})

const productStructuredData = computed(() => {
  if (!product.value) {
    return null
  }

  const aggregateRatingValue =
    typeof product.value.scores?.ecoscore?.absolute?.value === 'number'
      ? product.value.scores.ecoscore.absolute.value
      : null

  const offers = structuredOffers.value
  const imageUrl = schemaImageUrl.value

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
    offers,
    aggregateRating: aggregateRatingValue
      ? {
          '@type': 'AggregateRating',
          ratingValue: aggregateRatingValue,
          reviewCount: product.value.scores?.ranking?.globalCount ?? 1,
        }
      : undefined,
    review: reviewStructuredData.value ?? undefined,
    image: imageUrl ?? undefined,
    url: canonicalUrl.value,
    gtin13: String(
      product.value.base?.gtin ?? product.value.gtin ?? ''
    ).padStart(13, '0'),
    additionalProperty: aggregateRatingValue
      ? [
          {
            '@type': 'PropertyValue',
            name: 'EcoScore',
            value: aggregateRatingValue,
            minValue: 0,
            maxValue: 100,
          },
        ]
      : undefined,
  }
})

const impactScoreOn20 = computed(() => {
  const ecoScore = impactScores.value.find(
    s => s.id?.toUpperCase() === 'ECOSCORE'
  )
  return ecoScore?.on20 != null ? ecoScore.on20 : null
})

const metaTitle = computed(() => {
  const title = productTitle.value
  const score = impactScoreOn20.value

  if (title.length < 35 && score != null) {
    return `${title}${t('product.meta.impactScore', { score })}`
  }

  return title
})

useSeoMeta({
  title: () => metaTitle.value,
  description: () => productMetaDescription.value,
  ogTitle: () => product.value?.names?.ogTitle ?? metaTitle.value,
  ogDescription: () =>
    product.value?.names?.ogDescription ?? productMetaDescription.value,
  ogUrl: () => canonicalUrl.value,
  ogType: 'product',
  ogImage: () => ogImageUrl.value,
  ogImageAlt: () => ogImageAlt.value,
})

useHead(() => ({
  meta: productRobotsContent.value
    ? [{ name: 'robots', content: productRobotsContent.value }]
    : [],
}))

useHead(() => ({
  link: [{ rel: 'canonical', href: canonicalUrl.value }],
}))

useHead(() => {
  const scripts = [] as { type: string; key: string; children: string }[]

  if (productStructuredData.value) {
    scripts.push({
      key: 'product-structured-data',
      type: 'application/ld+json',
      children: JSON.stringify(productStructuredData.value),
    })
  }

  if (breadcrumbStructuredData.value) {
    scripts.push({
      key: 'breadcrumb-structured-data',
      type: 'application/ld+json',
      children: JSON.stringify(breadcrumbStructuredData.value),
    })
  }

  return {
    script: scripts,
  }
})
</script>

<style scoped>
.product-page {
  position: relative;
  padding: 2rem 0;
  overflow-x: hidden;
}

.product-page::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background:
    linear-gradient(
      90deg,
      rgba(var(--v-theme-hero-overlay-strong), 0.95) 0%,
      rgba(var(--v-theme-hero-overlay-strong), 0) 60%
    ),
    linear-gradient(
      0deg,
      rgba(var(--v-theme-hero-overlay-strong), 0.9) 0%,
      rgba(var(--v-theme-hero-overlay-strong), 0) 55%
    ),
    linear-gradient(
      135deg,
      rgba(var(--v-theme-hero-gradient-start), 0.18),
      rgba(var(--v-theme-hero-gradient-end), 0.2)
    );
}

.product-page > *:not(.product-sticky-banner) {
  position: relative;
  z-index: 1;
}

.product-page__layout {
  display: grid;
  grid-template-columns: minmax(240px, 280px) minmax(0, 1fr);
  gap: 2rem;
}

.product-page__nav {
  position: sticky;
  top: 108px; /* 64px header + 44px banner */
  align-self: start;
  height: fit-content;
}

.product-page__nav--mobile {
  position: static;
  top: auto;
  transform: none;
  align-self: stretch;
}

.product-page__content {
  display: flex;
  flex-direction: column;
  gap: 3rem;
  max-width: 100%;
  min-width: 0;
}

.product-page__section {
  scroll-margin-top: 108px; /* Match sticky nav + banner */
}

.product-page__hero {
  position: relative;
}

.product-page__hero :deep(.product-hero__background) {
  display: none;
}

.product-page__hero-corner {
  position: absolute;
  top: -1.75rem; /* Adjust based on panel padding */
  left: -1.75rem; /* Adjust based on panel padding */
  width: 100px;
  height: 100px;
}

.product-page__hero-corner-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 0.5rem 0.6rem;
  text-align: center;
  transform: rotate(-12deg);
}

.product-page__hero-corner-value {
  font-size: 1.8rem;
  font-weight: 700;
  line-height: 0.9;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-page__hero-corner-label {
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  opacity: 0.8;
  margin-top: 0.1rem;
}

.product-page__hero-corner-fallback {
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  text-align: center;
  line-height: 1.1;
}

.product-page__fab {
  position: fixed;
  right: 1.5rem;
  bottom: 1.5rem;
  z-index: 24;
}

.product-page__fab-label {
  font-weight: 600;
  margin-inline-start: 0.5rem;
}

@media (max-width: 1280px) {
  .product-page__layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .product-page__nav {
    position: sticky;
    top: 0;
    z-index: 20;
    transform: none;
    align-self: stretch;
  }

  .product-page__section {
    scroll-margin-top: 140px;
  }
}

@media (max-width: 960px) {
  .product-page__fab {
    right: 1rem;
    bottom: 1rem;
  }
}

@media (max-width: 768px) {
  .product-page {
    padding: 1rem 0;
  }

  .product-page__content {
    gap: 2rem;
  }

  .product-page__section {
    scroll-margin-top: 80px;
  }
}

.product-page__unrated {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 2rem;
  border-radius: 26px;
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-surface-primary-100), 0.95),
    rgba(var(--v-theme-surface-glass-strong), 0.9)
  );
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.12);
  align-items: center;
  justify-content: center;
  text-align: center;
  margin-bottom: 2rem;
}

.product-page__unrated-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
}

.product-page__unrated-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
}

.product-page__unrated-desc {
  font-size: 1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  margin: 0;
}

.product-page__unrated-cta {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.1rem;
  border-radius: 999px;
  text-decoration: none;
  font-weight: 600;
  font-size: 0.95rem;
  color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-surface-default), 0.9);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.12);
  transition: all 0.2s ease;
  margin-top: 1rem;
}

.product-page__unrated-cta:hover,
.product-page__unrated-cta:focus-visible {
  transform: translateY(-2px);
  background: rgba(var(--v-theme-surface-default), 1);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.18);
}
</style>
