<template>
  <v-card class="inf-admin-metric-card" variant="outlined">
    <v-card-text class="d-flex align-start ga-4">
      <v-avatar :color="color" variant="tonal" rounded="lg" size="44">
        <v-icon :icon="icon" size="24" />
      </v-avatar>

      <div class="min-w-0">
        <p class="text-overline text-medium-emphasis mb-1">{{ label }}</p>
        <p class="text-h4 font-weight-bold mb-1 inf-admin-metric-card__value">{{ value }}</p>
        <p v-if="caption" class="text-caption text-medium-emphasis mb-0">{{ caption }}</p>
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
  icon: 'mdi-chart-box-outline',
  color: 'primary',
  caption: undefined
})
</script>

<style scoped>
.inf-admin-metric-card {
  height: 100%;
}

.inf-admin-metric-card__value {
  line-height: 1.1;
  overflow-wrap: anywhere;
}
</style>
