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
              {{ t('product.start_impact_text') }}
              <br class="d-lg-none" />
              {{ t('product.start_impact_subtext') }}
            </span>
            <a
              :href="partnersLink"
              class="product-sticky-banner__link"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ t('product.partners_link') }}
            </a>
          </p>
        </div>

        <div class="product-sticky-banner__right">
          <div class="product-sticky-banner__prices">
            <ProductPriceRows
              v-if="product"
              :product="product"
              variant="compact"
            />
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
import { useI18n } from 'vue-i18n'
import type { ProductDto } from '~~/shared/api-client'

const { t } = useI18n()

withDefaults(
  defineProps<{
    open?: boolean
    color?: string
    elevation?: string | number
    ariaLabel?: string
    offersCountLabel?: string
    partnersLink?: string
    product?: ProductDto
  }>(),
  {
    open: false,
    color: undefined,
    elevation: 8,
    ariaLabel: undefined,
    offersCountLabel: undefined,
    partnersLink: '/partners',
    product: undefined,
  }
)

defineEmits<{
  'scroll-to-offers': []
}>()
</script>

<style scoped>
.product-sticky-banner {
  position: fixed;
  top: 64px;
  inset-inline: 0;
  height: 44px;
  z-index: 29;
  padding: 0 1rem;
  background: rgb(var(--v-theme-surface));
  color: rgb(var(--v-theme-on-surface));
  box-shadow: 0 4px 12px rgba(var(--v-theme-shadow-primary-600), 0.08);
  display: flex;
  align-items: center;
  border-bottom: 1px solid rgba(var(--v-theme-on-surface), 0.08);
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
  font-size: 0.825rem;
  line-height: 1.2;
  color: rgba(var(--v-theme-on-surface), 0.85);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.product-sticky-banner__icon {
  font-size: 1rem;
  margin-right: 0.25rem;
}

.product-sticky-banner__link {
  color: rgb(var(--v-theme-primary));
  text-decoration: underline;
  font-weight: 500;
  margin-left: 0.25rem;
}

.product-sticky-banner__link:hover {
  color: rgb(var(--v-theme-primary-darken-1));
}

.product-sticky-banner__right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.product-sticky-banner__prices {
  display: flex;
  gap: 0.5rem;
}

.product-sticky-banner__count-btn {
  color: rgba(var(--v-theme-on-surface), 0.7);
  text-transform: none;
  font-weight: 500;
  letter-spacing: normal;
  height: 28px !important;
}

.product-sticky-banner__count-btn:hover {
  color: rgb(var(--v-theme-on-surface));
}
</style>
