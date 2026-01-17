<script setup lang="ts">
import { computed } from 'vue'

export type NudgerCorner =
  | 'top-left'
  | 'top-right'
  | 'bottom-right'
  | 'bottom-left'

const props = withDefaults(
  defineProps<{
    accentCorners?: NudgerCorner[]
    flatCorners?: NudgerCorner[]
    baseRadius?: string
    accentRadius?: string
    border?: boolean
    padding?: string
    background?: string
    elevation?: number
  }>(),
  {
    accentCorners: () => [],
    flatCorners: () => [],
    baseRadius: '30px',
    accentRadius: '50px',
    border: false,
    padding: '1.25rem',
    background: '#ffffff',
    elevation: 0,
  }
)

const resolvedCorners = computed(() => {
  const corners: Record<NudgerCorner, string> = {
    'top-left': props.baseRadius,
    'top-right': props.baseRadius,
    'bottom-right': props.baseRadius,
    'bottom-left': props.baseRadius,
  }

  props.accentCorners.forEach((corner) => {
    corners[corner] = props.accentRadius
  })

  props.flatCorners.forEach((corner) => {
    corners[corner] = '0'
  })

  return corners
})

const styleVars = computed(() => ({
  '--nudger-card-padding': props.padding,
  '--nudger-card-background': props.background,
  '--nudger-card-top-left': resolvedCorners.value['top-left'],
  '--nudger-card-top-right': resolvedCorners.value['top-right'],
  '--nudger-card-bottom-right': resolvedCorners.value['bottom-right'],
  '--nudger-card-bottom-left': resolvedCorners.value['bottom-left'],
}))
</script>

<template>
  <v-sheet
    class="nudger-card"
    :class="{ 'nudger-card--border': props.border }"
    :style="styleVars"
    :elevation="props.elevation"
    rounded="0"
  >
    <slot />
  </v-sheet>
</template>

<style scoped lang="sass">
.nudger-card
  padding: var(--nudger-card-padding)
  background: var(--nudger-card-background)
  border-radius: var(--nudger-card-top-left) var(--nudger-card-top-right) var(--nudger-card-bottom-right) var(--nudger-card-bottom-left)
  box-sizing: border-box

.nudger-card--border
  border: 1px solid rgb(var(--v-theme-primary))
</style>
