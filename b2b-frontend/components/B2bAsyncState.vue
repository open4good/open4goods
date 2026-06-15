<template>
  <v-sheet class="b2b-async-state pa-6 text-center" rounded="lg">
    <v-progress-circular v-if="state === 'loading'" indeterminate color="primary" class="mb-4" />
    <v-avatar v-else :color="tone" variant="tonal" size="52" class="mb-4">
      <v-icon :icon="iconName" size="28" />
    </v-avatar>

    <h2 class="text-h6 font-weight-bold mb-2">{{ title }}</h2>
    <p v-if="description" class="text-body-2 text-medium-emphasis mb-4 b2b-async-state__description">
      {{ description }}
    </p>
    <slot name="actions" />
  </v-sheet>
</template>

<script setup lang="ts">
type AsyncState = 'loading' | 'empty' | 'error' | 'permission-denied' | 'partial'

const props = defineProps<{
  state: AsyncState
  title: string
  description?: string
}>()

const tone = computed(() => {
  if (props.state === 'error') {
    return 'error'
  }
  if (props.state === 'permission-denied') {
    return 'warning'
  }
  return 'primary'
})

const iconName = computed(() => {
  if (props.state === 'error') {
    return 'mdi-alert-circle-outline'
  }
  if (props.state === 'permission-denied') {
    return 'mdi-lock-outline'
  }
  if (props.state === 'partial') {
    return 'mdi-cloud-alert-outline'
  }
  return 'mdi-database-search-outline'
})
</script>

<style scoped>
.b2b-async-state {
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
}

.b2b-async-state__description {
  max-width: 560px;
  margin-inline: auto;
}
</style>
