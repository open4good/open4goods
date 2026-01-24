<template>
  <section class="product-hero">
    <div class="product-hero__background" aria-hidden="true">
      <img
        class="product-hero__background-media"
        :src="heroBackground"
        alt=""
        decoding="async"
        loading="eager"
      />
      <div class="product-hero__background-overlay" />
    </div>
    <div class="product-hero__content">
      <header class="product-hero__heading">
        <ProductDesignation
          :product="product"
          variant="page"
          title-tag="h3"
          title-class="product-hero__title text-center"
          description-class="product-hero__short-description text-center"
        />
        <CategoryNavigationBreadcrumbs
          v-if="heroBreadcrumbs.length"
          v-bind="heroBreadcrumbProps"
          class="product-hero__breadcrumbs text-center"
        />
        <v-fade-transition>
          <p v-if="aiBaseline" class="product-hero__baseline text-center">
            {{ aiBaseline }}
          </p>
        </v-fade-transition>
      </header>

      <div class="product-hero__grid">
        <div class="product-hero__panel product-hero__panel--main">
          <div class="product-hero__main-content">
            <div class="product-hero__gallery-section">
              <ProductHeroGallery
                class="product-hero__gallery"
                :product="product"
                :title="heroTitle"
              />
            </div>

            <div class="product-hero__details-section">
              <div v-if="hasBrandOrModel" class="product-hero__heading-group">
                <span v-if="productBrandName" class="product-hero__brand-name">
                  {{ productBrandName }}
                </span>
                -
                <span v-if="productModelName" class="product-hero__subtitle">
                  {{ productModelName }}
                </span>
              </div>

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
                  <template v-if="attribute.key === 'ai-summary'">
                    <ul
                      class="product-hero__ai-summary-list"
                      @click="handleAiSummaryClick"
                    >
                      <li
                        v-if="technicalOnelineHtml"
                        class="product-hero__ai-summary-item"
                      >
                        <v-icon
                          icon="mdi-cog-outline"
                          size="small"
                          class="product-hero__ai-summary-icon"
                        />
                        <div class="product-hero__ai-summary-content">
                          <span class="product-hero__ai-summary-label">
                            {{ t('product.hero.aiSummary.technical') }}
                          </span>
                          <!-- eslint-disable vue/no-v-html -->
                          <span
                            class="product-hero__ai-summary-text"
                            v-html="technicalOnelineHtml"
                          />
                          <!-- eslint-enable vue/no-v-html -->
                        </div>
                      </li>
                      <li
                        v-if="ecologicalOnelineHtml"
                        class="product-hero__ai-summary-item"
                      >
                        <v-icon
                          icon="mdi-leaf"
                          size="small"
                          class="product-hero__ai-summary-icon"
                        />
                        <div class="product-hero__ai-summary-content">
                          <span class="product-hero__ai-summary-label">
                            {{ t('product.hero.aiSummary.ecological') }}
                          </span>
                          <!-- eslint-disable vue/no-v-html -->
                          <span
                            class="product-hero__ai-summary-text"
                            v-html="ecologicalOnelineHtml"
                          />
                          <!-- eslint-enable vue/no-v-html -->
                        </div>
                      </li>
                      <li
                        v-if="communityOnelineHtml"
                        class="product-hero__ai-summary-item"
                      >
                        <v-icon
                          icon="mdi-account-group-outline"
                          size="small"
                          class="product-hero__ai-summary-icon"
                        />
                        <div class="product-hero__ai-summary-content">
                          <span class="product-hero__ai-summary-label">
                            {{ t('product.hero.aiSummary.community') }}
                          </span>
                          <!-- eslint-disable vue/no-v-html -->
                          <span
                            class="product-hero__ai-summary-text"
                            v-html="communityOnelineHtml"
                          />
                          <!-- eslint-enable vue/no-v-html -->
                        </div>
                      </li>
                    </ul>
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
                              <!-- eslint-disable-next-line vue/no-v-html -->
                              <span v-if="displayHtml" v-html="displayHtml" />
                              <span v-else>{{ displayValue }}</span>
                            </span>
                          </template>
                        </v-tooltip>
                      </template>
                    </ProductAttributeSourcingLabel>
                  </span>
                </li>
              </ul>

              <div class="product-hero__actions">
                <v-btn
                  class="product-hero__ai-button"
                  :prepend-icon="
                    hasAiReview ? 'mdi-check-circle-outline' : 'mdi-robot'
                  "
                  variant="flat"
                  @click="handleAiReviewClick"
                >
                  {{ t('product.hero.aiReview.label', 'Synthèse IA') }}
                </v-btn>

                <v-btn
                  class="product-hero__compare-button"
                  :class="{
                    'product-hero__compare-button--active': isCompareSelected,
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
            </div>
          </div>
        </div>

        <aside class="product-hero__panel product-hero__panel--pricing">
          <ProductHeroPricing :product="product" />
        </aside>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, type PropType } from 'vue'
import DOMPurify from 'isomorphic-dompurify'
import { useI18n } from 'vue-i18n'
import CategoryNavigationBreadcrumbs from '~/components/category/navigation/CategoryNavigationBreadcrumbs.vue'
import ProductHeroPricing from '~/components/product/ProductHeroPricing.vue'
import ProductDesignation from '~/components/product/ProductDesignation.vue'
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
  image: {
    type: String,
    default: null,
  },
})

const { t, te, n, locale } = useI18n()
const heroBackground = useThemedAsset('product/product-hero-background.svg')

// AI Review Logic

const aiReview = computed(() => props.product.aiReview?.review ?? null)
const hasAiReview = computed(() => Boolean(aiReview.value))

const sanitizeAiReviewHtml = (content: string | null | undefined): string => {
  if (!content) {
    return ''
  }

  return DOMPurify.sanitize(content, {
    ADD_ATTR: ['class', 'target', 'rel'],
  })
}

const technicalOnelineHtml = computed(() =>
  sanitizeAiReviewHtml(aiReview.value?.technicalOneline ?? null)
)
const ecologicalOnelineHtml = computed(() =>
  sanitizeAiReviewHtml(aiReview.value?.ecologicalOneline ?? null)
)
const communityOnelineHtml = computed(() =>
  sanitizeAiReviewHtml(aiReview.value?.communityOneline ?? null)
)
const aiBaseline = computed(() => {
  const baseline = (aiReview.value as { baseLine?: string | null } | null)
    ?.baseLine
  return typeof baseline === 'string' ? baseline.trim() : ''
})

const handleAiReviewClick = () => {
  const element =
    document.getElementById('synthese') ||
    document.querySelector('.product-ai-review')
  if (element) {
    const offset = 120 // Adjust based on header height
    const top = element.getBoundingClientRect().top + window.scrollY - offset
    window.scrollTo({ top, behavior: 'smooth' })
  }
}

const handleAiSummaryClick = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  const link = target.closest('a.review-ref') as HTMLAnchorElement | null

  if (!link || !link.hash) {
    return
  }

  event.preventDefault()

  // Dispatch event for ProductAiReviewSection to handle
  window.dispatchEvent(
    new CustomEvent('ai-review:scroll-to-source', {
      detail: { id: link.hash },
    })
  )
}

const normalizeString = (value: string | null | undefined) =>
  typeof value === 'string' ? value.trim() : ''

const heroTitle = computed(() => {
  return resolveProductLongName(props.product, locale.value)
})

const productBrandName = computed(() =>
  normalizeString(props.product.identity?.brand)
)
const productModelName = computed(() =>
  normalizeString(props.product.identity?.model)
)
const hasBrandOrModel = computed(
  () => productBrandName.value.length > 0 || productModelName.value.length > 0
)

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

  if (hasAiReview.value && aiReview.value) {
    baseAttributes.push({
      key: 'ai-summary',
      label: '', // Not used
      value: '', // Not used
      showLabel: false,
      enableTooltip: false,
    })
  } else {
    baseAttributes.push(...popularAttributes.value)
  }

  if (gtinCountry.value) {
    if (
      !baseAttributes.some(
        attribute => attribute.key === 'base.gtinInfo.countryName'
      )
    ) {
      baseAttributes.push({
        key: 'gtin-country',
        label: '',
        value: gtinCountry.value.name,
        flag: gtinCountry.value.flag,
        tooltip: t('product.hero.gtinTooltip'),
        showLabel: false,
      })
    }
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
    ? t('product.hero.compare.selected')
    : t('product.hero.compare.label')
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
  isCompareSelected.value
    ? 'mdi-check-circle-outline'
    : 'mdi-compare-horizontal'
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

const heroBreadcrumbs = computed<ProductHeroBreadcrumb[]>(() => {
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

      const normalizedTitle = titleValue.trim().toLowerCase()
      const normalizedLinkValue =
        breadcrumb?.link?.toString().trim().toLowerCase() ?? ''

      const matchesProductTitle = normalizedProductTitle.length
        ? normalizedTitle === normalizedProductTitle
        : false
      const matchesProductLink = normalizedProductSlug.length
        ? normalizedLinkValue.endsWith(normalizedProductSlug)
        : false

      if (matchesProductTitle || matchesProductLink) {
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

const heroBreadcrumbProps = computed(() => ({
  items: heroBreadcrumbs.value,
  ariaLabel: t('product.hero.breadcrumbAriaLabel'),
}))

const gtinCountry = computed(() => {
  const info = props.product.base?.gtinInfo
  if (!info?.countryName) {
    return null
  }

  return {
    name: info.countryName,
    flag: info.countryFlagUrl ?? null,
  }
})
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
  display: flex;
  flex-direction: column;
  gap: clamp(1.5rem, 3vw, 2.5rem);
  z-index: 1;
}

.product-hero__heading {
  display: grid;
  gap: 0.6rem;
  text-align: left;
  align-items: start;
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

.product-hero__title {
  font-size: clamp(1.5rem, 2vw, 2.2rem);
  font-weight: 800;
  line-height: 1.05;
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-strong));
  text-shadow: 0 18px 40px rgba(15, 23, 42, 0.16);
}

.product-hero__short-description {
  margin: 0.65rem auto 0;
  max-width: 680px;
  font-size: 1.05rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
  text-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
}

.product-hero__breadcrumbs {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.35rem 0.85rem;
  margin: 0.35rem 0 0;
  border-radius: 999px;
  background: rgba(var(--v-theme-surface-default), 0.6);
  backdrop-filter: blur(12px);
  color: rgb(var(--v-theme-text-neutral-secondary));
  box-shadow: 0 8px 26px rgba(15, 23, 42, 0.08);
  justify-content: center;
  text-align: center;
  width: fit-content;
  max-width: 100%;
  margin-inline: auto;
}

.product-hero__baseline {
  margin: 0.6rem auto 0;
  max-width: 680px;
  font-size: 1rem;
  font-weight: 500;
  color: rgba(var(--v-theme-text-neutral-strong), 0.9);
  animation: baselineFadeIn 0.6s ease 0.2s both;
}

@keyframes baselineFadeIn {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.product-hero__grid {
  display: grid;
  gap: clamp(1.5rem, 3vw, 2.75rem);
  grid-template-columns: 2fr 1fr;
  align-items: stretch;
}

@media (max-width: 960px) {
  .product-hero__grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 1.5rem;
  }
}

.product-hero__panel {
  position: relative;
  height: 100%;
  padding: clamp(1.25rem, 2.2vw, 1.75rem);
  border-radius: 0;
  background: transparent;
  border: none;
  box-shadow: none;
  backdrop-filter: none;
}

.product-hero__panel--main {
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.product-hero__main-content {
  display: grid;
  grid-template-columns: 1fr 1fr; /* Split Main panel 50/50 */
  gap: clamp(1.5rem, 3vw, 2.75rem);
  height: 100%;
}

@media (max-width: 768px) {
  .product-hero__main-content {
    grid-template-columns: 1fr;
  }
}

.product-hero__gallery-section {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.product-hero__panel--pricing {
  padding: clamp(1rem, 1.8vw, 1.5rem);
  background: transparent;
  border: none;
  box-shadow: none;
}

.product-hero__gallery {
  min-width: 0;
}

.product-hero__details-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 1rem 0;
  height: 100%;
}

.product-hero__heading-group {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 0.35rem 0.6rem;
}

.product-hero__brand-name {
  font-size: 1rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-hero__subtitle {
  font-size: 1rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
  line-height: 1.4;
}

.product-hero__attributes {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  list-style: none;
  padding: 0;
  margin: 0;
}

.product-hero__attribute--country {
  margin-top: 1rem;
  padding-top: 0.5rem;
}

.product-hero__actions {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
}

.product-hero__ai-summary-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-hero__ai-summary-item {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-size: 0.95rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__ai-summary-icon {
  color: rgb(var(--v-theme-primary));
}

.product-hero__ai-summary-content {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.product-hero__ai-summary-label {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.product-hero__ai-summary-text {
  font-size: 0.95rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__ai-summary-text :deep(.review-ref) {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  padding: 0.1rem 0.35rem;
  border-radius: 6px;
  background: rgba(var(--v-theme-surface-primary-080), 0.9);
  color: rgb(var(--v-theme-accent-primary-highlight));
  font-size: 1.05em;
  font-weight: 600;
  text-decoration: none;
}

.product-hero__ai-summary-text :deep(.review-ref:hover),
.product-hero__ai-summary-text :deep(.review-ref:focus-visible) {
  background: rgba(var(--v-theme-surface-primary-100), 0.95);
  box-shadow: 0 4px 10px rgba(var(--v-theme-shadow-primary-600), 0.18);
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
  margin-right: 0.75rem;
}

.product-hero__ai-button:hover {
  background: rgba(var(--v-theme-accent-primary-highlight), 0.1);
}

.product-hero__brand-line {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__model-name {
  font-size: 1.2rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__attribute {
  display: flex;
  align-items: baseline;
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

.product-hero__meta-group {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex: 1 1 auto;
  min-height: 0;
  margin-block-start: clamp(0.75rem, 2vh, 1.25rem);
}

.product-hero__meta-bottom {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 0.75rem;
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

@media (max-width: 1400px) {
  .product-hero__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .product-hero__panel--pricing {
    grid-column: span 2;
  }
}

@media (max-width: 960px) {
  .product-hero {
    padding: clamp(1.25rem, 4vw, 1.75rem);
  }

  .product-hero__grid {
    grid-template-columns: 1fr;
  }

  .product-hero__panel--pricing {
    grid-column: auto;
  }
}
</style>
