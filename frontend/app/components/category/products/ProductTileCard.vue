<template>
  <v-card
    class="product-tile-card"
    :class="[
      `product-tile-card--${layout}`,
      { 'product-tile-card--disabled': disabled },
    ]"
    rounded="xl"
    elevation="2"
    hover
    :to="productLink"
    :rel="linkRel"
  >
    <div class="product-tile-card__layout">
      <template v-if="layout === 'horizontal'">
        <div class="product-tile-card__media">
          <v-img
            :src="imageSrc"
            :alt="displayTitle"
            aspect-ratio="4 / 3"
            class="product-tile-card__image"
            cover
          >
            <template #placeholder>
              <v-skeleton-loader type="image" class="h-100" />
            </template>
          </v-img>

          <div class="product-tile-card__corner" role="presentation">
            <ImpactScore
              v-if="impactScore != null"
              :score="impactScore"
              :max="scoreMax"
              size="small"
              mode="badge"
              badge-layout="stacked"
              badge-variant="corner"
              flat
            />
            <span v-else class="product-tile-card__corner-fallback">
              {{ notRatedLabel }}
            </span>
          </div>
        </div>

        <div class="product-tile-card__content">
          <div class="product-tile-card__header">
            <div class="product-tile-card__header-top">
              <h3 class="product-tile-card__title text-truncate">
                {{ displayTitle }}
              </h3>
            </div>
            <div
              v-if="hasAttributes"
              class="product-tile-card__attributes"
              role="list"
            >
              <v-chip
                v-for="attribute in attributes"
                :key="attribute.key"
                class="product-tile-card__attribute"
                variant="tonal"
                size="small"
                color="surface-primary-080"
                role="listitem"
              >
                <v-icon
                  v-if="attribute.icon"
                  :icon="attribute.icon"
                  size="16"
                  class="me-1"
                />
                <span class="product-tile-card__attribute-value">{{
                  attribute.value
                }}</span>
              </v-chip>
            </div>
            <p class="product-tile-card__subtitle text-truncate">
              {{ subtitle }}
            </p>
          </div>

          <div v-if="offerBadges.length" class="product-tile-card__pricing-row">
            <v-img
              v-if="imageSrc"
              :src="imageSrc"
              :alt="displayTitle"
              width="120"
              height="120"
              class="product-tile-card__thumbnail"
              contain
            >
              <template #placeholder>
                <v-skeleton-loader type="image" class="h-100" />
              </template>
            </v-img>
            <div
              class="product-tile-card__pricing"
              :class="{
                'product-tile-card__pricing--stacked': offerBadges.length > 1,
              }"
              role="list"
            >
              <div
                v-for="badge in offerBadges"
                :key="badge.key"
                class="product-tile-card__price-badge"
                :class="`product-tile-card__price-badge--${badge.appearance}`"
                role="listitem"
              >
                <span class="product-tile-card__price-badge-label">{{
                  badge.label
                }}</span>
                <span class="product-tile-card__price-badge-price">{{
                  badge.price
                }}</span>
              </div>
            </div>
          </div>

          <v-row class="product-tile-card__meta">
            <div class="product-tile-card__offers">
              <v-icon icon="mdi-store" size="16" class="me-1" />
              <span>{{ offersCountLabel }}</span>
            </div>
            <CategoryProductCompareToggle
              class="product-tile-card__compare"
              :product="product"
              size="compact"
            />
          </v-row>
        </div>
      </template>

      <template v-else>
        <div
          class="product-tile-card__content product-tile-card__content--stacked"
        >
          <div class="product-tile-card__header">
            <div class="product-tile-card__header-top">
              <h3 class="product-tile-card__title text-truncate">
                {{ displayTitle }}
              </h3>
            </div>
            <div
              v-if="hasAttributes"
              class="product-tile-card__attributes"
              role="list"
            >
              <v-chip
                v-for="attribute in attributes"
                :key="attribute.key"
                class="product-tile-card__attribute"
                variant="tonal"
                size="x-small"
                color="surface-primary-080"
                role="listitem"
              >
                <v-icon
                  v-if="attribute.icon"
                  :icon="attribute.icon"
                  size="14"
                  class="me-1"
                />
                <span class="product-tile-card__attribute-value">{{
                  attribute.value
                }}</span>
              </v-chip>
            </div>
            <p class="product-tile-card__subtitle text-truncate">
              {{ subtitle }}
            </p>
          </div>

          <div
            class="product-tile-card__media product-tile-card__media--stacked"
          >
            <v-img
              :src="imageSrc"
              :alt="displayTitle"
              aspect-ratio="4 / 3"
              class="product-tile-card__image"
              contain
            >
              <template #placeholder>
                <v-skeleton-loader type="image" class="h-100" />
              </template>
            </v-img>

            <div class="product-tile-card__corner" role="presentation">
              <ImpactScore
                v-if="impactScore != null"
                :score="impactScore"
                :max="scoreMax"
                size="small"
                mode="badge"
                badge-layout="stacked"
                badge-variant="corner"
                flat
              />

              <span v-else class="product-tile-card__corner-fallback">
                {{ notRatedLabel }}
              </span>
            </div>
          </div>

          <div v-if="offerBadges.length" class="product-tile-card__pricing-row">
            <div
              class="product-tile-card__pricing"
              :class="{
                'product-tile-card__pricing--stacked': offerBadges.length > 1,
              }"
              role="list"
            >
              <div
                v-for="badge in offerBadges"
                :key="badge.key"
                class="product-tile-card__price-badge"
                :class="`product-tile-card__price-badge--${badge.appearance}`"
                role="listitem"
              >
                <span class="product-tile-card__price-badge-label">{{
                  badge.label
                }}</span>
                <span class="product-tile-card__price-badge-price">{{
                  badge.price
                }}</span>
              </div>
            </div>
          </div>

          <v-row
            class="product-tile-card__meta product-tile-card__meta--stacked"
          >
            <div class="product-tile-card__offers">
              <v-icon icon="mdi-store" size="16" class="me-1" />
              <span>{{ offersCountLabel }}</span>
            </div>
            <CategoryProductCompareToggle
              class="product-tile-card__compare"
              :product="product"
              size="compact"
            />
          </v-row>
        </div>
      </template>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import CategoryProductCompareToggle from '~/components/category/products/CategoryProductCompareToggle.vue'
import { resolveProductTitle } from '~/utils/_product-title-resolver'

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
    layout?: 'horizontal' | 'vertical'
    disabled?: boolean
    linkRel?: string
    scoreMax?: number
  }>(),
  {
    attributes: () => [],
    impactScore: null,
    offerBadges: () => [],
    imageSrc: undefined,
    productLink: undefined,
    layout: 'vertical',
    disabled: false,
    linkRel: undefined,
    scoreMax: 5,
  }
)

const subtitle = computed(
  () =>
    props.product.identity?.model ??
    props.product.identity?.bestName ??
    (props.product.gtin ? `#${props.product.gtin}` : props.untitledLabel)
)

const hasAttributes = computed(() => props.attributes.length > 0)

const displayTitle = computed(() => resolveProductTitle(props.product))
</script>

<style scoped lang="scss">
.product-tile-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  text-decoration: none;
  background-color: rgb(var(--v-theme-surface-default));
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3);

  &--disabled {
    filter: grayscale(1);
    opacity: 0.8;
  }

  &__layout {
    display: grid;
    grid-template-columns: 1fr;
    gap: 0.75rem;
    padding: 1rem;
    height: 100%;

    @media (min-width: 640px) {
      grid-template-columns: auto 1fr;
      align-items: stretch;
    }
  }

  &--vertical &__layout {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  &__media {
    position: relative;
    border-radius: 0.75rem;
    overflow: hidden;
    background: rgb(var(--v-theme-surface-glass));
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25);
    min-height: 0;
    max-height: 200px;
  }

  &__image {
    height: 100%;
    max-height: 100%;

    :deep(img) {
      object-fit: contain;
      mix-blend-mode: multiply;
      background: rgb(var(--v-theme-surface-default));
    }
  }

  &__corner {
    position: absolute;
    top: 0;
    left: 0;
    width: 64px;
    height: 64px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 0 0 54% 0;
    background: rgba(var(--v-theme-surface-glass-strong), 0.92);
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.45);
    color: rgb(var(--v-theme-text-neutral-strong));
    box-shadow: 0 12px 24px rgba(15, 23, 42, 0.14);
    backdrop-filter: blur(6px);
    z-index: 2;
    pointer-events: none;
  }

  &__corner-fallback {
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
    text-align: center;
    line-height: 1.1;
    transform: rotate(-12deg);
  }

  &__content {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    min-width: 0;
  }

  &__content--stacked {
    gap: 0.75rem;
  }

  &__header {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
  }

  &__header-top {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.5rem;
    min-width: 0;
  }

  &__title {
    margin: 0;
    font-size: 1.05rem;
    line-height: 1.35;
    color: rgb(var(--v-theme-text-neutral-strong));
    font-weight: 700;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__subtitle {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
    font-size: 0.9rem;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__attributes {
    display: flex;
    flex-wrap: wrap;
    gap: 0.4rem;
    justify-content: flex-end;
    flex: 1;
  }

  &--vertical &__attributes {
    justify-content: flex-start;
  }

  &__attribute {
    font-weight: 600;
    color: rgb(var(--v-theme-text-neutral-strong));
  }

  &__attribute-value {
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
  }

  &__pricing-row {
    display: grid;
    grid-template-columns: auto 1fr;
    gap: 0.6rem;
    align-items: center;
  }

  &--vertical &__pricing-row {
    grid-template-columns: 1fr;
  }

  &__thumbnail {
    border-radius: 0.65rem;
    overflow: hidden;
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
    background: rgb(var(--v-theme-surface-default));
  }

  &__pricing {
    display: grid;
    gap: 0.5rem;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  }

  &__pricing--stacked {
    grid-template-columns: 1fr;
  }

  &__price-badge {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.55rem 0.75rem;
    border-radius: 0.75rem;
    background: rgba(var(--v-theme-surface-primary-100), 0.75);
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
  }

  &__price-badge-label {
    color: rgb(var(--v-theme-text-neutral-secondary));
    font-size: 0.85rem;
    font-weight: 600;
  }

  &__price-badge-price {
    font-weight: 700;
    color: rgb(var(--v-theme-text-neutral-strong));
  }

  &__price-badge--new {
    background: rgba(var(--v-theme-primary), 0.08);
    border-color: rgba(var(--v-theme-primary), 0.25);
  }

  &__price-badge--occasion {
    background: rgba(var(--v-theme-accent-supporting), 0.08);
    border-color: rgba(var(--v-theme-accent-supporting), 0.25);
  }

  &__meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: auto;
    gap: 0.75rem;
    padding-top: 0.25rem;
  }

  &__meta--stacked {
    margin-top: auto;
  }

  &__media--stacked {
    max-height: 180px;
  }

  &__offers {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    color: rgb(var(--v-theme-text-neutral-secondary));
    font-weight: 600;
  }
}
</style>
