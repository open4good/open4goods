<template>
  <section class="search-result-group">
    <header class="search-result-group__header">
      <div class="search-result-group__headline">
        <h2 class="search-result-group__title">{{ title }}</h2>
        <p v-if="searchModeLabel" class="search-result-group__mode">
          {{ searchModeLabel }}
        </p>
        <NuxtLink
          v-if="verticalHomeUrl"
          :to="verticalHomeUrl"
          class="search-result-group__link"
          :aria-label="categoryLinkAria || undefined"
          data-test="search-result-group-link"
        >
          <span>{{ categoryLinkLabel }}</span>
          <v-icon icon="mdi-arrow-right" size="small" aria-hidden="true" />
        </NuxtLink>
      </div>
      <p v-if="countLabel" class="search-result-group__count">
        {{ countLabel }}
      </p>
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
    verticalHomeUrl?: string | null
    searchModeLabel?: string | null
    categoryLinkLabel?: string | null
    categoryLinkAria?: string | null
  }>(),
  {
    countLabel: null,
    popularAttributes: () => [],
    verticalHomeUrl: null,
    searchModeLabel: null,
    categoryLinkLabel: '',
    categoryLinkAria: null,
  }
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
    gap: 0.5rem

  &__title
    margin: 0
    font-size: clamp(1.5rem, 1.1rem + 0.9vw, 2rem)
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__mode
    margin: 0
    font-size: 0.9rem
    font-weight: 500
    color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

  &__link
    display: inline-flex
    align-items: center
    gap: 0.4rem
    align-self: flex-start
    padding: 0.25rem 0.6rem 0.25rem 0
    border-radius: 999px
    font-size: 0.95rem
    font-weight: 500
    color: rgba(var(--v-theme-accent-primary-highlight), 0.95)
    text-decoration: none
    transition: color 0.2s ease, transform 0.2s ease

    &:hover,
    &:focus-visible
      color: rgb(var(--v-theme-primary))
      transform: translateX(2px)

  &__count
    margin: 0
    font-size: 0.95rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__grid
    padding-bottom: 0.5rem
</style>
