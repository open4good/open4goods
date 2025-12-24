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
    <div class="product-tile-card__wrapper">
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

        <div class="product-tile-card__compare">
          <CategoryProductCompareToggle :product="product" size="compact" />
        </div>

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
        <h3 class="product-tile-card__title">{{ productTitle }}</h3>

        <div
          v-if="hasAttributes"
          class="product-tile-card__attributes"
          role="list"
        >
          <v-chip
            v-for="attribute in attributes"
            :key="attribute.key"
            size="small"
            class="product-tile-card__attribute"
            variant="tonal"
            color="surface-primary-080"
            density="comfortable"
            role="listitem"
          >
            <v-icon
              v-if="attribute.icon"
              :icon="attribute.icon"
              size="16"
              class="me-1"
            />
            <span class="text-truncate">{{ attribute.value }}</span>
          </v-chip>
        </div>

        <div
          v-if="offerBadges.length"
          class="product-tile-card__pricing"
          role="list"
        >
          <div
            v-for="badge in offerBadges"
            :key="badge.key"
            class="product-tile-card__price-badge"
            :class="`product-tile-card__price-badge--${badge.appearance}`"
            role="listitem"
          >
            <div class="product-tile-card__price-label">{{ badge.label }}</div>
            <div class="product-tile-card__price">{{ badge.price }}</div>
          </div>
        </div>

        <div class="product-tile-card__meta">
          <div class="product-tile-card__offers">
            <v-icon icon="mdi-store" size="18" class="me-1" />
            <span>{{ offersCountLabel }}</span>
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
import CategoryProductCompareToggle from './CategoryProductCompareToggle.vue'

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

const productTitle = computed(
  () =>
    props.product.identity?.bestName ??
    props.product.identity?.model ??
    props.product.identity?.brand ??
    (props.product.gtin ? `#${props.product.gtin}` : props.untitledLabel)
)

const hasAttributes = computed(() => props.attributes.length > 0)
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
  text-decoration: none;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);

  &--disabled {
    filter: grayscale(1);
    opacity: 0.7;

    .product-tile-card__image :deep(img) {
      filter: grayscale(1);
    }
  }

  &__wrapper {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  &__media {
    position: relative;
    border-radius: 0.75rem;
    overflow: hidden;
    background: rgb(var(--v-theme-surface-default));
  }

  &__image {
    min-height: 160px;

    :deep(img) {
      object-fit: contain;
      mix-blend-mode: multiply;
      background: #fff;
    }
  }

  &__compare {
    position: absolute;
    top: 0.5rem;
    right: 0.5rem;
    z-index: 2;
  }

  &__score {
    position: absolute;
    left: 0.75rem;
    bottom: 0.75rem;
    display: flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.35rem 0.6rem;
    border-radius: 999px;
    background: rgba(var(--v-theme-surface-default), 0.9);
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  }

  &__score-fallback {
    font-size: 0.75rem;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__content {
    display: flex;
    flex-direction: column;
    gap: 0.65rem;
    padding: 0 0.35rem 0.85rem;
  }

  &__title {
    font-size: 1.05rem;
    line-height: 1.35;
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-strong));
    font-weight: 700;
  }

  &__attributes {
    display: flex;
    gap: 0.5rem;
    overflow-x: auto;
    padding-bottom: 0.25rem;

    &::-webkit-scrollbar {
      height: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(var(--v-theme-primary), 0.35);
      border-radius: 999px;
    }
  }

  &__attribute {
    border-radius: 999px;
    text-transform: none;
    font-weight: 600;
    color: rgb(var(--v-theme-text-neutral-strong));
    letter-spacing: 0.01em;
  }

  &__pricing {
    display: flex;
    flex-wrap: wrap;
    gap: 0.6rem;
  }

  &__price-badge {
    flex: 1 1 160px;
    border-radius: 0.75rem;
    padding: 0.6rem 0.75rem;
    background: rgba(var(--v-theme-surface-default), 0.85);
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.5);
    min-width: 0;

    &--new {
      border-color: rgba(var(--v-theme-primary), 0.55);
      background: rgba(var(--v-theme-primary), 0.08);
      color: rgb(var(--v-theme-primary));
    }

    &--occasion {
      border-color: rgba(var(--v-theme-accent-supporting), 0.55);
      background: rgba(var(--v-theme-accent-supporting), 0.08);
      color: rgb(var(--v-theme-accent-supporting));
    }

    &--default {
      color: rgb(var(--v-theme-text-neutral-strong));
    }
  }

  &__price-label {
    font-size: 0.75rem;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    font-weight: 700;
    margin-bottom: 0.1rem;
  }

  &__price {
    font-size: 1.1rem;
    font-weight: 800;
    line-height: 1.2;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex-wrap: wrap;
    color: rgb(var(--v-theme-text-neutral-secondary));
    font-size: 0.9rem;
  }

  &__offers {
    display: inline-flex;
    align-items: center;
  }

  @media (min-width: 960px) {
    &__wrapper {
      flex-direction: row;
      gap: 1rem;
    }

    &__media {
      flex: 0 0 220px;
    }

    &__content {
      padding: 0.5rem 0.5rem 0.85rem;
    }
  }
}
</style>
