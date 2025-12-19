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
        size="compact"
      />
      <p v-else class="nudge-step-recos__empty">
        {{ $t('nudge-tool.steps.recommendations.empty') }}
      </p>
    </div>

    <p class="nudge-step-recos__footnote">
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
  }>(),
  {
    products: () => [],
    popularAttributes: () => [],
    totalCount: 0,
    loading: false,
  }
)

const products = computed(() => props.products)
const popularAttributes = computed(() => props.popularAttributes)
const totalCount = computed(() => props.totalCount)
const loading = computed(() => props.loading)

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

  &__footnote {
    margin-top: 12px;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }
}
</style>
