<template>
  <v-card class="b2b-credit-meter" variant="flat">
    <v-card-text class="pa-4">
      <div class="d-flex align-center justify-space-between ga-4 mb-3">
        <div>
          <p class="text-caption text-medium-emphasis mb-1">{{ label }}</p>
          <p class="text-h5 font-weight-bold mb-0">{{ formattedRemaining }}</p>
        </div>
        <v-chip color="primary" variant="tonal" size="small">{{ formattedConsumed }} used</v-chip>
      </div>

      <v-progress-linear :model-value="usedPercent" color="primary" height="8" rounded />

      <p v-if="projectedExhaustion" class="text-caption text-medium-emphasis mt-3 mb-0">
        Projected exhaustion: {{ projectedExhaustion }}
      </p>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  remaining: number
  consumed: number
  total?: number
  projectedExhaustion?: string
  label?: string
}>(), {
  total: undefined,
  projectedExhaustion: undefined,
  label: 'Credits remaining'
})

const { n } = useI18n()
const effectiveTotal = computed(() => props.total ?? props.remaining + props.consumed)
const usedPercent = computed(() => effectiveTotal.value > 0 ? Math.min(100, (props.consumed / effectiveTotal.value) * 100) : 0)
const formattedRemaining = computed(() => n(props.remaining))
const formattedConsumed = computed(() => n(props.consumed))
</script>

<style scoped>
.b2b-credit-meter {
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 8px;
}
</style>
