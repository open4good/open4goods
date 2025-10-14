<template>
  <v-container>
    <v-skeleton-loader
      v-if="pending"
      type="image"
      height="100"
    />
    <The-slide
      v-else-if="categoryImages.length > 0"
      :items="categoryImages"
      :height="100"
      :width="100"
    />
  </v-container>
</template>

<script setup lang="ts">
import { useCategories } from "~/composables/categories/useCategories";
import type { VerticalConfigDto } from "~~/shared/api-client";

const { categories, fetchCategories } = useCategories();

const { pending } = await useAsyncData(
  "home-categories-slide",
  () => fetchCategories(true), // Only enabled categories
  { server: true, immediate: true }
);

// Transform categories to image URLs for The-slide component
const categoryImages = computed(() => {
  return categories.value
    .filter((category: VerticalConfigDto): category is VerticalConfigDto & {
      imageSmall: string;
      verticalHomeTitle: string;
      verticalHomeUrl: string;
    } =>
      Boolean(category.imageSmall) &&
      Boolean(category.verticalHomeTitle) &&
      Boolean(category.verticalHomeUrl))
    .map(
      (
        category: VerticalConfigDto & {
          imageSmall: string;
          verticalHomeTitle: string;
          verticalHomeUrl: string;
        }
      ) => {
        const sanitizedSlug = category.verticalHomeUrl.replace(/^\/+/, "");

        return {
          imageSmall: category.imageSmall,
          verticalHomeTitle: category.verticalHomeTitle,
          href: `/${sanitizedSlug}`
        };
      }
    );
});
</script>
