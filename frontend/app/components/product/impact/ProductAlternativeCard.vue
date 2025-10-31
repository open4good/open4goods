<template>
  <article class="product-alternative-card">
    <NuxtLink
      :to="productLink"
      class="product-alternative-card__link"
      :aria-label="t('product.impact.alternatives.viewProduct', { name: displayName })"
      :prefetch="false"
    >
      <div class="product-alternative-card__media">
        <NuxtImg
          v-if="image"
          :src="image"
          :alt="displayName"
          width="220"
          height="150"
          class="product-alternative-card__image"
        />
        <div v-else class="product-alternative-card__placeholder">
          <v-icon icon="mdi-image-off" size="32" />
        </div>
      </div>
      <div class="product-alternative-card__body">
        <h4 class="product-alternative-card__title">{{ displayName }}</h4>
        <p v-if="subtitle" class="product-alternative-card__subtitle">{{ subtitle }}</p>
        <div class="product-alternative-card__meta">
          <ImpactScore
            v-if="impactScore != null"
            :score="impactScore"
            :max="5"
            size="small"
          />
          <span class="product-alternative-card__price">{{ bestPrice }}</span>
        </div>
      </div>
    </NuxtLink>
    <CategoryProductCompareToggle
      :product="product"
      size="compact"
      class="product-alternative-card__compare"
      icon="mdi-compare"
    />
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProductDto } from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import CategoryProductCompareToggle from '~/components/category/products/CategoryProductCompareToggle.vue'
import { formatBestPrice } from '~/utils/_product-pricing'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
})

const { t, n } = useI18n()

const product = computed(() => props.product)

const image = computed(() => {
  const candidate =
    product.value.resources?.coverImagePath ??
    product.value.resources?.externalCover ??
    product.value.resources?.images?.[0]?.url ??
    null

  return candidate || null
})

const displayName = computed(() => {
  return (
    product.value.identity?.bestName ??
    product.value.base?.bestName ??
    product.value.names?.h1Title ??
    product.value.identity?.model ??
    product.value.identity?.brand ??
    t('product.impact.alternatives.untitled')
  )
})

const subtitle = computed(() => {
  const brand = product.value.identity?.brand ?? null
  const model = product.value.identity?.model ?? null

  if (brand && model) {
    return `${brand} • ${model}`
  }

  return brand ?? model ?? null
})

const bestPrice = computed(() => formatBestPrice(product.value, t, n))

const impactScore = computed(() => resolvePrimaryImpactScore(product.value))

const productLink = computed(() => {
  return product.value.fullSlug ?? product.value.slug ?? '#'
})
</script>

<style scoped>
.product-alternative-card {
  position: relative;
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  background: rgba(var(--v-theme-surface-default), 0.96);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.product-alternative-card__link {
  display: flex;
  flex-direction: column;
  text-decoration: none;
  color: inherit;
  padding-bottom: 0.75rem;
}

.product-alternative-card__link:hover,
.product-alternative-card__link:focus-visible {
  color: inherit;
}

.product-alternative-card:hover,
.product-alternative-card:focus-within {
  transform: translateY(-4px);
  box-shadow: 0 18px 32px rgba(15, 23, 42, 0.18);
}

.product-alternative-card__media {
  position: relative;
  padding: 0.75rem;
  background: rgba(var(--v-theme-surface-primary-080), 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-alternative-card__image {
  width: 100%;
  height: auto;
  max-height: 160px;
  object-fit: contain;
  border-radius: 14px;
}

.product-alternative-card__placeholder {
  width: 100%;
  height: 140px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(var(--v-theme-text-neutral-soft), 0.6);
  background: rgba(var(--v-theme-surface-glass), 0.6);
}

.product-alternative-card__body {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0 1rem;
}

.product-alternative-card__title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.3;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-alternative-card__subtitle {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.8);
}

.product-alternative-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.product-alternative-card__price {
  font-weight: 600;
  font-size: 0.95rem;
  color: rgb(var(--v-theme-primary));
}

.product-alternative-card__compare {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
}
</style>
