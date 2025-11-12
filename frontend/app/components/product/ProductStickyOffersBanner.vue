<template>
  <Teleport to="body">
    <transition name="product-sticky-banner-fade">
      <div
        v-if="shouldDisplay"
        class="product-sticky-banner"
        :class="{ 'product-sticky-banner--mobile': isMobile }"
        :style="bannerStyle"
        role="region"
        :aria-label="ariaLabel"
      >
        <div class="product-sticky-banner__inner">
          <div class="product-sticky-banner__intro">
            <v-icon icon="mdi-heart-outline" size="24" class="product-sticky-banner__intro-icon" />
            <p class="product-sticky-banner__message">
              {{ supportMessage }}
            </p>
          </div>

          <div v-if="isMobile" class="product-sticky-banner__panels">
            <v-expansion-panels variant="accordion" class="product-sticky-banner__accordion">
              <v-expansion-panel v-if="hasOccasion" value="occasion">
                <v-expansion-panel-title expand-icon="mdi-chevron-down">
                  <div class="product-sticky-banner__panel-title">
                    <span>{{ occasionTitle }}</span>
                    <span v-if="occasionSelection" class="product-sticky-banner__panel-subtitle">
                      {{ occasionSelection.label }}
                    </span>
                  </div>
                </v-expansion-panel-title>
                <v-expansion-panel-text>
                  <div class="product-sticky-banner__segment">
                    <ProductStickyOffersBannerSelect
                      v-model="occasionSelection"
                      :items="occasionItems"
                      :placeholder="selectPlaceholder"
                      :aria-label="occasionSelectAria"
                    />
                    <v-btn
                      color="primary"
                      block
                      variant="flat"
                      class="product-sticky-banner__cta"
                      :disabled="!occasionSelection"
                      :href="occasionLink?.isExternal ? occasionLink.url : undefined"
                      :to="!occasionLink?.isExternal ? occasionLink?.url : undefined"
                      :prefetch="false"
                    >
                      {{ ctaLabel }}
                    </v-btn>
                  </div>
                </v-expansion-panel-text>
              </v-expansion-panel>

              <v-expansion-panel v-if="hasNew" value="new">
                <v-expansion-panel-title expand-icon="mdi-chevron-down">
                  <div class="product-sticky-banner__panel-title">
                    <span>{{ newTitle }}</span>
                    <span v-if="newSelection" class="product-sticky-banner__panel-subtitle">
                      {{ newSelection.label }}
                    </span>
                  </div>
                </v-expansion-panel-title>
                <v-expansion-panel-text>
                  <div class="product-sticky-banner__segment">
                    <ProductStickyOffersBannerSelect
                      v-model="newSelection"
                      :items="newItems"
                      :placeholder="selectPlaceholder"
                      :aria-label="newSelectAria"
                    />
                    <v-btn
                      color="primary"
                      block
                      variant="flat"
                      class="product-sticky-banner__cta"
                      :disabled="!newSelection"
                      :href="newLink?.isExternal ? newLink.url : undefined"
                      :to="!newLink?.isExternal ? newLink?.url : undefined"
                      :prefetch="false"
                    >
                      {{ ctaLabel }}
                    </v-btn>
                  </div>
                </v-expansion-panel-text>
              </v-expansion-panel>
            </v-expansion-panels>
          </div>

          <div v-else class="product-sticky-banner__segments">
            <div v-if="hasOccasion" class="product-sticky-banner__segment">
              <span class="product-sticky-banner__segment-title">{{ occasionTitle }}</span>
              <ProductStickyOffersBannerSelect
                v-model="occasionSelection"
                :items="occasionItems"
                :placeholder="selectPlaceholder"
                :aria-label="occasionSelectAria"
              />
              <v-btn
                color="primary"
                variant="flat"
                class="product-sticky-banner__cta"
                :disabled="!occasionSelection"
                :href="occasionLink?.isExternal ? occasionLink.url : undefined"
                :to="!occasionLink?.isExternal ? occasionLink?.url : undefined"
                :prefetch="false"
              >
                {{ ctaLabel }}
              </v-btn>
            </div>

            <v-divider v-if="hasOccasion && hasNew" vertical class="product-sticky-banner__divider" />

            <div v-if="hasNew" class="product-sticky-banner__segment">
              <span class="product-sticky-banner__segment-title">{{ newTitle }}</span>
              <ProductStickyOffersBannerSelect
                v-model="newSelection"
                :items="newItems"
                :placeholder="selectPlaceholder"
                :aria-label="newSelectAria"
              />
              <v-btn
                color="primary"
                variant="flat"
                class="product-sticky-banner__cta"
                :disabled="!newSelection"
                :href="newLink?.isExternal ? newLink.url : undefined"
                :to="!newLink?.isExternal ? newLink?.url : undefined"
                :prefetch="false"
              >
                {{ ctaLabel }}
              </v-btn>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDisplay } from 'vuetify'
import type { ProductDto } from '~~/shared/api-client'
import ProductStickyOffersBannerSelect from './ProductStickyOffersBannerSelect.vue'
import { STICKY_VIEWPORT_OFFSET } from './productHeroSticky.constants'

type ProductOffers = NonNullable<ProductDto['offers']>
type OfferItem =
  | NonNullable<ProductOffers['occasionOffers']>[number]
  | NonNullable<ProductOffers['newOffers']>[number]

type OfferCondition = 'occasion' | 'new'

interface OfferSelectItem {
  key: string
  label: string
  merchant: string
  priceLabel: string
  favicon: string | null
  link: { url: string; isExternal: boolean } | null
  offer: OfferItem
}

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
  heroExitedViewport: {
    type: Boolean,
    default: false,
  },
})

const { t, n } = useI18n()
const display = useDisplay()

const isMobile = computed(() => display.smAndDown.value)

const supportMessage = computed(() => t('product.hero.supportBanner.message'))
const selectPlaceholder = computed(() => t('product.hero.supportBanner.selectPlaceholder'))
const ctaLabel = computed(() => t('product.hero.supportBanner.action'))
const occasionTitle = computed(() => t('product.hero.supportBanner.segment.occasion.title'))
const newTitle = computed(() => t('product.hero.supportBanner.segment.new.title'))
const occasionSelectAria = computed(() => t('product.hero.supportBanner.segment.occasion.selectAria'))
const newSelectAria = computed(() => t('product.hero.supportBanner.segment.new.selectAria'))
const ariaLabel = computed(() => t('product.hero.supportBanner.ariaLabel'))

const offers = computed(() => props.product.offers ?? null)

const buildOfferLink = (offer: OfferItem) => {
  const affiliationToken = (offer as { affiliationToken?: string | null }).affiliationToken
  if (affiliationToken) {
    return { url: `/contrib/${affiliationToken}`, isExternal: false }
  }

  const url = (offer as { url?: string | null }).url
  if (url) {
    return { url, isExternal: !url.startsWith('/') }
  }

  return null
}

const formatPrice = (price: number, currency?: string | null) => {
  const resolvedCurrency = currency ?? offers.value?.bestPrice?.currency ?? 'EUR'

  if (resolvedCurrency === 'EUR') {
    return n(price, {
      style: 'decimal',
      minimumFractionDigits: price >= 100 ? 0 : 2,
      maximumFractionDigits: price >= 100 ? 0 : 2,
    })
  }

  return n(price, {
    style: 'currency',
    currency: resolvedCurrency,
    maximumFractionDigits: price >= 100 ? 0 : 2,
  })
}

const mapOffersToItems = (condition: OfferCondition) => {
  const conditionOffers =
    condition === 'occasion'
      ? offers.value?.occasionOffers ?? []
      : offers.value?.newOffers ?? []

  return [...conditionOffers]
    .filter((offer): offer is OfferItem => typeof offer?.price === 'number' && Number.isFinite(offer.price))
    .sort((a, b) => (a.price ?? Number.POSITIVE_INFINITY) - (b.price ?? Number.POSITIVE_INFINITY))
    .map((offer, index): OfferSelectItem => {
      const merchant = offer.datasourceName ?? t('product.price.metrics.unknownSource')
      const priceLabel = formatPrice(offer.price as number, offer.currency)
      const label = `${merchant} — ${priceLabel}`
      const favicon = offer.favicon ?? null
      const link = buildOfferLink(offer)

      return {
        key: `${condition}-${offer.affiliationToken ?? offer.url ?? offer.datasourceName ?? index}`,
        label,
        merchant,
        priceLabel,
        favicon,
        link,
        offer,
      }
    })
}

const occasionItems = computed<OfferSelectItem[]>(() => mapOffersToItems('occasion'))
const newItems = computed<OfferSelectItem[]>(() => mapOffersToItems('new'))

const hasOccasion = computed(() => occasionItems.value.length > 0)
const hasNew = computed(() => newItems.value.length > 0)

const shouldDisplay = computed(
  () => props.heroExitedViewport && (hasOccasion.value || hasNew.value),
)

const occasionSelection = ref<OfferSelectItem | null>(null)
const newSelection = ref<OfferSelectItem | null>(null)

watch(
  occasionItems,
  (items) => {
    occasionSelection.value = items[0] ?? null
  },
  { immediate: true },
)

watch(
  newItems,
  (items) => {
    newSelection.value = items[0] ?? null
  },
  { immediate: true },
)

const occasionLink = computed(() => occasionSelection.value?.link ?? null)
const newLink = computed(() => newSelection.value?.link ?? null)

const bannerStyle = computed(() => ({
  top: `${STICKY_VIEWPORT_OFFSET}px`,
}))
</script>

<style scoped>
.product-sticky-banner {
  position: fixed;
  inset-inline: 0;
  z-index: 50;
  display: flex;
  justify-content: center;
  padding: 0 1rem;
  pointer-events: none;
}

.product-sticky-banner__inner {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 0.75rem 1.5rem;
  border-radius: 999px;
  background: rgba(var(--v-theme-surface-glass-strong), 0.96);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.2);
  max-width: 1200px;
  width: 100%;
  pointer-events: auto;
}

.product-sticky-banner__intro {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1 1 auto;
  min-width: 0;
}

.product-sticky-banner__intro-icon {
  color: rgb(var(--v-theme-primary));
}

.product-sticky-banner__message {
  margin: 0;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-sticky-banner__segments {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.product-sticky-banner__segment {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 220px;
}

.product-sticky-banner__segment-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-secondary));
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.product-sticky-banner__cta {
  font-weight: 600;
}

.product-sticky-banner__divider {
  align-self: stretch;
}

.product-sticky-banner__panels {
  flex: 1 1 auto;
  width: 100%;
}

.product-sticky-banner__accordion {
  background: transparent;
}

.product-sticky-banner__panel-title {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.35rem;
}

.product-sticky-banner__panel-subtitle {
  font-size: 0.85rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-sticky-banner--mobile .product-sticky-banner__inner {
  border-radius: 24px;
  flex-direction: column;
  align-items: stretch;
  padding: 0.75rem 1rem;
  gap: 1rem;
}

.product-sticky-banner--mobile .product-sticky-banner__intro {
  justify-content: center;
  text-align: center;
}

.product-sticky-banner-fade-enter-active,
.product-sticky-banner-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.product-sticky-banner-fade-enter-from,
.product-sticky-banner-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 599px) {
  .product-sticky-banner {
    padding: 0 0.75rem;
  }

  .product-sticky-banner__segment {
    min-width: 0;
  }
}
</style>
