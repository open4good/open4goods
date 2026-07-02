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
          <v-chip
            v-if="impactScore != null"
            size="small"
            class="product-sticky-banner__score-chip"
            :color="impactScoreColor"
            variant="tonal"
            :prepend-icon="mdiLeaf"
          >
            {{ impactScore }}/20
          </v-chip>

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
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { mdiLeaf } from '@mdi/js'
import type { ProductDto } from '~~/shared/api-client'

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    open?: boolean
    color?: string
    elevation?: string | number
    ariaLabel?: string
    offersCountLabel?: string
    partnersLink?: string
    product?: ProductDto
    impactScore?: number | null
  }>(),
  {
    open: false,
    color: undefined,
    elevation: 8,
    ariaLabel: undefined,
    offersCountLabel: undefined,
    partnersLink: '/partners',
    product: undefined,
    impactScore: null,
  }
)

const impactScoreColor = computed(() => {
  const score = props.impactScore
  if (score == null) return 'grey'
  if (score >= 14) return 'success'
  if (score >= 9) return 'warning'
  return 'error'
})

defineEmits<{
  'scroll-to-offers': []
}>()
</script>

<style scoped>
.product-sticky-banner {
  position: fixed;
  top: 64px;
  inset-inline: 0;
  height: 52px;
  z-index: 29;
  padding: 0 1.5rem;
  background: rgba(var(--v-theme-surface-glass-strong), 0.9);
  backdrop-filter: blur(16px);
  color: rgb(var(--v-theme-text-neutral-strong));
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.12);
  display: flex;
  align-items: center;
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.15);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
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
  font-size: 0.875rem;
  line-height: 1.4;
  color: rgb(var(--v-theme-text-neutral-secondary));
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  font-weight: 500;
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

.product-sticky-banner__score-chip {
  font-weight: 700;
  font-size: 0.75rem;
  flex-shrink: 0;
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
