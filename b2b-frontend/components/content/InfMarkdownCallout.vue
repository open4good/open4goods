<script setup lang="ts">
defineProps<{
  title?: string
  tone?: 'info' | 'success' | 'warning' | 'error'
}>()
</script>

<template>
  <v-alert :type="tone || 'info'" variant="tonal" class="my-4">
    <template v-if="title" #title>{{ title }}</template>
    <slot />
  </v-alert>
</template>
