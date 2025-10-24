<template>
  <div
    class="product-hero__pricing-card"
    itemprop="offers"
    itemscope
    itemtype="https://schema.org/AggregateOffer"
    v-bind="$attrs"
  >
    <meta itemprop="offerCount" :content="String(product.offers?.offersCount ?? 0)" />
    <meta itemprop="priceCurrency" :content="product.offers?.bestPrice?.currency ?? 'EUR'" />
    <h2 class="product-hero__pricing-title">
      {{ $t('product.hero.bestPriceTitle') }}
    </h2>
    <div class="product-hero__price">
      <span class="product-hero__price-value" itemprop="lowPrice">
        {{ bestPriceLabel }}
      </span>
      <span class="product-hero__price-currency">
        {{ product.offers?.bestPrice?.currency ?? '€' }}
      </span>
    </div>
    <div class="product-hero__price-meta">
      <div v-if="bestPriceMerchant" class="product-hero__price-merchant">
        <span class="product-hero__price-merchant-prefix">{{ t('product.hero.priceMerchantPrefix') }}</span>
        <NuxtLink
          :to="bestPriceMerchant.url"
          class="product-hero__price-merchant-link"
          target="_blank"
          rel="nofollow noopener"
        >
          <img
            v-if="bestPriceMerchant.favicon"
            :src="bestPriceMerchant.favicon"
            :alt="bestPriceMerchant.name"
            width="18"
            height="18"
            class="product-hero__price-merchant-favicon"
          />
          <span>{{ bestPriceMerchant.name }}</span>
        </NuxtLink>
      </div>

      <button
        v-if="priceTrendLabel"
        type="button"
        class="product-hero__price-trend"
        @click="scrollToSelector('#price-history')"
      >
        <v-icon icon="mdi-chart-line" size="18" class="product-hero__price-trend-icon" />
        <span>{{ priceTrendLabel }}</span>
      </button>
    </div>
    <div class="product-hero__price-actions">
      <v-btn color="primary" variant="flat" @click="scrollToSelector('#offers-list', 136)">
        {{ viewOffersLabel }}
      </v-btn>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProductDto } from '~~/shared/api-client'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
})

const { n, t } = useI18n()

const bestPrice = computed(() => props.product.offers?.bestPrice ?? null)

const bestPriceLabel = computed(() => {
  if (!bestPrice.value?.price) {
    return '—'
  }

  return n(bestPrice.value.price, {
    style: 'currency',
    currency: bestPrice.value.currency ?? 'EUR',
    maximumFractionDigits: 0,
  })
})

const offersCount = computed(() => props.product.offers?.offersCount ?? 0)

const offersCountLabel = computed(() => n(offersCount.value))

const viewOffersLabel = computed(() =>
  offersCount.value <= 1
    ? t('product.hero.viewSingleOffer')
    : t('product.hero.viewOffersCount', { count: offersCountLabel.value }),
)

const bestPriceMerchant = computed(() => {
  const merchant = props.product.offers?.bestPrice
  if (!merchant?.datasourceName || !merchant?.url) {
    return null
  }

  return {
    name: merchant.datasourceName,
    url: merchant.url,
    favicon: merchant.favicon ?? null,
  }
})

const priceTrendLabel = computed(() => {
  const trend = props.product.offers?.newTrend
  if (!trend) {
    return null
  }

  if (trend.trend === 'PRICE_DECREASE') {
    return t('product.price.trend.decrease', {
      amount: n(Math.abs(trend.variation ?? 0), {
        style: 'currency',
        currency: props.product.offers?.bestPrice?.currency ?? 'EUR',
        maximumFractionDigits: 2,
      }),
    })
  }

  if (trend.trend === 'PRICE_INCREASE') {
    return t('product.price.trend.increase', {
      amount: n(Math.abs(trend.variation ?? 0), {
        style: 'currency',
        currency: props.product.offers?.bestPrice?.currency ?? 'EUR',
        maximumFractionDigits: 2,
      }),
    })
  }

  return t('product.price.trend.stable')
})

const scrollToSelector = (selector: string, offset = 120) => {
  if (!import.meta.client) {
    return
  }

  const target = document.querySelector<HTMLElement>(selector)
  if (!target) {
    return
  }

  const top = target.getBoundingClientRect().top + (window.scrollY || window.pageYOffset || 0) - offset
  window.scrollTo({ top: Math.max(0, top), behavior: 'smooth' })
}
</script>

<style scoped>
.product-hero__pricing-card {
  border-radius: 24px;
  background: rgba(var(--v-theme-surface-glass-strong), 0.9);
  padding: 1.75rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.1);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-hero__pricing-title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
}

.product-hero__price {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.product-hero__price-value {
  font-size: clamp(2rem, 3.4vw, 2.6rem);
  font-weight: 700;
}

.product-hero__price-currency {
  font-size: 1.1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.8);
}

.product-hero__price-meta {
  margin-top: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  align-items: flex-start;
}

.product-hero__price-merchant {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-hero__price-merchant-prefix {
  font-weight: 600;
}

.product-hero__price-merchant-link {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-weight: 600;
  color: rgb(var(--v-theme-primary));
  text-decoration: none;
}

.product-hero__price-merchant-link:hover,
.product-hero__price-merchant-link:focus-visible {
  text-decoration: underline;
}

.product-hero__price-merchant-favicon {
  border-radius: 4px;
  width: 18px;
  height: 18px;
  object-fit: cover;
}

.product-hero__price-trend {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.5rem;
  border: none;
  border-radius: 999px;
  background: rgba(var(--v-theme-surface-primary-080), 0.6);
  color: rgb(var(--v-theme-text-neutral-secondary));
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.product-hero__price-trend:hover,
.product-hero__price-trend:focus-visible {
  background: rgba(var(--v-theme-surface-primary-100), 0.85);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__price-trend-icon {
  color: rgb(var(--v-theme-accent-primary-highlight));
}

.product-hero__price-actions {
  margin-top: 0.5rem;
}
</style>
