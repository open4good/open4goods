<template>
  <section class="product-hero">
    <div class="product-hero__content">
      <header v-if="heroTitle" class="product-hero__heading">
        <h1 class="product-hero__title text-center">{{ heroTitle }}</h1>
        <CategoryNavigationBreadcrumbs
          v-if="heroBreadcrumbs.length"
          v-bind="heroBreadcrumbProps"
          class="product-hero__breadcrumbs text-center"
        />
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
              <div class="product-hero__corner" role="presentation">
                <ImpactScore
                  v-if="impactScoreOn20 !== null"
                  :score="impactScoreOn20"
                  :max="5"
                  size="xxlarge"
                  mode="badge"
                  badge-layout="stacked"
                  badge-variant="corner"
                />
                <span v-else class="product-hero__corner-fallback">
                  {{ t('category.products.notRated') }}
                </span>
              </div>
            </div>

            <div class="product-hero__details-section">
              <div v-if="hasBrandOrModel" class="product-hero__heading-group">
                <span
                  v-if="productBrandName"
                  class="product-hero__brand-name"
                  >{{ productBrandName }}</span
                >
                <span v-if="productModelName" class="product-hero__subtitle">{{
                  productModelName
                }}</span>
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
                  <template v-if="attribute.showLabel !== false">
                    <span class="product-hero__attribute-label">{{
                      attribute.label
                    }}</span>
                    <span
                      class="product-hero__attribute-separator"
                      aria-hidden="true"
                      >:</span
                    >
                  </template>
                  <span class="product-hero__attribute-value">
                    <template v-if="attribute.tooltip">
                      <v-tooltip location="bottom" :text="attribute.tooltip">
                        <template #activator="{ props: tooltipProps }">
                          <ProductAttributeSourcingLabel
                            class="product-hero__attribute-value-label"
                            :sourcing="attribute.sourcing"
                            :value="attribute.value"
                            :enable-tooltip="attribute.enableTooltip !== false"
                          >
                            <template #default="{ displayValue, displayHtml }">
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
                                <!-- eslint-disable-next-line vue/no-v-html -->
                                <span v-if="displayHtml" v-html="displayHtml" />
                                <span v-else>{{ displayValue }}</span>
                              </span>
                            </template>
                          </ProductAttributeSourcingLabel>
                        </template>
                      </v-tooltip>
                    </template>
                    <template v-else>
                      <ProductAttributeSourcingLabel
                        class="product-hero__attribute-value-label"
                        :sourcing="attribute.sourcing"
                        :value="attribute.value"
                        :enable-tooltip="attribute.enableTooltip !== false"
                      >
                        <template #default="{ displayValue, displayHtml }">
                          <span class="product-hero__attribute-value-content">
                            <NuxtImg
                              v-if="attribute.flag"
                              :src="attribute.flag"
                              :alt="displayValue"
                              width="32"
                              height="24"
                              class="product-hero__flag"
                            />
                            <!-- eslint-disable-next-line vue/no-v-html -->
                            <span v-if="displayHtml" v-html="displayHtml" />
                            <span v-else>{{ displayValue }}</span>
                          </span>
                        </template>
                      </ProductAttributeSourcingLabel>
                    </template>
                  </span>
                </li>
              </ul>

              <div class="product-hero__actions">
                <v-btn
                  class="product-hero__compare-button"
                  :class="{
                    'product-hero__compare-button--active': isCompareSelected,
                  }"
                  color="primary"
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
import { useI18n } from 'vue-i18n'
import CategoryNavigationBreadcrumbs from '~/components/category/navigation/CategoryNavigationBreadcrumbs.vue'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import ProductHeroPricing from '~/components/product/ProductHeroPricing.vue'
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
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'
import { humanizeSlug } from '~/utils/_product-title'
import { resolveProductTitle } from '~/utils/_product-title-resolver'

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
})

const { t, te, n, locale } = useI18n()

const normalizeString = (value: string | null | undefined) =>
  typeof value === 'string' ? value.trim() : ''

const heroTitle = computed(() => {
  return resolveProductTitle(props.product, locale.value)
})

const gtinLabel = computed(() => {
  const gtin = normalizeString(props.product.gtin?.toString())
  return gtin ? t('product.meta.gtinFallback', { gtin }) : ''
})

const fallbackTitle = computed(() => {
  return heroTitle.value || gtinLabel.value
})

const bestName = computed(() => heroTitle.value)

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
  const baseAttributes: HeroAttribute[] = [...popularAttributes.value]

  if (
    gtinCountry.value &&
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
    props.product.identity?.bestName ||
    props.product.base?.bestName

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

const impactScoreOn20 = computed(() => resolvePrimaryImpactScore(props.product))
</script>

<style scoped>
.product-hero {
  position: relative;
  overflow: hidden;
  padding: clamp(1.5rem, 4vw, 3rem);
  border-radius: 32px;
  background: white;
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.12);
  isolation: isolate;
}

.product-hero__background {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.product-hero__bg-gradient {
  position: absolute;
  inset: -20%;
  background: radial-gradient(
    circle at 50% 20%,
    rgba(var(--v-theme-hero-overlay-strong), 0.08),
    rgba(var(--v-theme-hero-gradient-mid), 0.06),
    transparent 70%
  );
  transform: translateZ(0);
}

.product-hero__bg-image {
  position: absolute;
  inset: -10%;
  background-size: cover;
  background-position: center;
  opacity: 0.35;
  filter: saturate(1.15) blur(2px);
  transform: translate3d(0, 0, 0) scale(1.05);
  background-attachment: fixed;
}

.product-hero__bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(
      rgba(var(--v-theme-border-primary-strong), 0.07) 1px,
      transparent 1px
    ),
    linear-gradient(
      90deg,
      rgba(var(--v-theme-border-primary-strong), 0.07) 1px,
      transparent 1px
    );
  background-size: 120px 120px;
  mask-image: radial-gradient(
    circle at 50% 30%,
    rgba(0, 0, 0, 0.6),
    transparent 70%
  );
}

.product-hero__bg-glow {
  position: absolute;
  width: 480px;
  height: 480px;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}

.product-hero__bg-glow--primary {
  top: -80px;
  left: -120px;
  background: rgba(var(--v-theme-hero-gradient-start), 0.35);
}

.product-hero__bg-glow--accent {
  bottom: -180px;
  right: -140px;
  background: rgba(var(--v-theme-hero-gradient-end), 0.38);
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
  font-size: clamp(2.1rem, 3vw, 3.25rem);
  font-weight: 800;
  line-height: 1.05;
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-strong));
  text-shadow: 0 18px 40px rgba(15, 23, 42, 0.16);
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
  border-radius: 24px;
  background: rgba(var(--v-theme-surface-glass-strong), 0.66);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(16px);
}

.product-hero__panel--main {
  background: rgba(var(--v-theme-surface-glass), 0.7);
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

.product-hero__details-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 1rem 0;
}

.product-hero__panel--pricing {
  padding: clamp(1rem, 1.8vw, 1.5rem);
  background:
    linear-gradient(
      135deg,
      rgba(var(--v-theme-hero-gradient-start), 0.12),
      rgba(var(--v-theme-hero-gradient-end), 0.1)
    ),
    rgba(var(--v-theme-surface-default), 0.86);
  border: 1px solid rgba(var(--v-theme-accent-primary-highlight), 0.35);
  box-shadow: 0 24px 52px rgba(var(--v-theme-shadow-primary-600), 0.18);
}

.product-hero__gallery {
  min-width: 0;
}

.product-hero__corner {
  position: absolute;
  top: -1.75rem; /* Adjust based on panel padding */
  left: -1.75rem; /* Adjust based on panel padding */
  width: 150px;
  height: 150px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 0 0 54% 0;
  background: rgba(var(--v-theme-surface-glass-strong), 0.92);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.45);
  color: rgb(var(--v-theme-text-neutral-strong));
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(6px);
  z-index: 2;
  pointer-events: none;
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
  flex-direction: column;
  gap: 0.25rem;
}

.product-hero__brand-name {
  font-size: 1rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-hero__subtitle {
  font-size: 1.5rem;
  font-weight: 800;
  color: rgb(var(--v-theme-text-neutral-strong));
  line-height: 1.2;
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
  padding-top: 1rem;
}

.product-hero__compare-button {
  background: rgba(var(--v-theme-surface-glass-strong), 0.5);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2);
  color: rgb(var(--v-theme-text-neutral-strong));
  padding: 0 1.25rem;
  height: 48px;
  border-radius: 14px;
  text-transform: none;
  letter-spacing: 0.01em;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
  transition: all 0.25s ease;
}

.product-hero__corner-fallback {
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  text-align: center;
  line-height: 1.1;
  transform: rotate(-12deg);
}

.product-hero__brand-line {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__brand-name {
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-hero__model-name {
  font-size: 1.2rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__attributes {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0;
  margin: 0;
  list-style: none;
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

.product-hero__compare-button {
  text-transform: none;
  font-weight: 700;
  letter-spacing: 0.01em;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  padding-inline: 1.4rem;
  background: linear-gradient(
    120deg,
    rgba(var(--v-theme-surface-default), 0.98),
    rgba(var(--v-theme-surface-glass-strong), 0.86)
  );
  color: rgb(var(--v-theme-text-neutral-strong));
  box-shadow:
    0 10px 28px rgba(15, 23, 42, 0.16),
    0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.35) inset;
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.product-hero__compare-button:hover {
  transform: translateY(-2px);
  background-color: rgba(var(--v-theme-surface-default), 1);
}

.product-hero__compare-button:focus-visible {
  box-shadow:
    0 0 0 3px rgba(var(--v-theme-accent-primary-highlight), 0.45),
    0 10px 28px rgba(15, 23, 42, 0.16);
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
  background: rgba(var(--v-theme-surface-default), 0.82);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
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

  .product-hero__bg-image {
    background-attachment: scroll;
  }
}
</style>
