<template>
  <section
    class="product-hero__pricing-panel"
    :class="`product-hero__pricing-panel--${condition}`"
  >
    <header
      v-if="!hideHeader"
      class="product-hero__pricing-panel-header"
    >
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
        <v-tooltip v-if="offerName" :text="offerName" location="bottom">
          <template #activator="{ props: tooltipProps }">
            <div
              class="product-hero__pricing-panel-main-wrapper"
              v-bind="tooltipProps"
            >
              <ClientOnly v-if="merchant?.clientOnly">
                <template #default>
                  <NuxtLink
                    :to="merchant.url"
                    class="product-hero__pricing-panel-main product-hero__pricing-panel-main--link"
                    :target="merchant.isInternal ? undefined : '_blank'"
                    :rel="merchant.isInternal ? undefined : 'nofollow noopener'"
                    :prefetch="false"
                    @click="emitMerchantClick"
                  >
                    <div class="product-hero__pricing-panel-merchant">
                      <img
                        v-if="merchant.favicon"
                        :src="merchant.favicon"
                        alt=""
                        width="40"
                        height="40"
                        class="product-hero__pricing-panel-merchant-favicon"
                      />
                      <span
                        v-if="showMerchantName"
                        class="product-hero__pricing-panel-merchant-name ml-2"
                      >
                        {{ merchant.name }}
                      </span>
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
                  </NuxtLink>
                </template>
                <template #fallback>
                  <div class="product-hero__pricing-panel-main">
                    <div class="product-hero__pricing-panel-merchant">
                      <img
                        v-if="merchant?.favicon"
                        :src="merchant.favicon"
                        alt=""
                        width="40"
                        height="40"
                        class="product-hero__pricing-panel-merchant-favicon"
                      />
                      <span
                        v-if="showMerchantName && merchant"
                        class="product-hero__pricing-panel-merchant-name ml-2"
                      >
                        {{ merchant.name }}
                      </span>
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
                </template>
              </ClientOnly>

              <NuxtLink
                v-else-if="merchant?.url"
                :to="merchant.url"
                class="product-hero__pricing-panel-main product-hero__pricing-panel-main--link"
                :target="merchant.isInternal ? undefined : '_blank'"
                :rel="merchant.isInternal ? undefined : 'nofollow noopener'"
                :prefetch="false"
                @click="emitMerchantClick"
              >
                <div class="product-hero__pricing-panel-merchant">
                  <img
                    v-if="merchant.favicon"
                    :src="merchant.favicon"
                    alt=""
                    width="40"
                    height="40"
                    class="product-hero__pricing-panel-merchant-favicon"
                  />
                  <span
                    v-if="showMerchantName"
                    class="product-hero__pricing-panel-merchant-name ml-2"
                  >
                    {{ merchant.name }}
                  </span>
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
              </NuxtLink>

              <div v-else class="product-hero__pricing-panel-main">
                <div
                  v-if="merchant"
                  class="product-hero__pricing-panel-merchant"
                >
                  <img
                    v-if="merchant.favicon"
                    :src="merchant.favicon"
                    alt=""
                    width="40"
                    height="40"
                    class="product-hero__pricing-panel-merchant-favicon"
                  />
                  <span
                    v-if="showMerchantName"
                    class="product-hero__pricing-panel-merchant-name ml-2"
                  >
                    {{ merchant.name }}
                  </span>
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
            </div>
          </template>
        </v-tooltip>
        <!-- Replaced by the block above -->

        <div
          v-if="offersListToDisplay.length > 0"
          class="product-hero__pricing-panel-alternatives"
        >
          <p class="product-hero__pricing-panel-alternatives-title">
            <v-icon
              icon="mdi-shopping-search-outline"
              size="16"
              class="me-1"
            />
            {{ offersListLabel }}
          </p>
          <TransitionGroup
            tag="ul"
            name="alt-list"
            class="product-hero__pricing-panel-alternatives-list"
          >
            <li
              v-for="offer in offersListToDisplay"
              :key="offer.id"
              class="product-hero__pricing-panel-alternative"
            >
              <button
                type="button"
                class="product-hero__pricing-panel-alternative-row"
                @click="onOfferSelected(offer)"
              >
                <img
                  v-if="offer.favicon"
                  :src="offer.favicon"
                  alt=""
                  width="24"
                  height="24"
                  class="product-hero__pricing-panel-alternative-favicon"
                />
                <v-icon
                  v-else
                  icon="mdi-storefront-outline"
                  size="20"
                  class="product-hero__pricing-panel-alternative-favicon-fallback"
                />
                <span class="product-hero__pricing-panel-alternative-name">
                  {{ offer.label }}
                </span>
                <span class="product-hero__pricing-panel-alternative-price">
                  {{ offer.priceLabel }}
                </span>
                <v-icon
                  icon="mdi-chevron-right"
                  size="18"
                  class="product-hero__pricing-panel-alternative-chevron"
                />
              </button>
            </li>
          </TransitionGroup>
        </div>

        <p
          v-if="moreOffersLabel"
          class="product-hero__pricing-panel-more"
        >
          <v-icon icon="mdi-information-outline" size="14" class="me-1" />
          {{ moreOffersLabel }}
        </p>
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
import { computed } from 'vue'
import { useRouter } from '#imports'

type OfferCondition = 'occasion' | 'new'

type MerchantInfo = {
  name: string
  url: string | null
  favicon: string | null
  isInternal: boolean
  clientOnly: boolean
}

type OfferOption = {
  id: string
  label: string
  offerName: string | null
  priceLabel: string
  favicon: string | null
  url: string | null
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
  offersList: {
    type: Array as () => OfferOption[] | null,
    default: null,
  },
  offersListLabel: {
    type: String,
    default: null,
  },
  moreOffersLabel: {
    type: String,
    default: null,
  },
  hideHeader: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits<{
  (
    event: 'merchant-click',
    payload: { name: string | null; url: string | null }
  ): void
  (event: 'trend-click' | 'view-offers'): void
}>()

const router = useRouter()

const offersListToDisplay = computed(() => props.offersList ?? [])

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

const onOfferSelected = (item: OfferOption) => {
  if (item.url) {
    emit('merchant-click', {
      name: item.label,
      url: item.url,
    })

    const isInternal = item.url.startsWith('/')
    if (isInternal) {
      router.push(item.url)
    } else {
      window.open(item.url, '_blank', 'noopener,noreferrer')
    }
  }
}

</script>

<style scoped>
.product-hero__pricing-panel {
  border-radius: 24px;
  padding: 1.25rem;
  background: rgba(var(--v-theme-surface-glass-strong), 0.9);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  box-shadow: 0 14px 40px rgba(15, 23, 42, 0.08);
  transition:
    transform 0.3s ease,
    box-shadow 0.3s ease;
}

.product-hero__pricing-panel:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.12);
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
  color: rgb(var(--v-theme-text-neutral-strong));
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

.product-hero__pricing-panel-main-wrapper {
  width: 100%;
}

.product-hero__pricing-panel-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: nowrap;
  padding: 0.5rem 0;
  border-radius: 0;
  width: 100%;
  margin: 0;
  box-shadow: none;
  text-decoration: none;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__pricing-panel-main--link {
  transition:
    background-color 0.2s ease,
    transform 0.2s ease;
  padding: 0.75rem;
  border-radius: 16px;
}

.product-hero__pricing-panel-main--link:hover,
.product-hero__pricing-panel-main--link:focus-visible {
  box-shadow: none;
  cursor: pointer;
  background: rgba(var(--v-theme-surface-primary-080), 0.4);
  transform: translateY(-1px);
}

.product-hero__pricing-panel-merchant {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
  min-width: 0;
}

.product-hero__pricing-panel-merchant-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-hero__pricing-panel-merchant-favicon {
  border-radius: 8px;
  width: 40px;
  height: 40px;
  object-fit: cover;
  flex-shrink: 0;
}

.product-hero__pricing-panel-price {
  text-align: right;
  margin-left: auto;
  flex-shrink: 0;
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
  gap: 0.6rem;
  padding: 1rem;
  border-radius: 16px;
  background: rgba(var(--v-theme-surface-primary-050), 0.6);
  color: rgb(var(--v-theme-text-neutral-secondary));
  font-weight: 600;
  border: 1px dashed rgba(var(--v-theme-border-primary-strong), 0.2);
}

.product-hero__pricing-panel-more {
  margin: 0.25rem 0 0;
  font-size: 0.78rem;
  color: rgb(var(--v-theme-text-neutral-soft));
  text-align: center;
  display: inline-flex;
  align-items: center;
  align-self: center;
}

.product-hero__pricing-panel-alternatives {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  margin-top: 0.5rem;
  padding-top: 0.75rem;
  border-top: 1px dashed rgba(var(--v-theme-border-primary-strong), 0.25);
}

.product-hero__pricing-panel-alternatives-title {
  display: inline-flex;
  align-items: center;
  margin: 0 0 0.25rem;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: rgb(var(--v-theme-text-neutral-soft));
}

.product-hero__pricing-panel-alternatives-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.product-hero__pricing-panel-alternative {
  width: 100%;
}

.product-hero__pricing-panel-alternative-row {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  width: 100%;
  padding: 0.55rem 0.75rem;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.18);
  border-radius: 12px;
  background: rgba(var(--v-theme-surface-glass), 0.7);
  color: rgb(var(--v-theme-text-neutral-strong));
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.product-hero__pricing-panel-alternative-row:hover,
.product-hero__pricing-panel-alternative-row:focus-visible {
  background: rgba(var(--v-theme-surface-primary-080), 0.85);
  border-color: rgba(var(--v-theme-border-primary-strong), 0.55);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
  outline: none;
}

.product-hero__pricing-panel-alternative-favicon {
  border-radius: 6px;
  width: 24px;
  height: 24px;
  object-fit: cover;
  flex-shrink: 0;
}

.product-hero__pricing-panel-alternative-favicon-fallback {
  color: rgb(var(--v-theme-text-neutral-soft));
  flex-shrink: 0;
}

.product-hero__pricing-panel-alternative-name {
  font-weight: 600;
  font-size: 0.9rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
  flex: 1 1 auto;
}

.product-hero__pricing-panel-alternative-price {
  font-weight: 700;
  font-size: 0.95rem;
  color: rgb(var(--v-theme-text-neutral-strong));
  flex-shrink: 0;
}

.product-hero__pricing-panel-alternative-chevron {
  color: rgb(var(--v-theme-text-neutral-soft));
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.product-hero__pricing-panel-alternative-row:hover
  .product-hero__pricing-panel-alternative-chevron,
.product-hero__pricing-panel-alternative-row:focus-visible
  .product-hero__pricing-panel-alternative-chevron {
  transform: translateX(2px);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.alt-list-enter-active,
.alt-list-leave-active {
  transition:
    opacity 0.25s ease,
    transform 0.25s ease;
}

.alt-list-enter-from,
.alt-list-leave-to {
  opacity: 0;
  transform: translateY(-4px);
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
  background: rgba(var(--v-theme-primary), 0.18);
  color: rgb(var(--v-theme-text-neutral-strong));
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
  color: rgb(var(--v-theme-text-neutral-strong));
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
  /* No changes required anymore, keeping row layout for mobile */
}
</style>
