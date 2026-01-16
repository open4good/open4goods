<template>
  <v-slide-y-transition>
    <v-sheet
      v-if="open"
      class="product-sticky-banner d-none d-md-flex"
      :color="color"
      :elevation="elevation"
      rounded="0"
      border="none"
      role="region"
      :aria-label="ariaLabel"
    >
      <div class="product-sticky-banner__content">
        <div class="product-sticky-banner__left">
          <p class="product-sticky-banner__text">
            <span class="product-sticky-banner__icon">❤️</span>
            <span class="product-sticky-banner__message">
              {{ $t('product.start_impact_text') }}
              <br class="d-lg-none" />
              {{ $t('product.start_impact_subtext') }}
            </span>
            <a
              :href="partnersLink"
              class="product-sticky-banner__link"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ $t('product.partners_link') }}
            </a>
          </p>
        </div>

        <div class="product-sticky-banner__right">
          <div class="product-sticky-banner__prices">
            <v-btn
              v-if="newPriceLabel"
              color="primary"
              variant="elevated"
              size="small"
              class="product-sticky-banner__price-btn"
              :href="newPriceLink"
              target="_blank"
              rel="noopener noreferrer"
              @click="$emit('offer-click', 'new')"
            >
              {{ newPriceLabel }}
            </v-btn>
            <v-btn
              v-if="usedPriceLabel"
              color="secondary"
              variant="elevated"
              size="small"
              class="product-sticky-banner__price-btn"
              :href="usedPriceLink"
              target="_blank"
              rel="noopener noreferrer"
              @click="$emit('offer-click', 'used')"
            >
              {{ usedPriceLabel }}
            </v-btn>
          </div>

          <v-btn
            v-if="offersCountLabel"
            variant="text"
            size="small"
            class="product-sticky-banner__count-btn"
            @click="$emit('scroll-to-offers')"
          >
            {{ offersCountLabel }}
          </v-btn>
        </div>
      </div>
    </v-sheet>
  </v-slide-y-transition>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    open?: boolean
    color?: string
    elevation?: string | number
    ariaLabel?: string
    newPriceLabel?: string | null
    newPriceLink?: string | null
    usedPriceLabel?: string | null
    usedPriceLink?: string | null
    offersCountLabel?: string | null
    partnersLink?: string
  }>(),
  {
    open: false,
    color: 'surface-primary-080',
    elevation: 8,
    ariaLabel: undefined,
    newPriceLabel: null,
    newPriceLink: null,
    usedPriceLabel: null,
    usedPriceLink: null,
    offersCountLabel: null,
    partnersLink: '/partners',
  }
)

defineEmits<{
  'offer-click': [type: 'new' | 'used']
  'scroll-to-offers': []
}>()
</script>

<style scoped>
.product-sticky-banner {
  position: fixed;
  top: 64px;
  inset-inline: 0;
  height: 80px;
  z-index: 29;
  padding: 0 1.5rem;
  background: rgba(var(--v-theme-hero-gradient-mid), 0.98);
  color: rgb(var(--v-theme-text-on-accent));
  box-shadow: 0 4px 12px rgba(var(--v-theme-shadow-primary-600), 0.15);
  display: flex;
  align-items: center;
}

.product-sticky-banner__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  gap: 1rem;
}

.product-sticky-banner__left {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
}

.product-sticky-banner__text {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.3;
  color: rgba(var(--v-theme-text-on-accent), 0.95);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.product-sticky-banner__icon {
  font-size: 1.2rem;
  margin-right: 0.25rem;
}

.product-sticky-banner__link {
  color: rgb(var(--v-theme-primary-lighten-1));
  text-decoration: underline;
  font-weight: 600;
  margin-left: 0.25rem;
}

.product-sticky-banner__link:hover {
  color: rgb(var(--v-theme-primary-lighten-2));
}

.product-sticky-banner__right {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-shrink: 0;
}

.product-sticky-banner__prices {
  display: flex;
  gap: 0.5rem;
}

.product-sticky-banner__price-btn {
  font-weight: 700;
  text-transform: none;
  min-width: 100px;
}

.product-sticky-banner__count-btn {
  color: rgba(var(--v-theme-text-on-accent), 0.9);
  text-transform: none;
  font-weight: 600;
  letter-spacing: normal;
}

.product-sticky-banner__count-btn:hover {
  color: rgb(var(--v-theme-text-on-accent));
}
</style>
