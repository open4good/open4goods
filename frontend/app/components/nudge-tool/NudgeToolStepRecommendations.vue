<template>
  <div class="nudge-step-recos">
    <!-- Header removed, moved to Wizard -->

    <div v-if="loading" class="py-4">
      <v-skeleton-loader type="image, article" class="mb-4" />
      <v-skeleton-loader type="image, article" />
    </div>
    <div v-else>
      <CategoryProductCardGrid
        v-if="hasProducts"
        :products="products"
        :popular-attributes="popularAttributes"
        variant="classic"
        size="big"
      />
      <p v-else class="nudge-step-recos__empty">
        {{ $t('nudge-tool.steps.recommendations.empty') }}
      </p>
    </div>

    <div v-if="categoryLink" class="nudge-step-recos__footnote-wrapper">
      <NuxtLink
        :to="categoryLink"
        :aria-label="categoryLinkLabel"
        class="nudge-step-recos__footnote"
      >
        {{
          $t('nudge-tool.steps.recommendations.total', {
            count: totalCount,
          })
        }}
        <v-icon icon="mdi-arrow-right" size="small" class="ms-1" />
      </NuxtLink>
    </div>
    <p v-else class="nudge-step-recos__footnote">
      {{
        $t('nudge-tool.steps.recommendations.total', {
          count: totalCount,
        })
      }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AttributeConfigDto, ProductDto } from '~~/shared/api-client'
import CategoryProductCardGrid from '~/components/category/products/CategoryProductCardGrid.vue'

const props = withDefaults(
  defineProps<{
    products?: ProductDto[]
    popularAttributes?: AttributeConfigDto[]
    totalCount?: number
    loading?: boolean
    categoryLink?: string | null
    categoryLinkLabel?: string
  }>(),
  {
    products: () => [],
    popularAttributes: () => [],
    totalCount: 0,
    loading: false,
    categoryLink: null,
    categoryLinkLabel: '',
  }
)

const products = computed(() => props.products)
const popularAttributes = computed(() => props.popularAttributes)
const totalCount = computed(() => props.totalCount)
const loading = computed(() => props.loading)
const categoryLink = computed(() => props.categoryLink)
const categoryLinkLabel = computed(() => props.categoryLinkLabel)

const hasProducts = computed(() => products.value.length > 0)
</script>

<style scoped lang="scss">
.nudge-step-recos {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 12px;
  }

  &__title {
    margin: 0;
    font-size: 1.5rem;
    font-weight: 700;
  }

  &__subtitle {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__empty {
    margin: 16px 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__footnote-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 12px;
  }

  &__footnote {
    color: rgb(var(--v-theme-text-neutral-secondary));
    text-decoration: none;
    font-size: 0.9rem;
    display: inline-flex;
    align-items: center;
    transition: color 0.2s;

    &:hover {
      color: rgb(var(--v-theme-primary));
      text-decoration: underline;
    }
  }
}
</style>
