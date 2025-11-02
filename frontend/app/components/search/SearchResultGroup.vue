<template>
  <section class="search-result-group">
    <header class="search-result-group__header">
      <div class="search-result-group__headline">
        <p v-if="eyebrow" class="search-result-group__eyebrow">{{ eyebrow }}</p>
        <h2 class="search-result-group__title">{{ title }}</h2>
      </div>
      <p v-if="countLabel" class="search-result-group__count">{{ countLabel }}</p>
    </header>

    <CategoryProductCardGrid
      class="search-result-group__grid"
      :products="products"
      :popular-attributes="popularAttributes"
      size="compact"
    />
  </section>
</template>

<script setup lang="ts">
import type { AttributeConfigDto, ProductDto } from '~~/shared/api-client'
import CategoryProductCardGrid from '~/components/category/products/CategoryProductCardGrid.vue'

withDefaults(
  defineProps<{
    title: string
    products: ProductDto[]
    countLabel?: string | null
    popularAttributes?: AttributeConfigDto[]
    eyebrow?: string | null
  }>(),
  {
    countLabel: null,
    popularAttributes: () => [],
    eyebrow: null,
  },
)
</script>

<style scoped lang="sass">
.search-result-group
  display: flex
  flex-direction: column
  gap: 1.5rem
  padding-block: 1.5rem
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.5)

  &:last-of-type
    border-bottom: none

  &__header
    display: flex
    flex-direction: column
    gap: 0.75rem

    @media (min-width: 960px)
      flex-direction: row
      align-items: baseline
      justify-content: space-between

  &__headline
    display: flex
    flex-direction: column
    gap: 0.25rem

  &__eyebrow
    margin: 0
    font-size: 0.875rem
    letter-spacing: 0.08em
    text-transform: uppercase
    color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

  &__title
    margin: 0
    font-size: clamp(1.5rem, 1.1rem + 0.9vw, 2rem)
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__count
    margin: 0
    font-size: 0.95rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__grid
    padding-bottom: 0.5rem
</style>
