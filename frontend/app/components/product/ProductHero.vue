<template>
  <section
    class="product-hero"
    itemscope
    itemtype="https://schema.org/Product"
  >
    <meta itemprop="sku" :content="String(product.base?.gtin ?? '')" />
    <meta itemprop="brand" :content="product.identity?.brand ?? ''" />

    <ProductHeroGallery class="product-hero__gallery" :product="product" :title="title" />

    <div class="product-hero__details">
      <CategoryNavigationBreadcrumbs
        v-if="heroBreadcrumbs.length"
        v-bind="heroBreadcrumbProps"
        class="product-hero__breadcrumbs"
      />
      <p v-if="product.identity?.brand" class="product-hero__eyebrow">
        {{ product.identity.brand }}
      </p>
      <h1 class="product-hero__title" itemprop="name">
        {{ title }}
      </h1>
      <p v-if="product.identity?.model" class="product-hero__subtitle">
        {{ product.identity.model }}
      </p>

      <div class="product-hero__meta">
        <ImpactScore
          v-if="impactScore !== null"
          :score="impactScore"
          :max="5"
          size="large"
          :show-value="true"
          class="product-hero__impact"
        />
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
import type { ProductDto } from '~~/shared/api-client'

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
})

const { t, te } = useI18n()

const title = computed(
  () =>
    props.product.names?.h1Title ??
    props.product.identity?.bestName ??
    props.product.base?.bestName ??
    '',
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
  const productName = title.value || props.product.identity?.bestName || props.product.base?.bestName

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
  const normalizedProductTitle = title.value.trim().toLowerCase()
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


.product-hero__details {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
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

.product-hero__eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-weight: 600;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.8);
}

.product-hero__title {
  font-size: clamp(2rem, 2.8vw, 3rem);
  font-weight: 700;
  line-height: 1.1;
  margin: 0;
}

.product-hero__subtitle {
  font-size: 1.1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
}

.product-hero__impact {
  display: inline-flex;
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
