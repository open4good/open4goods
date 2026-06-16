<template>
  <v-card class="b2b-usage-chart" variant="flat">
    <v-card-title class="text-subtitle-1 font-weight-bold">{{ title }}</v-card-title>
    <v-card-text>
      <div v-if="points.length" class="b2b-usage-chart__bars">
        <div
          v-for="point in points"
          :key="point.label"
          class="b2b-usage-chart__bar"
          :style="{ height: `${barHeight(point.value)}%` }"
          :title="`${point.label}: ${point.value}`"
        />
      </div>
      <div v-else class="d-flex flex-column align-center justify-center py-10 text-center text-medium-emphasis">
        <v-icon icon="mdi-chart-bar-stacked" size="40" class="mb-2" />
        <p class="text-body-2 mb-0">No usage data yet.</p>
      </div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
const props = defineProps<{
  title: string
  points: Array<{ label: string; value: number }>
}>()

const maxValue = computed(() => Math.max(...props.points.map((point) => point.value), 1))

function barHeight(value: number) {
  return Math.max(8, (value / maxValue.value) * 100)
}
</script>

<style scoped>
.b2b-usage-chart {
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 8px;
}

.b2b-usage-chart__bars {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(16px, 1fr);
  align-items: end;
  min-height: 180px;
  gap: 8px;
}

.b2b-usage-chart__bar {
  min-height: 8px;
  border-radius: 6px 6px 0 0;
  background: rgb(var(--v-theme-primary));
}
</style>
