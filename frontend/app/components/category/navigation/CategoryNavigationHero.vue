<template>
  <section
    class="category-navigation-hero"
    :aria-labelledby="titleId"
    :style="heroBackgroundStyle"
  >
    <v-container class="category-navigation-hero__container px-4" max-width="xl">
      <div class="category-navigation-hero__content">
        <div class="category-navigation-hero__copy">
          <CategoryNavigationBreadcrumbs
            v-if="breadcrumbs && breadcrumbs.length"
            class="mb-4"
            v-bind="{ items: breadcrumbs, ariaLabel: breadcrumbAriaLabel }"
          />
          <h1 :id="titleId" class="text-h3 text-sm-h2 font-weight-bold mb-3">
            {{ title }}
          </h1>
          <p v-if="description" class="text-body-1 text-lg-h6 mb-4">
            {{ description }}
          </p>
          <p
            v-if="resultSummary"
            class="text-body-2 text-neutral-soft mb-0"
            aria-live="polite"
          >
            {{ resultSummary }}
          </p>
        </div>

        <div class="category-navigation-hero__search">
          <v-text-field
            :model-value="modelValue"
            :label="searchLabel"
            :placeholder="searchPlaceholder"
            prepend-inner-icon="mdi-magnify"
            variant="solo"
            density="compact"
            color="primary"
            class="category-navigation-hero__search-field"
            @update:model-value="onUpdateModelValue"
          />
        </div>
      </div>
    </v-container>
  </section>
</template>

<script setup lang="ts">
import { computed, useId } from 'vue'

import CategoryNavigationBreadcrumbs from './CategoryNavigationBreadcrumbs.vue'
import { useThemedAsset } from '~~/app/composables/useThemedAsset'

interface BreadcrumbItem {
  title: string
  link?: string
}

const titleId = useId()

defineProps<{
  title: string
  description?: string
  breadcrumbs?: BreadcrumbItem[]
  modelValue: string
  searchLabel: string
  searchPlaceholder: string
  resultSummary?: string
  breadcrumbAriaLabel: string
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
}>()

const onUpdateModelValue = (value: string) => {
  emit('update:modelValue', value)
}

const textureGrid = useThemedAsset('backgrounds/texture-grid.svg')

const heroBackgroundStyle = computed(() => ({
  '--category-hero-texture': textureGrid.value ? `url('${textureGrid.value}')` : 'none',
}))
</script>

<style scoped>
.category-navigation-hero {
  --category-hero-texture: none;
  background: linear-gradient(
      135deg,
      rgba(var(--v-theme-hero-gradient-start), 0.95),
      rgba(var(--v-theme-hero-gradient-end), 0.85)
    ),
    var(--category-hero-texture);
  color: rgb(var(--v-theme-hero-overlay-strong));
}


.category-navigation-hero__container {
  padding-block: 2.75rem;
}

.category-navigation-hero__content {
  display: grid;
  gap: 1.75rem;
  align-items: center;
}

.category-navigation-hero__copy :deep(.category-navigation-breadcrumbs) {
  color: rgb(var(--v-theme-hero-overlay-strong));
}

.category-navigation-hero__copy :deep(.category-navigation-breadcrumbs__current) {
  color: rgb(var(--v-theme-hero-overlay-strong));
}

.category-navigation-hero__copy :deep(.category-navigation-breadcrumbs__separator) {
  color: rgba(var(--v-theme-hero-overlay-strong), 0.72);
}

.category-navigation-hero__copy :deep(.category-navigation-breadcrumbs__link--interactive:hover),
.category-navigation-hero__copy :deep(.category-navigation-breadcrumbs__link--interactive:focus-visible) {
  color: rgb(var(--v-theme-hero-overlay-strong));
  text-decoration: underline;
}

.category-navigation-hero__copy :deep(.category-navigation-breadcrumbs__link--interactive:focus-visible) {
  outline: 2px solid rgba(var(--v-theme-hero-overlay-strong), 0.8);
  outline-offset: 2px;
  border-radius: 0.25rem;
}

@media (min-width: 960px) {
  .category-navigation-hero__container {
    padding-block: 3.5rem;
  }

  .category-navigation-hero__content {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 2.5rem;
  }
}

.category-navigation-hero__copy {
  max-width: min(60ch, 100%);
}

@media (min-width: 960px) {
  .category-navigation-hero__copy {
    max-width: clamp(52ch, 60vw, 72ch);
  }
}

.category-navigation-hero__search {
  display: flex;
  justify-content: flex-start;
}

.category-navigation-hero__search-field {
  width: 100%;
  max-width: 420px;
  --v-field-border-opacity: 0;
  --v-field-background: rgba(255, 255, 255, 0.14);
  --v-input-control-height: 52px;
  backdrop-filter: blur(12px);
  border-radius: 1rem;
}

@media (min-width: 960px) {
  .category-navigation-hero__search {
    justify-content: flex-end;
  }
}
</style>
