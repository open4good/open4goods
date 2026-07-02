<template>
  <section class="product-hero">
    <div
      v-if="showDecorativeBackground"
      class="product-hero__background"
      aria-hidden="true"
    >
      <img
        class="product-hero__background-media"
        :src="heroBackground"
        alt=""
        decoding="async"
        loading="lazy"
        fetchpriority="low"
      />
      <div class="product-hero__background-overlay" />
    </div>
    <div class="product-hero__content">
      <Transition name="product-hero-rise" appear>
        <v-row class="product-hero__layout align-stretch" density="comfortable">
          <v-col
            v-if="galleryItems.length > 0"
            cols="12"
            md="4"
            class="product-hero__media-column"
          >
            <Transition name="fade">
              <ProductHeroInlineGallery
                v-if="showInlineGallery"
                v-model:index="activeInlineGalleryIndex"
                :items="galleryItems"
                :start-index="activeInlineGalleryIndex"
                @close="handleCloseInlineGallery"
              />
            </Transition>

            <div class="product-hero__gallery-section">
              <ProductHeroGallery
                class="product-hero__gallery"
                :product="product"
                :title="heroTitle"
                @open-inline-gallery="handleOpenInlineGallery"
              />
            </div>
          </v-col>

          <v-col
            cols="12"
            :md="galleryItems.length > 0 ? 8 : 12"
            class="product-hero__decision-column"
          >
            <div class="product-hero__decision-shell">
              <header class="product-hero__heading">
                <CategoryNavigationBreadcrumbs
                  v-if="visibleBreadcrumbs.length"
                  v-bind="heroBreadcrumbProps"
                  class="product-hero__breadcrumbs"
                />
                <ProductDesignation
                  :product="product"
                  variant="page"
                  title-tag="h1"
                  title-class="product-hero__title"
                  description-class="product-hero__short-description"
                  :title-prefix="verticalTitle"
                />
              </header>

              <ProductVerdictPanel
                :product="product"
                class="product-hero__verdict"
                @navigate="handleVerdictNavigate"
              />

              <div
                class="product-hero__decision-grid"
                :class="{
                  'product-hero__decision-grid--single': hidePricingPanel,
                }"
              >
                <div class="product-hero__details-section">
                  <div
                    v-if="impactScore != null"
                    class="product-hero__impact-card cursor-pointer"
                    @click="handleImpactScoreClick"
                  >
                    <ImpactScore
                      :score="impactScore"
                      :min="impactScoreMin"
                      :max="impactScoreMax"
                      :show-methodology="false"
                      :show-min-max="false"
                      size="lg"
                      :banner="true"
                      :brand="productBrandName"
                      :model="productModelName"
                      :category="productVerticalName"
                      :ranking="ecoscoreRanking"
                      :count="ecoscoreCount"
                    />
                  </div>

                  <div v-if="hasCategory" class="product-hero__actions">
                    <v-btn
                      class="product-hero__compare-button"
                      :class="{
                        'product-hero__compare-button--active':
                          isCompareSelected,
                      }"
                      variant="flat"
                      :aria-pressed="isCompareSelected"
                      :aria-label="compareButtonAriaLabel"
                      :title="compareButtonTitle"
                      :disabled="isCompareDisabled"
                      @click="toggleCompare"
                    >
                      <v-icon
                        :icon="compareButtonIcon"
                        size="20"
                        class="product-hero__compare-icon"
                      />
                      <span class="product-hero__compare-label">{{
                        compareButtonText
                      }}</span>
                    </v-btn>
                  </div>

                  <div class="product-hero__centered-info">
                    <v-btn
                      v-if="hasCategory"
                      variant="text"
                      class="product-hero__more-characteristics"
                      @click="handleMoreCharacteristicsClick"
                    >
                      {{ t('product.hero.moreCharacteristics') }}
                    </v-btn>
                    <ul
                      v-if="heroAttributes.length"
                      class="product-hero__attributes"
                      role="list"
                    >
                      <li
                        v-for="attribute in heroAttributes"
                        :key="attribute.key"
                        class="product-hero__attribute"
                        :class="{
                          'product-hero__attribute--country':
                            attribute.key === 'gtin-country',
                        }"
                        role="listitem"
                      >
                        <template v-if="attribute.key === 'descriptions-panel'">
                          <div class="product-hero__descriptions-panel">
                            <v-tabs
                              v-model="activeDescriptionTab"
                              density="compact"
                              color="primary"
                              align-tabs="start"
                              class="mb-4 product-hero__tabs"
                            >
                              <v-tab
                                v-for="(desc, source) in productDescriptions"
                                :key="source"
                                :value="source"
                                class="text-body-2 font-weight-bold"
                              >
                                <img
                                  v-if="productFavicons[source]"
                                  :src="productFavicons[source]"
                                  width="16"
                                  height="16"
                                  class="mr-2 rounded-circle"
                                  alt=""
                                />
                                {{ source }}
                              </v-tab>
                            </v-tabs>

                            <v-window
                              v-model="activeDescriptionTab"
                              class="product-hero__tab-content"
                            >
                              <v-window-item
                                v-for="(desc, source) in productDescriptions"
                                :key="source"
                                :value="source"
                              >
                                <!-- eslint-disable vue/no-v-html -->
                                <div class="product-hero__description-wrapper">
                                  <div
                                    class="text-body-2 text-medium-emphasis product-hero__description-body"
                                    v-html="getDisplayDescription(source, desc)"
                                  />
                                  <v-btn
                                    v-if="shouldShowToggle(desc)"
                                    variant="text"
                                    size="small"
                                    color="primary"
                                    class="mt-2"
                                    @click="toggleDescription(source)"
                                  >
                                    {{
                                      isDescriptionExpanded(source)
                                        ? t('product.hero.description.showLess')
                                        : t('product.hero.description.showMore')
                                    }}
                                  </v-btn>
                                </div>
                                <!-- eslint-enable vue/no-v-html -->
                              </v-window-item>
                            </v-window>
                          </div>
                        </template>
                        <template v-else-if="attribute.showLabel !== false">
                          <span class="product-hero__attribute-label">{{
                            attribute.label
                          }}</span>
                          <span
                            class="product-hero__attribute-separator"
                            aria-hidden="true"
                            >:</span
                          >
                        </template>
                        <span
                          v-if="attribute.key !== 'ai-summary'"
                          class="product-hero__attribute-value"
                        >
                          <ProductAttributeSourcingLabel
                            class="product-hero__attribute-value-label"
                            :sourcing="attribute.sourcing"
                            :value="attribute.value"
                            :enable-tooltip="attribute.enableTooltip !== false"
                          >
                            <template #default="{ displayValue, displayHtml }">
                              <v-tooltip
                                location="bottom"
                                :text="attribute.tooltip"
                                :disabled="!attribute.tooltip"
                              >
                                <template #activator="{ props: tooltipProps }">
                                  <span
                                    class="product-hero__attribute-value-content"
                                    v-bind="tooltipProps"
                                  >
                                    <NuxtImg
                                      v-if="attribute.flag"
                                      :src="attribute.flag"
                                      :alt="displayValue"
                                      width="32"
                                      height="24"
                                      class="product-hero__flag"
                                    />
                                    <v-icon
                                      v-if="attribute.icon"
                                      :icon="attribute.icon"
                                      size="small"
                                      class="product-hero__icon mr-2"
                                    />
                                    <!-- eslint-disable vue/no-v-html -->
                                    <div
                                      v-if="displayHtml"
                                      v-html="displayHtml"
                                    />
                                    <!-- eslint-enable vue/no-v-html -->
                                    <span v-else>{{ displayValue }}</span>
                                  </span>
                                </template>
                              </v-tooltip>
                            </template>
                          </ProductAttributeSourcingLabel>
                        </span>
                      </li>
                    </ul>
                  </div>
                </div>

                <aside
                  v-if="!hidePricingPanel"
                  class="product-hero__panel product-hero__panel--pricing"
                >
                  <ProductHeroPricing :product="product" />
                </aside>
              </div>
            </div>
          </v-col>
        </v-row>
      </Transition>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, ref, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { mdiMinus, mdiPlus } from '@mdi/js'
import CategoryNavigationBreadcrumbs from '~/components/category/navigation/CategoryNavigationBreadcrumbs.vue'
import ProductHeroPricing from '~/components/product/ProductHeroPricing.vue'
import ProductDesignation from '~/components/product/ProductDesignation.vue'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import {
  MAX_COMPARE_ITEMS,
  useProductCompareStore,
  type CompareListBlockReason,
} from '~/stores/useProductCompareStore'
import ProductAttributeSourcingLabel from '~/components/product/attributes/ProductAttributeSourcingLabel.vue'
import {
  formatAttributeValue,
  resolvePopularAttributes,
} from '~/utils/_product-attributes'
import {
  resolveProductLongName,
  resolveProductShortName,
} from '~/utils/_product-title-resolver'
import { useThemedAsset } from '~/composables/useThemedAsset'
import { useProductGallery } from '~/composables/useProductGallery'

import type {
  AttributeConfigDto,
  ProductAttributeSourceDto,
  ProductDto,
} from '~~/shared/api-client'

export interface ProductHeroBreadcrumb {
  title: string
  link?: string
}

const ProductHeroGallery = defineAsyncComponent(
  () => import('~/components/product/ProductHeroGallery.vue')
)
const ProductHeroInlineGallery = defineAsyncComponent(
  () => import('~/components/product/ProductHeroInlineGallery.vue')
)

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
  breadcrumbs: {
    type: Array as PropType<ProductHeroBreadcrumb[]>,
    default: () => [],
  },
  popularAttributes: {
    type: Array as PropType<AttributeConfigDto[]>,
    default: () => [],
  },
  impactScore: {
    type: Number,
    default: null,
  },
  impactScoreMin: {
    type: Number,
    default: 0,
  },
  impactScoreMax: {
    type: Number,
    default: 20,
  },
  image: {
    type: String,
    default: null,
  },
  hasCategory: {
    type: Boolean,
    default: false,
  },
  verticalTitle: {
    type: String,
    default: '',
  },
  hidePricingPanel: {
    type: Boolean,
    default: false,
  },
  showDecorativeBackground: {
    type: Boolean,
    default: false,
  },
})

const { t, te, n, locale } = useI18n()
const heroBackground = useThemedAsset('product/product-hero-background.svg')

const handleVerdictNavigate = (section: 'impact' | 'price' | 'reliability') => {
  let targetId = 'impact'
  if (section === 'price') {
    targetId = 'prix'
  } else if (section === 'reliability') {
    targetId = document.getElementById('vigilance')
      ? 'vigilance'
      : 'caracteristiques'
  }
  const element = document.getElementById(targetId)
  if (element) {
    const offset = 120
    const top = element.getBoundingClientRect().top + window.scrollY - offset
    window.scrollTo({ top, behavior: 'smooth' })
  }
}

const handleImpactScoreClick = () => {
  const element = document.getElementById('impact')
  if (element) {
    const offset = 120 // Adjust based on header height
    const top = element.getBoundingClientRect().top + window.scrollY - offset
    window.scrollTo({ top, behavior: 'smooth' })
  }
}

const handleMoreCharacteristicsClick = () => {
  const element =
    document.getElementById('caracteristiques') ||
    document.querySelector('.product-attributes')

  if (element) {
    const offset = 120
    const top = element.getBoundingClientRect().top + window.scrollY - offset
    window.scrollTo({ top, behavior: 'smooth' })
  }
}

const normalizeString = (value: string | null | undefined) =>
  typeof value === 'string' ? value.trim() : ''

const heroTitle = computed(() => {
  return resolveProductLongName(props.product, locale.value)
})

// Product Gallery Logic
const { galleryItems } = useProductGallery(
  computed(() => props.product),
  heroTitle.value
)
const showInlineGallery = ref(false)
const activeInlineGalleryIndex = ref(0)

const handleOpenInlineGallery = (index: number) => {
  activeInlineGalleryIndex.value = index
  showInlineGallery.value = true
}

const handleCloseInlineGallery = () => {
  showInlineGallery.value = false
}

const productVerticalName = computed(() =>
  normalizeString(props.product.names?.seoName)
)
const productBrandName = computed(() =>
  normalizeString(props.product.identity?.brand)
)
const productModelName = computed(() =>
  normalizeString(props.product.identity?.model)
)

const ecoscoreRanking = computed(
  () => props.product.scores?.scores?.ECOSCORE?.ranking ?? null
)
const ecoscoreCount = computed(
  () => props.product.scores?.scores?.ECOSCORE?.absolute?.count ?? null
)

const productDescriptions = computed(
  () => props.product.datasources?.descriptions ?? {}
)
const productFavicons = computed(
  () => props.product.datasources?.favicons ?? {}
)
const hasDescriptions = computed(
  () => Object.keys(productDescriptions.value).length > 0
)
const activeDescriptionTab = ref<string | null>(null)

// Select first tab by default
watch(
  hasDescriptions,
  val => {
    if (val && !activeDescriptionTab.value) {
      activeDescriptionTab.value = Object.keys(productDescriptions.value)[0]
    }
  },
  { immediate: true }
)

const DESCRIPTION_MAX_CHARS = 175

// Track expanded state for each description source
const expandedDescriptions = ref<Record<string, boolean>>({})

const stripHtmlTags = (html: string): string => {
  return html.replace(/<[^>]*>/g, '')
}

const truncateDescription = (html: string, maxChars: number): string => {
  const plainText = stripHtmlTags(html)
  if (plainText.length <= maxChars) {
    return html
  }

  // Truncate plain text and add ellipsis
  const truncated = plainText.substring(0, maxChars).trim()

  // Simple approach: truncate and add ellipsis
  return truncated + '...'
}

const shouldShowToggle = (description: string): boolean => {
  const plainText = stripHtmlTags(description)
  return plainText.length > DESCRIPTION_MAX_CHARS
}

const isDescriptionExpanded = (source: string): boolean => {
  return expandedDescriptions.value[source] ?? false
}

const toggleDescription = (source: string) => {
  expandedDescriptions.value[source] = !isDescriptionExpanded(source)
}

const getDisplayDescription = (source: string, description: string): string => {
  if (isDescriptionExpanded(source) || !shouldShowToggle(description)) {
    return description
  }
  return truncateDescription(description, DESCRIPTION_MAX_CHARS)
}

const brandModelLine = computed(() => {
  const parts = [productBrandName.value, productModelName.value].filter(
    value => value.length
  )
  return parts.join(' - ')
})

type DisplayedAttribute = { key: string; label: string; value: string }

type HeroAttribute = DisplayedAttribute & {
  flag?: string | null
  tooltip?: string
  sourcing?: ProductAttributeSourceDto | null
  enableTooltip?: boolean
  showLabel?: boolean
  icon?: string
}

const popularAttributeConfigs = computed(() => props.popularAttributes ?? [])

const resolveIndexedAttributeSourcing = (
  key: string
): ProductAttributeSourceDto | null => {
  const indexedAttributes = props.product.attributes?.indexedAttributes ?? {}

  if (key in indexedAttributes && indexedAttributes[key]?.sourcing) {
    return indexedAttributes[key]?.sourcing ?? null
  }

  if (key.includes('.')) {
    const segments = key.split('.')
    const lastSegment = segments[segments.length - 1]
    if (lastSegment && indexedAttributes[lastSegment]?.sourcing) {
      return indexedAttributes[lastSegment]?.sourcing ?? null
    }
  }

  return null
}

const popularAttributes = computed<HeroAttribute[]>(() =>
  resolvePopularAttributes(props.product, popularAttributeConfigs.value)
    .map(attribute => {
      const value = formatAttributeValue(attribute, t, n)
      if (!value) {
        return null
      }

      const sourcing = resolveIndexedAttributeSourcing(attribute.key)

      return {
        key: attribute.key,
        label: attribute.label,
        value,
        sourcing,
        enableTooltip: false,
      }
    })
    .filter((attribute): attribute is HeroAttribute => attribute != null)
)

const heroAttributes = computed<HeroAttribute[]>(() => {
  const baseAttributes: HeroAttribute[] = []

  if (hasDescriptions.value) {
    baseAttributes.push({
      key: 'descriptions-panel',
      label: '',
      value: '',
      showLabel: false,
      enableTooltip: false,
    })
  } else {
    baseAttributes.push(...popularAttributes.value)
  }

  return baseAttributes
})

const compareStore = useProductCompareStore()

const reasonMessage = (reason: CompareListBlockReason | undefined) => {
  switch (reason) {
    case 'limit-reached':
      return t('category.products.compare.limitReached', {
        count: MAX_COMPARE_ITEMS,
      })
    case 'vertical-mismatch':
      return t('category.products.compare.differentCategory')
    case 'missing-identifier':
      return t('category.products.compare.missingIdentifier')
    default:
      return t('product.hero.compare.add')
  }
}

const compareEligibility = computed(() =>
  compareStore.canAddProduct(props.product)
)
const isCompareSelected = computed(() => compareStore.hasProduct(props.product))

const compareButtonText = computed(() =>
  isCompareSelected.value
    ? t('product.hero.compare.remove')
    : t('product.hero.compare.add')
)

const compareButtonTitle = computed(() => {
  if (isCompareSelected.value) {
    return t('product.hero.compare.remove')
  }

  if (!compareEligibility.value.success) {
    return reasonMessage(compareEligibility.value.reason)
  }

  return t('product.hero.compare.add')
})

const compareButtonAriaLabel = computed(() => {
  const productName =
    heroTitle.value ||
    resolveProductShortName(props.product, locale.value) ||
    ''

  if (isCompareSelected.value) {
    if (te('product.hero.compare.ariaSelected')) {
      return t('product.hero.compare.ariaSelected', { name: productName })
    }

    return t('product.hero.compare.remove')
  }

  if (!compareEligibility.value.success) {
    return reasonMessage(compareEligibility.value.reason)
  }

  if (te('product.hero.compare.ariaAdd')) {
    return t('product.hero.compare.ariaAdd', { name: productName })
  }

  return t('product.hero.compare.add')
})

const compareButtonIcon = computed(() =>
  isCompareSelected.value ? mdiMinus : mdiPlus
)

const isCompareDisabled = computed(
  () => !isCompareSelected.value && !compareEligibility.value.success
)

const toggleCompare = () => {
  if (isCompareDisabled.value) {
    return
  }

  compareStore.toggleProduct(props.product)
}

const itemIsProduct = (
  breadcrumb: ProductHeroBreadcrumb,
  productTitle: string,
  productSlug: string
) => {
  const rawTitle = breadcrumb?.title ?? breadcrumb?.link ?? ''
  const trimmedTitle = rawTitle.toString().trim()
  const titleValue = trimmedTitle.length
    ? trimmedTitle
    : t('product.hero.missingBreadcrumbTitle')

  if (!titleValue.trim().length) {
    return false
  }

  const normalizedTitle = titleValue.trim().toLowerCase()
  const normalizedLinkValue =
    breadcrumb?.link?.toString().trim().toLowerCase() ?? ''

  const matchesProductTitle = productTitle.length
    ? normalizedTitle === productTitle
    : false
  const matchesProductLink = productSlug.length
    ? normalizedLinkValue.endsWith(productSlug)
    : false

  return matchesProductTitle || matchesProductLink
}

const fullBreadcrumbs = computed<ProductHeroBreadcrumb[]>(() => {
  const normalizedProductTitle = heroTitle.value.trim().toLowerCase()
  const normalizedProductSlug = (
    props.product.fullSlug ??
    props.product.slug ??
    ''
  )
    .trim()
    .toLowerCase()

  const baseCrumbs = props.breadcrumbs.reduce<ProductHeroBreadcrumb[]>(
    (acc, breadcrumb) => {
      const rawTitle = breadcrumb?.title ?? breadcrumb?.link ?? ''
      const trimmedTitle = rawTitle.toString().trim()
      const titleValue = trimmedTitle.length
        ? trimmedTitle
        : t('product.hero.missingBreadcrumbTitle')

      if (!titleValue.trim().length) {
        return acc
      }

      const isProduct = itemIsProduct(
        breadcrumb,
        normalizedProductTitle,
        normalizedProductSlug
      )

      if (isProduct) {
        return acc
      }

      const trimmedLink = breadcrumb?.link?.toString().trim() ?? ''
      const normalizedLink = trimmedLink
        ? trimmedLink.startsWith('http')
          ? trimmedLink
          : trimmedLink.startsWith('/')
            ? trimmedLink
            : `/${trimmedLink}`
        : undefined

      acc.push({
        title: titleValue,
        link: normalizedLink,
      })

      return acc
    },
    []
  )

  const modelTitle = productModelName.value.trim()
  const brandModelTitle = brandModelLine.value.trim()
  const heroFallback = heroTitle.value.trim()
  const finalTitle = modelTitle.length
    ? modelTitle
    : brandModelTitle.length
      ? brandModelTitle
      : heroFallback

  if (finalTitle.length) {
    const normalizedFinal = finalTitle.toLowerCase()
    const hasDuplicate = baseCrumbs.some(
      crumb => crumb.title.trim().toLowerCase() === normalizedFinal
    )

    if (!hasDuplicate) {
      baseCrumbs.push({
        title: finalTitle,
      })
    }
  }

  return baseCrumbs
})

const visibleBreadcrumbs = computed<ProductHeroBreadcrumb[]>(() => {
  const crumbs = [...fullBreadcrumbs.value]
  // Remove the last item if it's the product (which fullBreadcrumbs ensures is at the end)
  // We identify it by checking if it matches the generated finalTitle or simply by removing the last element
  // since fullBreadcrumbs logic guarantees the product is appended if missing, or preserved if present.
  // The user wants to remove the "terminal model name".

  // If the list is empty or has only 1 item (the product itself), we might end up with empty list.
  if (crumbs.length > 0) {
    // Check if the last item is indeed the product.
    // In fullBreadcrumbs we push the product if distinct.
    // If props.breadcrumbs contained the product, it was excluded from loop but added back?
    // Wait, the loop excludes it (L766-774).
    // Then L811 adds it back.
    // So yes, the last item is ALWAYS the product (unless props.breadcrumbs was empty AND product has no name? Unlikely).
    crumbs.pop()
  }

  // Limit to maximum 3 elements (the 3 last ones)
  return crumbs.slice(-3)
})

const heroBreadcrumbProps = computed(() => ({
  items: visibleBreadcrumbs.value,
  ariaLabel: t('product.hero.breadcrumbAriaLabel'),
}))
</script>

<style scoped>
.product-hero {
  position: relative;
  overflow: hidden;
  padding: clamp(1.5rem, 4vw, 3rem);
}

.product-hero__background {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.product-hero__background-media {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-hero__background-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(
      90deg,
      rgb(var(--v-theme-surface-default)) 0%,
      rgb(var(--v-theme-surface-default)) 45%,
      rgba(var(--v-theme-surface-default), 0) 65%
    ),
    radial-gradient(
      circle at 18% 22%,
      rgba(var(--v-theme-hero-gradient-start), 0.18),
      transparent 42%
    ),
    radial-gradient(
      circle at 82% 18%,
      rgba(var(--v-theme-hero-gradient-end), 0.2),
      transparent 40%
    ),
    linear-gradient(
      180deg,
      rgba(var(--v-theme-surface-default), 0.08) 0%,
      rgba(var(--v-theme-surface-default), 0.2) 40%,
      rgba(var(--v-theme-surface-default), 0.65) 100%
    );
}

.product-hero__content {
  position: relative;
  z-index: 1;
  overflow-x: hidden;
  max-width: 100%;
}

.product-hero__layout {
  --product-hero-gap: clamp(1.25rem, 2.4vw, 2.5rem);

  margin: 0;
  width: 100%;
}

.product-hero__media-column,
.product-hero__decision-column {
  display: flex;
  min-width: 0;
}

.product-hero__media-column {
  align-items: flex-start;
}

.product-hero__decision-column {
  min-height: 100%;
}

.product-hero__decision-shell {
  display: flex;
  flex-direction: column;
  gap: clamp(1rem, 2vw, 1.5rem);
  width: 100%;
  min-width: 0;
}

.product-hero__heading {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: clamp(0.65rem, 1.4vw, 1rem);
  text-align: left;
}

.product-hero__eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.35rem 0.85rem;
  border-radius: 999px;
  font-size: 0.85rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  background: rgba(var(--v-theme-hero-overlay-soft), 0.6);
  color: rgb(var(--v-theme-text-neutral-secondary));
  box-shadow: 0 10px 32px rgba(15, 23, 42, 0.08);
  width: fit-content;
}

.product-hero__heading :deep(.product-hero__title) {
  font-size: 2rem;
  font-weight: 800;
  line-height: 1.08;
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-strong));
  text-shadow: 0 18px 40px rgba(15, 23, 42, 0.16);
}

.product-hero__heading :deep(.product-hero__short-description) {
  margin: 0.65rem 0 0;
  max-width: 760px;
  font-size: 1.05rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
  text-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
}

.product-hero__impact-score {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.5rem;
  width: 100%;
}

.product-hero__breadcrumbs {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.35rem 0.85rem;
  margin: 0;
  border-radius: 999px;
  background: rgba(var(--v-theme-surface-default), 0.6);
  backdrop-filter: blur(12px);
  color: rgb(var(--v-theme-text-neutral-secondary));
  box-shadow: 0 8px 26px rgba(15, 23, 42, 0.08);
  justify-content: flex-start;
  text-align: left;
  width: fit-content;
  max-width: 100%;
}

.product-hero__baseline {
  margin: 0.6rem auto 0;
  max-width: 680px;
  font-size: 1rem;
  font-weight: 500;
  color: rgba(var(--v-theme-text-neutral-strong), 0.9);
}

.product-hero__decision-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(18rem, 0.72fr);
  gap: clamp(1rem, 2vw, 1.5rem);
  align-items: start;
}

.product-hero__decision-grid--single {
  grid-template-columns: minmax(0, 1fr);
}

.product-hero__panel {
  position: relative;
  height: 100%;
  padding: 0;
  border-radius: 0;
  background: transparent;
  border: none;
  box-shadow: none;
  backdrop-filter: none;
}

.product-hero__gallery-section {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;
  animation: productHeroMediaIn 0.46s ease-out both;
}

.product-hero__panel--pricing {
  padding: 0;
  background: transparent;
  border: none;
  box-shadow: none;
  animation: productHeroPanelIn 0.42s ease-out both;
  animation-delay: 0.12s;
}

.product-hero__gallery {
  min-width: 0;
}

.product-hero__details-section {
  display: flex;
  flex-direction: column;
  gap: clamp(0.9rem, 1.7vw, 1.2rem);
  min-width: 0;
}

.product-hero__centered-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.8rem;
  min-width: 0;
}

.product-hero__more-characteristics {
  align-self: flex-start;
  text-transform: none;
  letter-spacing: normal;
}

.product-hero__attributes {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  list-style: none;
  padding: 0;
  margin: 0;
}

.product-hero__actions {
  display: flex;
  justify-content: flex-start;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.product-hero__impact-card {
  max-width: 100%;
  transition:
    transform 0.22s ease,
    filter 0.22s ease;
}

.product-hero__impact-card:hover,
.product-hero__impact-card:focus-within {
  filter: saturate(1.05);
  transform: translateY(-2px);
}

.product-hero__verdict {
  margin: 0;
  animation: productHeroPanelIn 0.42s ease-out both;
  animation-delay: 0.06s;
}

@media (max-width: 768px) {
  .product-hero__actions {
    flex-direction: column;
    width: 100%;
  }

  .product-hero__compare-button,
  .product-hero__ai-button {
    width: 100%;
    justify-content: center;
  }

  .product-hero__decision-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

.product-hero-rise-enter-active,
.product-hero-rise-leave-active {
  transition:
    opacity 0.34s ease,
    transform 0.34s ease;
}

.product-hero-rise-enter-from,
.product-hero-rise-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

@keyframes productHeroMediaIn {
  from {
    opacity: 0;
    transform: translateX(-14px);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes productHeroPanelIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .product-hero__gallery-section,
  .product-hero__panel--pricing,
  .product-hero__verdict {
    animation: none;
  }

  .product-hero-rise-enter-active,
  .product-hero-rise-leave-active,
  .product-hero__compare-button,
  .product-hero__ai-button,
  .product-hero__impact-card {
    transition: none;
  }
}

.product-hero__compare-button {
  background: rgba(var(--v-theme-surface-glass-strong), 0.5);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(var(--v-theme-accent-primary-highlight), 0.2);
  color: rgb(var(--v-theme-accent-primary-highlight));
  padding: 0 1.25rem;
  height: 48px;
  border-radius: 14px;
  text-transform: none;
  letter-spacing: 0.01em;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
  transition: all 0.25s ease;
}

.product-hero__compare-button:hover {
  background: rgba(var(--v-theme-accent-primary-highlight), 0.1);
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(var(--v-theme-shadow-primary-600), 0.12);
}

.product-hero__compare-button:active {
  transform: translateY(0);
}

.product-hero__ai-button {
  background: rgba(var(--v-theme-surface-glass-strong), 0.5);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(var(--v-theme-accent-primary-highlight), 0.2);
  color: rgb(var(--v-theme-accent-primary-highlight));
  padding: 0 1.25rem;
  height: 48px;
  border-radius: 14px;
  text-transform: none;
  letter-spacing: 0.01em;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
  transition: all 0.25s ease;
}

.product-hero__ai-button:hover {
  background: rgba(var(--v-theme-accent-primary-highlight), 0.1);
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(var(--v-theme-shadow-primary-600), 0.12);
}

.product-hero__ai-button:active {
  transform: translateY(0);
}

.product-hero__attribute {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.97rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-hero__attribute-label {
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__attribute-separator {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.8);
}

.product-hero__attribute-value {
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-hero__attribute-value-content {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
}

.product-hero__flag {
  border-radius: 6px;
  width: 32px;
  height: 24px;
  object-fit: cover;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.18);
}

.product-hero__compare-button--active {
  background: linear-gradient(
    120deg,
    rgba(var(--v-theme-primary), 0.16),
    rgba(var(--v-theme-primary), 0.2)
  );
  color: rgb(var(--v-theme-primary));
  box-shadow: 0 16px 32px rgba(var(--v-theme-primary), 0.2);
}

.product-hero__compare-icon {
  margin-inline-end: 0.25rem;
}

.product-hero__compare-label {
  font-size: 0.98rem;
}

.product-hero--classic {
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-surface-ice-050), 0.9),
    rgba(var(--v-theme-surface-glass), 0.95)
  );
}

.product-hero--classic .product-hero__background {
  display: none;
}

.product-hero--classic .product-hero__panel {
  background: transparent;
  box-shadow: none;
}

@media (max-width: 960px) {
  .product-hero {
    padding: clamp(1.25rem, 4vw, 1.75rem);
  }

  .product-hero__heading :deep(.product-hero__title) {
    font-size: 1.9rem;
  }

  .product-hero__decision-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  /* Bold mobile fold reorder (owner-approved): breadcrumb/h1 -> price CTA ->
   * compact gallery -> verdict, ahead of the details/attributes section.
   * `display: contents` unwraps the intermediate v-col/grid wrappers so
   * their children become orderable flex items of `.product-hero__layout`
   * directly, without touching desktop markup or duplicating components. */
  .product-hero__layout {
    display: flex;
    flex-direction: column;
  }

  .product-hero__decision-column,
  .product-hero__decision-shell,
  .product-hero__decision-grid {
    display: contents;
  }

  .product-hero__heading {
    order: 1;
  }

  .product-hero__panel--pricing {
    order: 2;
  }

  .product-hero__media-column {
    order: 3;
  }

  .product-hero__verdict {
    order: 4;
  }

  .product-hero__details-section {
    order: 5;
  }
}

@media (max-width: 600px) {
  .product-hero__heading :deep(.product-hero__title) {
    font-size: 1.7rem;
  }

  .product-hero__heading :deep(.product-hero__short-description) {
    font-size: 0.98rem;
  }
}

.product-hero__descriptions-panel {
  width: 100%;
}

.product-hero__tabs :deep(.v-btn) {
  text-transform: none;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.product-hero__tab-content {
  background: rgba(var(--v-theme-surface-glass-strong), 0.8);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  padding: 1.25rem;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}

.product-hero__description-body {
  font-size: 0.95rem;
  line-height: 1.6;
}

.product-hero__description-body :deep(h1),
.product-hero__description-body :deep(h2),
.product-hero__description-body :deep(h3) {
  font-size: 1.1rem;
  font-weight: 600;
  margin-top: 1rem;
  margin-bottom: 0.5rem;
}

.product-hero__description-body :deep(p) {
  margin-bottom: 0.75rem;
}

.product-hero__description-body :deep(ul) {
  padding-left: 1.25rem;
  margin-bottom: 0.75rem;
}

.product-hero__description-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 0.5rem 0;
}
</style>
