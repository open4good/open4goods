<template>
  <v-container>
    <The-slide :items="categoryImages" :height="80" />
  </v-container>
</template>

<script setup lang="ts">
import { useCategories } from '~/composables/categories/useCategories'
import type { VerticalConfigDto } from '~~/shared/api-client'

const { categories, fetchCategories } = useCategories()

await useAsyncData(
  'home-categories-slide',
  () => fetchCategories(true), // Only enabled categories
  { server: true, immediate: true }
)

// Transform categories to image URLs for The-slide component
const categoryImages = computed(() => {
  return categories.value
    .filter((category: VerticalConfigDto): category is VerticalConfigDto & { imageSmall: string } =>
      Boolean(category.imageSmall)
    )
    .map((category: VerticalConfigDto & { imageSmall: string }) => category.imageSmall)
})
</script>