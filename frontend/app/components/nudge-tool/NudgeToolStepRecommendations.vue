<template>
  <div class="nudge-step-recos">
    <div class="nudge-step-recos__header">
      <div>
        <h2 class="nudge-step-recos__title">{{ $t('nudge-tool.steps.recommendations.title') }}</h2>
        <p class="nudge-step-recos__subtitle">{{ $t('nudge-tool.steps.recommendations.subtitle') }}</p>
      </div>
    </div>

    <div v-if="loading" class="py-4">
      <v-skeleton-loader type="image, article" class="mb-4" />
      <v-skeleton-loader type="image, article" />
    </div>
    <div v-else>
      <CategoryProductCardGrid
        v-if="products.length"
        :products="products"
        :popular-attributes="popularAttributes"
        size="compact"
      />
      <p v-else class="nudge-step-recos__empty">{{ $t('nudge-tool.steps.recommendations.empty') }}</p>
    </div>

    <p class="nudge-step-recos__footnote">
      {{ $t('nudge-tool.steps.recommendations.total', { count: totalCount }) }}
    </p>
  </div>
</template>

<script setup lang="ts">
import type { AttributeConfigDto, ProductDto } from '~~/shared/api-client'

import CategoryProductCardGrid from '~/components/category/products/CategoryProductCardGrid.vue'

const props = defineProps<{
  products: ProductDto[]
  popularAttributes?: AttributeConfigDto[]
  totalCount: number
  loading: boolean
}>()

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

  &__footnote {
    margin-top: 12px;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }
}
</style>
