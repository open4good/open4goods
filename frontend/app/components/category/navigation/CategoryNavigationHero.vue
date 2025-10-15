<template>
  <section
    class="category-navigation-hero"
    :aria-labelledby="titleId"
  >
    <v-container class="py-12 px-4" max-width="xl">
      <div class="category-navigation-hero__content">
        <div class="category-navigation-hero__copy">
          <p v-if="eyebrow" class="text-overline mb-2 text-hero-pill-on-dark">
            {{ eyebrow }}
          </p>
          <CategoryNavigationBreadcrumbs
            v-if="breadcrumbs && breadcrumbs.length"
            class="mb-4"
            v-bind="{ items: breadcrumbs, ariaLabel: breadcrumbAriaLabel }"
          />
          <h1 :id="titleId" class="text-h3 text-sm-h2 font-weight-bold mb-4">
            {{ title }}
          </h1>
          <p v-if="description" class="text-body-1 text-lg-h6 mb-6">
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
            density="comfortable"
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
import { useId } from 'vue'

import CategoryNavigationBreadcrumbs from './CategoryNavigationBreadcrumbs.vue'

interface BreadcrumbItem {
  title: string
  link?: string
}

const titleId = useId()

defineProps<{
  eyebrow?: string
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
</script>

<style scoped>
.category-navigation-hero {
  background: linear-gradient(
      135deg,
      rgba(var(--v-theme-hero-gradient-start), 0.95),
      rgba(var(--v-theme-hero-gradient-end), 0.85)
    ),
    url('/images/backgrounds/texture-grid.svg');
  color: rgb(var(--v-theme-hero-overlay-strong));
}

.category-navigation-hero__content {
  display: grid;
  gap: 2.5rem;
  align-items: center;
}

@media (min-width: 960px) {
  .category-navigation-hero__content {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 3rem;
  }
}

.category-navigation-hero__copy {
  max-width: 640px;
}

.category-navigation-hero__search {
  display: flex;
  justify-content: flex-end;
}

.category-navigation-hero__search-field {
  width: 100%;
  max-width: 420px;
  --v-field-border-opacity: 0;
  --v-field-background: rgba(255, 255, 255, 0.14);
  --v-input-control-height: 64px;
  backdrop-filter: blur(12px);
  border-radius: 1rem;
}

.text-hero-pill-on-dark {
  color: rgb(var(--v-theme-hero-pill-on-dark));
  letter-spacing: 0.12em;
}
</style>
