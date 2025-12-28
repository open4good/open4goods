<template>
  <v-card
    class="product-tile-card"
    :class="{ 'product-tile-card--disabled': disabled }"
    rounded="xl"
    elevation="2"
    hover
    :to="productLink"
    :rel="linkRel"
  >
    <div class="product-tile-card__container">
      <!-- 1. Header: Title & Subtitle on full row -->
      <div class="product-tile-card__header">
        <h3 class="product-tile-card__title">{{ productTitle }}</h3>

        <div
          v-if="hasAttributes"
          class="product-tile-card__subtitle text-truncate"
        >
          {{ attributesText }}
        </div>
      </div>

      <!-- 2. Body: Image + Pricing/Actions -->
      <div class="product-tile-card__body">
        <div class="product-tile-card__media">
          <v-img
            :src="imageSrc"
            :alt="productTitle"
            aspect-ratio="4 / 3"
            class="product-tile-card__image"
            cover
          >
            <template #placeholder>
              <v-skeleton-loader type="image" class="h-100" />
            </template>
          </v-img>

          <div class="product-tile-card__score">
            <ImpactScore
              v-if="impactScore != null"
              :score="impactScore"
              :max="5"
              size="small"
            />
            <span v-else class="product-tile-card__score-fallback">
              {{ notRatedLabel }}
            </span>
          </div>
        </div>

        <div class="product-tile-card__content">
          <!-- New Badge Style: Simple Text -->
          <div
            v-if="offerBadges.length"
            class="product-tile-card__pricing"
            role="list"
          >
            <div
              v-for="badge in offerBadges"
              :key="badge.key"
              class="product-tile-card__simple-badge"
              :class="`product-tile-card__simple-badge--${badge.appearance}`"
              role="listitem"
            >
              <span class="product-tile-card__badge-label me-1">{{
                badge.label
              }}</span>
              <span class="product-tile-card__badge-price">{{
                badge.price
              }}</span>
            </div>
          </div>

          <div class="product-tile-card__meta mt-auto">
            <!-- Compare Button -->
            <v-btn
              variant="text"
              size="small"
              class="px-0 text-none"
              :color="isCompared ? 'primary' : undefined"
              @click.prevent.stop="toggleCompare"
            >
              <v-icon
                :icon="isCompared ? 'mdi-check' : 'mdi-plus'"
                start
                size="small"
              />
              {{
                isCompared
                  ? t('category.products.compare.added')
                  : t('category.products.compare.buttonLabel')
              }}
            </v-btn>

            <div class="product-tile-card__offers ms-auto">
              <v-icon icon="mdi-store" size="16" class="me-1" />
              <span>{{ offersCountLabel }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import { useProductCompareStore } from '~/stores/useProductCompareStore'
import { useI18n } from 'vue-i18n'

export type ProductTileAttribute = {
  key: string
  label: string
  value: string
  icon?: string | null
}

export type ProductTileOfferBadge = {
  key: string
  label: string
  price: string
  appearance: 'new' | 'occasion' | 'default'
}

const props = withDefaults(
  defineProps<{
    product: ProductDto
    productLink?: string
    imageSrc?: string
    attributes?: ProductTileAttribute[]
    impactScore?: number | null
    offerBadges?: ProductTileOfferBadge[]
    offersCountLabel: string
    untitledLabel: string
    notRatedLabel: string
    disabled?: boolean
    linkRel?: string
  }>(),
  {
    attributes: () => [],
    impactScore: null,
    offerBadges: () => [],
    imageSrc: undefined,
    productLink: undefined,
    disabled: false,
    linkRel: undefined,
  }
)

const { t } = useI18n()
const compareStore = useProductCompareStore()

const productTitle = computed(
  () =>
    props.product.identity?.bestName ??
    props.product.identity?.model ??
    props.product.identity?.brand ??
    (props.product.gtin ? `#${props.product.gtin}` : props.untitledLabel)
)

const hasAttributes = computed(() => props.attributes.length > 0)
const attributesText = computed(() => {
  return props.attributes.map(a => a.value).join(' · ')
})

const isCompared = computed(() => compareStore.hasProduct(props.product))

const toggleCompare = () => {
  compareStore.toggleProduct(props.product)
}
</script>

<style scoped lang="scss">
.product-tile-card {
  height: 100%;
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-surface-primary-080), 0.6),
    rgba(var(--v-theme-surface-glass), 0.85)
  );
  display: flex;
  flex-direction: column;
  text-decoration: none;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);

  &--disabled {
    filter: grayscale(1);
    opacity: 0.7;

    .product-tile-card__image :deep(img) {
      filter: grayscale(1);
    }
  }

  &__container {
    padding: 1rem;
    display: flex;
    flex-direction: column;
    height: 100%;
    gap: 0.75rem;
  }

  &__header {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  &__title {
    font-size: 1.1rem;
    line-height: 1.35;
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-strong));
    font-weight: 700;
  }

  &__subtitle {
    font-size: 0.85rem;
    color: rgb(var(--v-theme-text-neutral-secondary));
    opacity: 0.85;
  }

  &__body {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    flex: 1;

    @media (min-width: 600px) {
      flex-direction: row;
    }
  }

  &__media {
    position: relative;
    border-radius: 0.75rem;
    overflow: hidden;
    background: rgb(var(--v-theme-surface-default));
    flex-shrink: 0;

    @media (min-width: 600px) {
      width: 140px;
    }
  }

  &__image {
    min-height: 120px;

    :deep(img) {
      object-fit: contain;
      mix-blend-mode: multiply;
      background: #fff;
    }
  }

  &__score {
    position: absolute;
    left: 0.5rem;
    bottom: 0.5rem;
    display: flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.25rem 0.5rem;
    border-radius: 999px;
    background: rgba(var(--v-theme-surface-default), 0.9);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }

  &__score-fallback {
    font-size: 0.7rem;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__content {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    flex: 1;
    min-width: 0; /* flex fix */
  }

  /* Simple Badge Styles */
  &__pricing {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  &__simple-badge {
    display: flex;
    align-items: baseline;
    font-size: 0.95rem;

    &--new {
      .product-tile-card__badge-label {
        color: rgb(var(--v-theme-primary));
        font-weight: 600;
        text-transform: uppercase;
        font-size: 0.75rem;
      }
      .product-tile-card__badge-price {
        color: rgb(var(--v-theme-text-neutral-strong));
        font-weight: 700;
      }
    }

    &--occasion {
      .product-tile-card__badge-label {
        color: rgb(var(--v-theme-accent-supporting));
        font-weight: 600;
        text-transform: uppercase;
        font-size: 0.75rem;
      }
      .product-tile-card__badge-price {
        color: rgb(var(--v-theme-text-neutral-strong));
        font-weight: 700;
      }
    }

    &--default {
      .product-tile-card__badge-label {
        color: rgb(var(--v-theme-text-neutral-secondary));
      }
    }
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-top: auto;
    padding-top: 0.5rem;
  }

  &__offers {
    display: flex;
    align-items: center;
    font-size: 0.85rem;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }
}
</style>
