<template>
  <section
    class="product-hero"
    itemscope
    itemtype="https://schema.org/Product"
  >
    <meta itemprop="sku" :content="String(product.base?.gtin ?? '')" />
    <meta itemprop="brand" :content="product.identity?.brand ?? ''" />

    <header v-if="heroTitle" class="product-hero__heading">
      <h1 class="product-hero__title" itemprop="name">
        {{ heroTitle }}
      </h1>
    </header>

    <ProductHeroGallery class="product-hero__gallery" :product="product" :title="heroTitle" />

    <div class="product-hero__details">
      <CategoryNavigationBreadcrumbs
        v-if="heroBreadcrumbs.length"
        v-bind="heroBreadcrumbProps"
        class="product-hero__breadcrumbs"
      />

      <p v-if="brandModelLine" class="product-hero__brand-line">
        {{ brandModelLine }}
      </p>

      <ul v-if="popularAttributes.length" class="product-hero__attributes" role="list">
        <li
          v-for="attribute in popularAttributes"
          :key="attribute.key"
          class="product-hero__attribute"
          role="listitem"
        >
          <span class="product-hero__attribute-label">{{ attribute.label }}</span>
          <span class="product-hero__attribute-separator" aria-hidden="true">:</span>
          <span class="product-hero__attribute-value">{{ attribute.value }}</span>
        </li>
      </ul>

      <div class="product-hero__meta-group">
        <div class="product-hero__meta-top">
          <v-tooltip v-if="gtinCountry" location="bottom" :text="t('product.hero.gtinTooltip')">
            <template #activator="{ props: tooltipProps }">
              <span v-bind="tooltipProps" class="product-hero__origin">
                <NuxtImg
                  v-if="gtinCountry.flag"
                  :src="gtinCountry.flag"
                  :alt="gtinCountry.name"
                  width="32"
                  height="24"
                  class="product-hero__flag"
                />
                <span>{{ gtinCountry.name }}</span>
              </span>
            </template>
          </v-tooltip>
        </div>

        <div v-if="impactScore !== null" class="product-hero__meta-middle">
          <div class="product-hero__impact-card">
            <p class="product-hero__impact-title">{{ impactBadgeTitle }}</p>

            <div class="product-hero__impact-score">
              <ImpactScore :score="impactScore" :max="5" size="large" :show-value="true" />
            </div>

            <NuxtLink
              v-if="impactScoreLearnMoreLink"
              :to="impactScoreLearnMoreLink"
              class="product-hero__impact-link"
            >
              {{ impactBadgeLinkLabel }}
            </NuxtLink>
          </div>
        </div>

        <div class="product-hero__meta-bottom">
          <v-btn
            class="product-hero__compare-button"
            :class="{ 'product-hero__compare-button--active': isCompareSelected }"
            color="primary"
            variant="flat"
            :aria-pressed="isCompareSelected"
            :aria-label="compareButtonAriaLabel"
            :title="compareButtonTitle"
            :disabled="isCompareDisabled"
            @click="toggleCompare"
          >
            <v-icon :icon="compareButtonIcon" size="20" class="product-hero__compare-icon" />
            <span class="product-hero__compare-label">{{ compareButtonText }}</span>
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
import { formatAttributeValue, resolvePopularAttributes } from '~/utils/_product-attributes'
import type { AttributeConfigDto, ProductDto } from '~~/shared/api-client'

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

const brandModelLine = computed(() => {
  const brand = props.product.identity?.brand?.trim()
  const model = props.product.identity?.model?.trim()

  return [brand, model].filter((value) => Boolean(value && value.length)).join(' - ')
})

type DisplayedAttribute = { key: string; label: string; value: string }

const popularAttributeConfigs = computed(() => props.popularAttributes ?? [])

const popularAttributes = computed<DisplayedAttribute[]>(() =>
  resolvePopularAttributes(props.product, popularAttributeConfigs.value)
    .map((attribute) => {
      const value = formatAttributeValue(attribute, t, n)
      if (!value) {
        return null
      }

      return {
        key: attribute.key,
        label: attribute.label,
        value,
      }
    })
    .filter((attribute): attribute is DisplayedAttribute => attribute != null),
)

const compareStore = useProductCompareStore()

const reasonMessage = (reason: CompareListBlockReason | undefined) => {
  switch (reason) {
    case 'limit-reached':
      return t('category.products.compare.limitReached', { count: MAX_COMPARE_ITEMS })
    case 'vertical-mismatch':
      return t('category.products.compare.differentCategory')
    case 'missing-identifier':
      return t('category.products.compare.missingIdentifier')
    default:
      return t('product.hero.compare.add')
  }
}

const compareEligibility = computed(() => compareStore.canAddProduct(props.product))
const isCompareSelected = computed(() => compareStore.hasProduct(props.product))

const compareButtonText = computed(() =>
  isCompareSelected.value ? t('product.hero.compare.selected') : t('product.hero.compare.label'),
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
    heroTitle.value || props.product.identity?.bestName || props.product.base?.bestName

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
  isCompareSelected.value ? 'mdi-check-circle-outline' : 'mdi-compare-horizontal',
)

const isCompareDisabled = computed(
  () => !isCompareSelected.value && !compareEligibility.value.success,
)

const toggleCompare = () => {
  if (isCompareDisabled.value) {
    return
  }

  compareStore.toggleProduct(props.product)
}

const heroBreadcrumbs = computed<ProductHeroBreadcrumb[]>(() => {
  const normalizedProductTitle = heroTitle.value.trim().toLowerCase()
  const normalizedProductSlug = (props.product.fullSlug ?? props.product.slug ?? '').trim().toLowerCase()

  return props.breadcrumbs.reduce<ProductHeroBreadcrumb[]>((acc, breadcrumb) => {
    const rawTitle = breadcrumb?.title ?? breadcrumb?.link ?? ''
    const trimmedTitle = rawTitle.toString().trim()
    const titleValue = trimmedTitle.length ? trimmedTitle : t('product.hero.missingBreadcrumbTitle')

    if (!titleValue.trim().length) {
      return acc
    }

    const normalizedTitle = titleValue.trim().toLowerCase()
    const normalizedLinkValue = breadcrumb?.link?.toString().trim().toLowerCase() ?? ''

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
  }, [])
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

const impactScore = computed(() => {
  const ecoscore = props.product.base?.ecoscoreValue
  if (typeof ecoscore === 'number') {
    return ecoscore
  }

  const relative = props.product.scores?.ecoscore?.relativeValue
  return typeof relative === 'number' ? relative : null
})

const impactScoreFormatted = computed(() => {
  if (impactScore.value === null) {
    return null
  }

  const value = impactScore.value
  const hasFraction = Math.abs(value % 1) > 0.001

  return n(value, {
    style: 'decimal',
    minimumFractionDigits: hasFraction ? 1 : 0,
    maximumFractionDigits: hasFraction ? 1 : 0,
  })
})

const impactBadgeTitle = computed(() => {
  if (impactScoreFormatted.value === null) {
    return ''
  }

  if (te('product.hero.impactBadge.title')) {
    return t('product.hero.impactBadge.title', { score: impactScoreFormatted.value })
  }

  if (te('product.hero.impactScoreLabel')) {
    return `${t('product.hero.impactScoreLabel')}: ${impactScoreFormatted.value}`
  }

  return `Impact score: ${impactScoreFormatted.value}`
})

const impactBadgeLinkLabel = computed(() => {
  if (te('product.hero.impactBadge.learnMore')) {
    return t('product.hero.impactBadge.learnMore')
  }

  if (te('common.actions.learnMore')) {
    return t('common.actions.learnMore')
  }

  if (te('common.learnMore')) {
    return t('common.learnMore')
  }

  return 'Learn more'
})

const verticalHomeSegment = computed(() => {
  const fullSlug = props.product.fullSlug?.toString().trim() ?? ''
  const slug = props.product.slug?.toString().trim() ?? ''

  if (fullSlug && slug && fullSlug.endsWith(slug)) {
    const base = fullSlug.slice(0, fullSlug.length - slug.length).replace(/\/$/, '')
    const normalized = base.replace(/^\/+/, '')
    return normalized.length ? normalized : null
  }

  if (fullSlug) {
    const segments = fullSlug.split('/').filter(Boolean)
    segments.pop()
    if (!segments.length) {
      return null
    }

    return segments.join('/')
  }

  if (slug.includes('/')) {
    const segments = slug.split('/').filter(Boolean)
    segments.pop()
    if (!segments.length) {
      return null
    }

    return segments.join('/')
  }

  return null
})

const impactScoreLearnMoreLink = computed(() => {
  const base = verticalHomeSegment.value
  if (!base) {
    return null
  }

  const normalized = base.replace(/\/$/, '')
  return `/${normalized}/ecoscore`
})
</script>

<style scoped>
.product-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr) minmax(0, 0.8fr);
  gap: 2.5rem;
  padding: 2.5rem;
  border-radius: 32px;
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-ice-050), 0.9), rgba(var(--v-theme-surface-glass), 0.95));
  box-shadow: 0 32px 70px rgba(15, 23, 42, 0.08);
}


.product-hero__breadcrumbs {
  display: flex;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-hero__breadcrumbs :deep(.category-navigation-breadcrumbs__link) {
  color: inherit;
}

.product-hero__breadcrumbs :deep(.category-navigation-breadcrumbs__link:hover),
.product-hero__breadcrumbs :deep(.category-navigation-breadcrumbs__link:focus-visible) {
  color: rgb(var(--v-theme-primary));
}

.product-hero__breadcrumbs :deep(.category-navigation-breadcrumbs__current) {
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__heading {
  grid-column: 1 / -1;
  text-align: center;
  margin-bottom: 1.25rem;
}

.product-hero__title {
  font-size: clamp(2rem, 2.8vw, 3rem);
  font-weight: 700;
  line-height: 1.1;
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__details {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-hero__brand-line {
  font-size: 1.05rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
  margin: 0;
}

.product-hero__name {
  font-size: 1.2rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
  margin: 0;
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

.product-hero__meta-group {
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: clamp(0.75rem, 2vh, 1.25rem);
  flex: 1 1 auto;
  min-height: 0;
}

.product-hero__meta-top,
.product-hero__meta-bottom {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  align-items: flex-start;
}

.product-hero__meta-middle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.product-hero__origin {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
  width: fit-content;
}

.product-hero__flag {
  border-radius: 4px;
  width: 32px;
  height: 24px;
  object-fit: cover;
}

.product-hero__impact-card {
  width: 100%;
  border-radius: 24px;
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-surface-primary-050), 0.95),
    rgba(var(--v-theme-surface-glass), 0.92)
  );
  box-shadow: 0 24px 54px rgba(15, 23, 42, 0.12);
  padding: clamp(1.25rem, 2vw, 1.75rem);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.product-hero__impact-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__impact-score {
  display: flex;
  justify-content: center;
}

.product-hero__impact-link {
  align-self: flex-end;
  font-weight: 600;
  font-size: 0.9rem;
  color: rgb(var(--v-theme-primary));
  text-decoration: none;
  transition: color 0.2s ease, text-decoration-color 0.2s ease;
}

.product-hero__impact-link:hover,
.product-hero__impact-link:focus-visible {
  color: rgb(var(--v-theme-accent-primary-highlight));
  text-decoration: underline;
  text-decoration-thickness: 2px;
}

.product-hero__meta-bottom {
  justify-content: flex-end;
}

@media (max-width: 1280px) {
  .product-hero__impact-card {
    padding: clamp(1.1rem, 2.4vw, 1.5rem);
  }
}

@media (max-width: 960px) {
  .product-hero__impact-card {
    padding: 1.1rem;
  }

  .product-hero__impact-title {
    font-size: 0.95rem;
  }

  .product-hero__impact-link {
    font-size: 0.85rem;
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
  transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
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
