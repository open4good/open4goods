<template>
  <section
    class="product-hero__pricing-panel"
    :class="`product-hero__pricing-panel--${condition}`"
  >
    <header class="product-hero__pricing-panel-header">
      <div class="product-hero__pricing-panel-title">
        <v-icon
          :icon="conditionIcon"
          size="18"
          class="product-hero__pricing-panel-title-icon"
        />
        <span>{{ conditionLabel }}</span>
      </div>
      <div v-if="offersCountLabel" class="product-hero__pricing-panel-count">
        {{ offersCountLabel }}
      </div>
    </header>

    <div class="product-hero__pricing-panel-body">
      <template v-if="hasOffer">
        <div class="product-hero__pricing-panel-main">
          <div v-if="merchant" class="product-hero__pricing-panel-merchant">
            <v-tooltip v-if="offerName" :text="offerName" location="bottom">
              <template #activator="{ props: tooltipProps }">
                <div v-bind="tooltipProps">
                  <ClientOnly v-if="merchant.clientOnly">
                    <template #default>
                      <NuxtLink
                        :to="merchant.url"
                        class="product-hero__pricing-panel-merchant-link"
                        :target="merchant.isInternal ? undefined : '_blank'"
                        :rel="
                          merchant.isInternal ? undefined : 'nofollow noopener'
                        "
                        :prefetch="false"
                        @click="emitMerchantClick"
                      >
                        <img
                          v-if="merchant.favicon"
                          :src="merchant.favicon"
                          :alt="merchant.name"
                          width="52"
                          height="52"
                          class="product-hero__pricing-panel-merchant-favicon"
                        />
                        <span
                          v-if="showMerchantName"
                          class="product-hero__pricing-panel-merchant-name ml-2"
                          >{{ merchant.name }}</span
                        >
                      </NuxtLink>
                    </template>
                    <template #fallback>
                      <div class="product-hero__pricing-panel-merchant-static">
                        <img
                          v-if="merchant.favicon"
                          :src="merchant.favicon"
                          :alt="merchant.name"
                          width="52"
                          height="52"
                          class="product-hero__pricing-panel-merchant-favicon"
                        />
                        <span
                          v-if="showMerchantName"
                          class="product-hero__pricing-panel-merchant-name ml-2"
                          >{{ merchant.name }}</span
                        >
                      </div>
                    </template>
                  </ClientOnly>
                  <NuxtLink
                    v-else-if="merchant.url"
                    :to="merchant.url"
                    class="product-hero__pricing-panel-merchant-link"
                    :target="merchant.isInternal ? undefined : '_blank'"
                    :rel="merchant.isInternal ? undefined : 'nofollow noopener'"
                    :prefetch="false"
                    @click="emitMerchantClick"
                  >
                    <img
                      v-if="merchant.favicon"
                      :src="merchant.favicon"
                      :alt="merchant.name"
                      width="52"
                      height="52"
                      class="product-hero__pricing-panel-merchant-favicon"
                    />
                    <span
                      v-if="showMerchantName"
                      class="product-hero__pricing-panel-merchant-name ml-2"
                      >{{ merchant.name }}</span
                    >
                  </NuxtLink>
                  <div
                    v-else
                    class="product-hero__pricing-panel-merchant-static"
                  >
                    <img
                      v-if="merchant.favicon"
                      :src="merchant.favicon"
                      :alt="merchant.name"
                      width="52"
                      height="52"
                      class="product-hero__pricing-panel-merchant-favicon"
                    />
                    <span
                      v-if="showMerchantName"
                      class="product-hero__pricing-panel-merchant-name ml-2"
                      >{{ merchant.name }}</span
                    >
                  </div>
                </div>
              </template>
            </v-tooltip>
            <template v-else>
              <ClientOnly v-if="merchant.clientOnly">
                <template #default>
                  <NuxtLink
                    :to="merchant.url"
                    class="product-hero__pricing-panel-merchant-link"
                    :target="merchant.isInternal ? undefined : '_blank'"
                    :rel="merchant.isInternal ? undefined : 'nofollow noopener'"
                    :prefetch="false"
                    @click="emitMerchantClick"
                  >
                    <img
                      v-if="merchant.favicon"
                      :src="merchant.favicon"
                      :alt="merchant.name"
                      width="52"
                      height="52"
                      class="product-hero__pricing-panel-merchant-favicon"
                    />
                    <span
                      v-if="showMerchantName"
                      class="product-hero__pricing-panel-merchant-name ml-2"
                      >{{ merchant.name }}</span
                    >
                  </NuxtLink>
                </template>
                <template #fallback>
                  <div class="product-hero__pricing-panel-merchant-static">
                    <img
                      v-if="merchant.favicon"
                      :src="merchant.favicon"
                      :alt="merchant.name"
                      width="52"
                      height="52"
                      class="product-hero__pricing-panel-merchant-favicon"
                    />
                    <span v-if="showMerchantName" class="ml-2">{{
                      merchant.name
                    }}</span>
                  </div>
                </template>
              </ClientOnly>
              <NuxtLink
                v-else-if="merchant.url"
                :to="merchant.url"
                class="product-hero__pricing-panel-merchant-link"
                :target="merchant.isInternal ? undefined : '_blank'"
                :rel="merchant.isInternal ? undefined : 'nofollow noopener'"
                :prefetch="false"
                @click="emitMerchantClick"
              >
                <img
                  v-if="merchant.favicon"
                  :src="merchant.favicon"
                  :alt="merchant.name"
                  width="52"
                  height="52"
                  class="product-hero__pricing-panel-merchant-favicon"
                />
                <span
                  v-if="showMerchantName"
                  class="product-hero__pricing-panel-merchant-name ml-2"
                  >{{ merchant.name }}</span
                >
              </NuxtLink>
              <div v-else class="product-hero__pricing-panel-merchant-static">
                <img
                  v-if="merchant.favicon"
                  :src="merchant.favicon"
                  :alt="merchant.name"
                  width="52"
                  height="52"
                  class="product-hero__pricing-panel-merchant-favicon"
                />
                <span
                  v-if="showMerchantName"
                  class="product-hero__pricing-panel-merchant-name ml-2"
                  >{{ merchant.name }}</span
                >
              </div>
            </template>
          </div>

          <div class="product-hero__pricing-panel-price">
            <p class="product-hero__pricing-panel-price-label">
              {{ priceTitle }}
            </p>
            <div class="product-hero__pricing-panel-price-value">
              <span class="product-hero__pricing-panel-price-amount">
                {{ priceLabel }}
              </span>
              <span
                v-if="priceCurrency"
                class="product-hero__pricing-panel-price-currency"
              >
                {{ priceCurrency }}
              </span>
            </div>
          </div>
        </div>

        <v-select
          v-if="alternativeOffers?.length"
          v-model="selectedAlternative"
          class="product-hero__pricing-panel-select"
          :items="alternativeOffers"
          :label="alternativeOffersLabel"
          :placeholder="alternativeOffersPlaceholder"
          item-title="label"
          item-value="id"
          return-object
          variant="outlined"
          density="comfortable"
          hide-details
          menu-icon="mdi-chevron-down"
        >
          <template #selection="{ item }">
            <div class="product-hero__pricing-panel-select-value">
              <img
                v-if="item.raw.favicon"
                :src="item.raw.favicon"
                :alt="item.raw.label"
                width="20"
                height="20"
                class="product-hero__pricing-panel-select-avatar"
              />
              <span class="product-hero__pricing-panel-select-name ml-4">
                {{ item.raw.label }}
              </span>
              <span class="product-hero__pricing-panel-select-price">
                {{ item.raw.priceLabel }}
              </span>
            </div>
          </template>
          <template #item="{ item, props: itemProps }">
            <v-list-item v-bind="itemProps">
              <template #prepend>
                <img
                  v-if="item.raw.favicon"
                  :src="item.raw.favicon"
                  :alt="item.raw.label"
                  width="24"
                  height="24"
                  class="product-hero__pricing-panel-select-avatar"
                />
                <v-icon v-else icon="mdi-storefront-outline" size="20" />
              </template>
              <v-list-item-title>
                {{ item.raw.label }}
              </v-list-item-title>
              <v-list-item-subtitle
                v-if="
                  item.raw.offerName && item.raw.offerName !== item.raw.label
                "
              >
                {{ item.raw.offerName }}
              </v-list-item-subtitle>
              <template #append>
                <span class="product-hero__pricing-panel-select-price">
                  {{ item.raw.priceLabel }}
                </span>
              </template>
            </v-list-item>
          </template>
        </v-select>
      </template>

      <div v-else class="product-hero__pricing-panel-empty">
        <v-icon icon="mdi-basket-off-outline" size="20" />
        <span>{{ emptyStateLabel }}</span>
      </div>
    </div>

    <footer class="product-hero__pricing-panel-footer">
      <v-tooltip
        v-if="trendLabel && trendTooltip"
        :text="trendTooltip"
        location="bottom"
      >
        <template #activator="{ props: tooltipProps }">
          <button
            type="button"
            class="product-hero__price-trend ma-0"
            :class="trendToneClass"
            v-bind="tooltipProps"
            @click="emitTrendClick"
          >
            <v-icon
              :icon="trendIcon"
              size="18"
              class="product-hero__price-trend-icon"
            />
            <span>{{ trendLabel }}</span>
          </button>
        </template>
      </v-tooltip>
      <button
        v-else-if="trendLabel"
        type="button"
        class="product-hero__price-trend ma-0"
        :class="trendToneClass"
        @click="emitTrendClick"
      >
        <v-icon
          :icon="trendIcon"
          size="18"
          class="product-hero__price-trend-icon"
        />
        <span>{{ trendLabel }}</span>
      </button>

      <div class="product-hero__price-actions">
        <v-btn
          v-if="hasOffer"
          color="primary"
          variant="flat"
          @click="emitViewOffers"
        >
          {{ viewOffersLabel }}
        </v-btn>
      </div>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

type OfferCondition = 'occasion' | 'new'

type MerchantInfo = {
  name: string
  url: string | null
  favicon: string | null
  isInternal: boolean
  clientOnly: boolean
}

type AlternativeOfferOption = {
  id: string
  label: string
  offerName: string | null
  priceLabel: string
  favicon: string | null
}

const props = defineProps({
  condition: {
    type: String as () => OfferCondition,
    required: true,
  },
  conditionLabel: {
    type: String,
    required: true,
  },
  offersCountLabel: {
    type: String,
    default: null,
  },
  priceTitle: {
    type: String,
    required: true,
  },
  priceLabel: {
    type: String,
    required: true,
  },
  priceCurrency: {
    type: String,
    default: null,
  },
  emptyStateLabel: {
    type: String,
    required: true,
  },
  hasOffer: {
    type: Boolean,
    required: true,
  },
  merchant: {
    type: Object as () => MerchantInfo | null,
    default: null,
  },
  offerName: {
    type: String,
    default: null,
  },
  showMerchantName: {
    type: Boolean,
    default: false,
  },
  trendLabel: {
    type: String,
    default: null,
  },
  trendTooltip: {
    type: String,
    default: null,
  },
  trendToneClass: {
    type: String,
    default: '',
  },
  trendIcon: {
    type: String,
    default: '',
  },
  viewOffersLabel: {
    type: String,
    required: true,
  },
  alternativeOffers: {
    type: Array as () => AlternativeOfferOption[] | null,
    default: null,
  },
  alternativeOffersLabel: {
    type: String,
    default: null,
  },
  alternativeOffersPlaceholder: {
    type: String,
    default: null,
  },
})

const emit = defineEmits<{
  (
    event: 'merchant-click',
    payload: { name: string | null; url: string | null }
  ): void
  (event: 'trend-click' | 'view-offers'): void
}>()

const conditionIcon = computed(() =>
  props.condition === 'new' ? 'mdi-tag-outline' : 'mdi-recycle-variant'
)

const emitMerchantClick = () => {
  emit('merchant-click', {
    name: props.merchant?.name ?? null,
    url: props.merchant?.url ?? null,
  })
}

const emitTrendClick = () => {
  emit('trend-click')
}

const emitViewOffers = () => {
  emit('view-offers')
}

const selectedAlternative = ref<AlternativeOfferOption | null>(null)

watch(
  () => props.alternativeOffers,
  offers => {
    if (offers?.length && !selectedAlternative.value) {
      selectedAlternative.value = offers[0]
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.product-hero__pricing-panel {
  border-radius: 20px;
  padding: 1.1rem 1rem;
  background: rgba(var(--v-theme-surface-glass), 0.95);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-hero__pricing-panel--new {
  --product-hero-panel-accent: rgb(var(--v-theme-accent-primary-highlight));
}

.product-hero__pricing-panel--occasion {
  --product-hero-panel-accent: rgb(var(--v-theme-accent-supporting));
}

.product-hero__pricing-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.product-hero__pricing-panel-title {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-weight: 700;
  color: var(--product-hero-panel-accent);
}

.product-hero__pricing-panel-title-icon {
  color: var(--product-hero-panel-accent);
}

.product-hero__pricing-panel-count {
  font-size: 0.85rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-hero__pricing-panel-body {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.product-hero__pricing-panel-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.product-hero__pricing-panel-merchant-link,
.product-hero__pricing-panel-merchant-static {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.5rem 0.75rem;
  border-radius: 16px;
  font-weight: 600;
  text-decoration: none;
  background: rgba(var(--v-theme-surface-default), 0.94);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.12);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__pricing-panel-merchant-link {
  color: var(--product-hero-panel-accent);
}

.product-hero__pricing-panel-merchant-link:hover,
.product-hero__pricing-panel-merchant-link:focus-visible {
  box-shadow: 0 16px 32px rgba(var(--v-theme-shadow-primary-600), 0.18);
}

.product-hero__pricing-panel-merchant-favicon {
  border-radius: 8px;
  width: 52px;
  height: 52px;
  object-fit: cover;
}

.product-hero__pricing-panel-price {
  text-align: right;
  margin-left: auto;
}

.product-hero__pricing-panel-price-label {
  margin: 0 0 0.2rem;
  font-size: 0.85rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-hero__pricing-panel-price-value {
  display: flex;
  align-items: baseline;
  gap: 0.4rem;
  justify-content: flex-end;
}

.product-hero__pricing-panel-price-amount {
  font-size: clamp(1.6rem, 3.5vw, 2.2rem);
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__pricing-panel-price-currency {
  font-size: 1rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-hero__pricing-panel-empty {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.75rem;
  border-radius: 16px;
  background: rgba(var(--v-theme-surface-primary-050), 0.8);
  color: rgb(var(--v-theme-text-neutral-secondary));
  font-weight: 600;
}

.product-hero__pricing-panel-select {
  width: 100%;
}

.product-hero__pricing-panel-select-value {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
}

.product-hero__pricing-panel-select-name {
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__pricing-panel-select-price {
  margin-left: auto;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__pricing-panel-select-avatar {
  border-radius: 6px;
  object-fit: cover;
}

.product-hero__pricing-panel-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  justify-content: space-between;
}

.product-hero__price-trend {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.6rem;
  border: none;
  border-radius: 999px;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease;
  background: transparent;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-hero__price-trend:hover,
.product-hero__price-trend:focus-visible {
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.14);
}

.product-hero__price-trend-icon {
  transition: color 0.2s ease;
}

.product-hero__price-trend--stable {
  background: rgba(var(--v-theme-surface-primary-080), 0.6);
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-hero__price-trend--stable .product-hero__price-trend-icon {
  color: rgb(var(--v-theme-accent-primary-highlight));
}

.product-hero__price-trend--stable:hover,
.product-hero__price-trend--stable:focus-visible {
  background: rgba(var(--v-theme-surface-primary-100), 0.85);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__price-trend--decrease {
  background: rgba(var(--v-theme-primary), 0.14);
  color: rgb(var(--v-theme-primary));
}

.product-hero__price-trend--decrease .product-hero__price-trend-icon {
  color: rgb(var(--v-theme-primary));
}

.product-hero__price-trend--decrease:hover,
.product-hero__price-trend--decrease:focus-visible {
  background: rgba(var(--v-theme-primary), 0.22);
}

.product-hero__price-trend--increase {
  background: rgba(var(--v-theme-error), 0.18);
  color: rgb(var(--v-theme-error));
}

.product-hero__price-trend--increase .product-hero__price-trend-icon {
  color: rgb(var(--v-theme-error));
}

.product-hero__price-trend--increase:hover,
.product-hero__price-trend--increase:focus-visible {
  background: rgba(var(--v-theme-error), 0.26);
}

.product-hero__price-actions {
  margin-top: 0.5rem;
}

@media (max-width: 600px) {
  .product-hero__pricing-panel-main {
    flex-direction: column;
    align-items: flex-start;
  }

  .product-hero__pricing-panel-price {
    text-align: left;
  }

  .product-hero__pricing-panel-price-value {
    justify-content: flex-start;
  }
}

@media (min-width: 960px) {
  .product-hero__pricing-panel-merchant-name {
    display: none;
  }
}
</style>
