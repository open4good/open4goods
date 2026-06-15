<template>
  <header class="b2b-page-header">
    <v-breadcrumbs
      v-if="breadcrumbs?.length"
      :items="breadcrumbs"
      density="compact"
      class="pa-0 mb-3 text-medium-emphasis"
    />

    <div class="d-flex flex-column flex-md-row align-start align-md-center ga-4">
      <div class="flex-grow-1">
        <p v-if="eyebrow" class="text-overline text-primary mb-1">{{ eyebrow }}</p>
        <h1 class="text-h4 text-md-h3 font-weight-bold mb-2">{{ title }}</h1>
        <p v-if="subtitle" class="text-body-1 text-medium-emphasis mb-0 b2b-page-header__subtitle">
          {{ subtitle }}
        </p>
      </div>

      <div v-if="$slots.actions" class="d-flex flex-wrap ga-2 justify-start justify-md-end">
        <slot name="actions" />
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
  eyebrow?: string
  breadcrumbs?: Array<{ title: string; to?: string; disabled?: boolean }>
}>()
</script>

<style scoped>
.b2b-page-header {
  padding-block: 24px;
}

.b2b-page-header__subtitle {
  max-width: 780px;
}
</style>
