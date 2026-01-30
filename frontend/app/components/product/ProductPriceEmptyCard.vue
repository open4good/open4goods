<template>
  <div
    class="d-flex flex-column align-center justify-center text-center pa-6 bg-grey-lighten-5 rounded-lg mb-4"
    style="border: 1px dashed rgba(0, 0, 0, 0.08)"
  >
    <v-icon :icon="icon" color="grey-lighten-1" class="mb-2" />
    <span class="text-caption font-weight-bold text-uppercase text-grey ls-1">
      {{ label }}
    </span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  icon: string
  label: string
}>()
</script>
