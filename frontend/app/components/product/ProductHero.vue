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

      <div class="product-hero__facts">
        <div class="product-hero__fact">
          <span class="product-hero__fact-label">{{ $t('product.hero.gtin') }}</span>
          <span class="product-hero__fact-value" itemprop="gtin13">
            {{ product.gtin }}
          </span>
        </div>
        <div class="product-hero__fact">
          <span class="product-hero__fact-label">{{ $t('product.hero.offersCount') }}</span>
          <span class="product-hero__fact-value">
            {{ offersCountLabel }}
          </span>
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

const { n, t } = useI18n()

const title = computed(
  () =>
    props.product.names?.h1Title ??
    props.product.identity?.bestName ??
    props.product.base?.bestName ??
    '',
)

const offersCount = computed(() => props.product.offers?.offersCount ?? 0)
const offersCountLabel = computed(() => n(offersCount.value))

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

.product-hero__facts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 1rem;
  margin-top: 0.5rem;
}

.product-hero__fact {
  background: rgba(var(--v-theme-surface-glass-strong), 0.6);
  border-radius: 14px;
  padding: 0.875rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.product-hero__fact-label {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.product-hero__fact-value {
  font-weight: 700;
  font-size: 1rem;
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
