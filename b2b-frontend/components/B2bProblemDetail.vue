<template>
  <v-alert type="error" variant="tonal" class="b2b-problem-detail">
    <div class="font-weight-bold mb-1">{{ problem.title || fallbackTitle }}</div>
    <div v-if="problem.detail" class="text-body-2 mb-2">{{ problem.detail }}</div>
    <div class="d-flex flex-wrap ga-2 text-caption">
      <v-chip v-if="problem.status" size="x-small" color="error" variant="flat">{{ problem.status }}</v-chip>
      <span v-if="problem.type">{{ problem.type }}</span>
      <span v-if="problem.instance">{{ problem.instance }}</span>
    </div>
  </v-alert>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  problem: {
    type?: string
    title?: string
    status?: number
    detail?: string
    instance?: string
  }
  fallbackTitle?: string
}>(), {
  fallbackTitle: 'Request failed'
})
</script>

<style scoped>
.b2b-problem-detail {
  border-radius: 8px;
}
</style>
