<template>
  <v-card class="b2b-kpi-card" variant="flat">
    <v-card-text class="pa-4">
      <div class="d-flex align-start justify-space-between ga-3">
        <div>
          <p class="text-caption text-medium-emphasis mb-1">{{ label }}</p>
          <p class="text-h5 font-weight-bold mb-0">{{ value }}</p>
          <p v-if="caption" class="text-caption text-medium-emphasis mt-1 mb-0">{{ caption }}</p>
        </div>
        <v-avatar v-if="icon" :color="color" variant="tonal" rounded="lg" size="40">
          <v-icon :icon="icon" />
        </v-avatar>
      </div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  label: string
  value: string | number
  caption?: string
  icon?: string
  color?: string
}>(), {
  caption: undefined,
  icon: undefined,
  color: 'primary'
})
</script>

<style scoped>
.b2b-kpi-card {
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 8px;
}
</style>
