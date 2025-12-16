<template>
  <section class="product-hero" itemscope itemtype="https://schema.org/Product">
    <meta itemprop="sku" :content="String(product.base?.gtin ?? '')" />
    <meta itemprop="brand" :content="product.identity?.brand ?? ''" />

    <header v-if="heroTitle" class="product-hero__heading">
      <h1 class="product-hero__title" itemprop="name">
        {{ heroTitle }}
      </h1>
      <CategoryNavigationBreadcrumbs
        v-if="heroBreadcrumbs.length"
        v-bind="heroBreadcrumbProps"
        class="product-hero__breadcrumbs"
      />
    </header>

    <ProductHeroGallery
      class="product-hero__gallery"
      :product="product"
      :title="heroTitle"
    />

    <div class="product-hero__details">
      <div v-if="impactScore !== null" class="product-hero__impact-overview">
        <ImpactScore
          :score="impactScore"
          :max="5"
          size="xlarge"
          :show-value="true"
        />
      </div>

      <div v-if="hasBrandOrModel" class="product-hero__brand-line">
        <span v-if="productBrandName" class="product-hero__brand-name">{{
          productBrandName
        }}</span>
        <span v-if="productModelName" class="product-hero__model-name">{{
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
          role="listitem"
        >
          <template v-if="attribute.showLabel !== false">
            <span class="product-hero__attribute-label">{{
              attribute.label
            }}</span>
            <span class="product-hero__attribute-separator" aria-hidden="true"
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

      <div class="product-hero__meta-group">
        <div class="product-hero__meta-bottom">
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

    <aside class="product-hero__pricing">
      <ProductHeroPricing :product="product" />
    </aside>
  </section>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import CategoryNavigationBreadcrumbs from '~/components/category/navigation/CategoryNavigationBreadcrumbs.vue'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import ProductHeroGallery from '~/components/product/ProductHeroGallery.vue'
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
import type {
  AttributeConfigDto,
  ProductAttributeSourceDto,
  ProductDto,
} from '~~/shared/api-client'

export interface ProductHeroBreadcrumb {
  title: string
  link?: string
}

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

const { t, te, n } = useI18n()

const normalizeString = (value: string | null | undefined) =>
  typeof value === 'string' ? value.trim() : ''

const fallbackTitle = computed(() => {
  const medium = normalizeString(props.product.names?.h1Title)
  const identity = normalizeString(props.product.identity?.bestName)
  const base = normalizeString(props.product.base?.bestName)

  return medium || identity || base || ''
})

const bestName = computed(() => {
  const identity = normalizeString(props.product.identity?.bestName)
  if (identity) {
    return identity
  }

  const base = normalizeString(props.product.base?.bestName)
  if (base) {
    return base
  }

  return fallbackTitle.value
})

const heroTitle = computed(() => {
  const aiTitle = normalizeString(props.product.aiReview?.review?.mediumTitle)
  if (aiTitle) {
    return aiTitle
  }

  return fallbackTitle.value || bestName.value
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

const impactScore = computed(() => resolvePrimaryImpactScore(props.product))
</script>

<style scoped>
.product-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr) minmax(0, 0.8fr);
  gap: 2.5rem;
  padding: 2.5rem;
  border-radius: 32px;
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-surface-ice-050), 0.9),
    rgba(var(--v-theme-surface-glass), 0.95)
  );
  box-shadow: 0 32px 70px rgba(15, 23, 42, 0.08);
}

.product-hero__breadcrumbs {
  display: flex;
  justify-content: center;
  margin: 0.75rem auto 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-hero__heading {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-bottom: 1.5rem;
}

.product-hero__title {
  font-size: clamp(2rem, 2.8vw, 3rem);
  font-weight: 700;
  line-height: 1.1;
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__gallery {
  min-width: 0;
}

.product-hero__details {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-width: 0;
}

.product-hero__brand-line {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__brand-name {
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-hero__model-name {
  font-size: 1.2rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__attributes {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding: 0;
  margin: 0 0 1.25rem;
  list-style: none;
}

.product-hero__attribute {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
  font-size: 0.95rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
}

.product-hero__attribute-label {
  font-weight: 500;
  color: rgba(var(--v-theme-text-neutral-strong), 0.9);
}

.product-hero__attribute-separator {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.6);
}

.product-hero__attribute-value {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
}

.product-hero__attribute-value-content {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.product-hero__flag {
  border-radius: 4px;
  width: 32px;
  height: 24px;
  object-fit: cover;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.15);
}

.product-hero__impact-overview {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  color: rgb(var(--v-theme-text-neutral-strong));
  margin-block-end: clamp(1rem, 2vh, 1.5rem);
}

.product-hero__meta-group {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  flex: 1 1 auto;
  min-height: 0;
  margin-block-start: clamp(1.25rem, 2.5vh, 2rem);
}

.product-hero__meta-bottom {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 0.75rem;
}

@media (max-width: 960px) {
  .product-hero__impact-overview {
    justify-content: flex-start;
  }
}

.product-hero__compare-button {
  text-transform: none;
  font-weight: 600;
  letter-spacing: 0;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding-inline: 1.2rem;
  background-color: rgba(var(--v-theme-surface-default), 0.92);
  color: rgb(var(--v-theme-text-neutral-strong));
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.14);
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease;
}

.product-hero__compare-button:hover {
  background-color: rgba(var(--v-theme-surface-default), 1);
}

.product-hero__compare-button:focus-visible {
  box-shadow: 0 0 0 3px rgba(var(--v-theme-accent-primary-highlight), 0.45);
}

.product-hero__compare-button--active {
  background-color: rgba(var(--v-theme-primary), 0.16);
  color: rgb(var(--v-theme-primary));
  box-shadow: 0 14px 32px rgba(var(--v-theme-primary), 0.18);
}

.product-hero__compare-icon {
  margin-inline-end: 0.25rem;
}

.product-hero__compare-label {
  font-size: 0.95rem;
}

.product-hero__pricing {
  align-self: stretch;
}

@media (max-width: 1280px) {
  .product-hero {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: auto auto;
  }

  .product-hero__pricing {
    grid-column: 1 / -1;
  }
}

@media (max-width: 960px) {
  .product-hero {
    grid-template-columns: 1fr;
    padding: 1.5rem;
    gap: 1.5rem;
  }
}
</style>
