<script setup lang="ts">
import { computed } from 'vue'

export type NudgerCorner =
  | 'top-left'
  | 'top-right'
  | 'bottom-right'
  | 'bottom-left'

const props = withDefaults(
  defineProps<{
    /** Array of corners to apply the accent (larger) radius */
    accentCorners?: NudgerCorner[]
    /** Array of corners to make flat (0 radius) */
    flatCorners?: NudgerCorner[]
    /** Base border-radius applied to all corners by default */
    baseRadius?: string
    /** Radius applied to accent corners */
    accentRadius?: string
    /** Individual override for top-left corner radius */
    topLeftRadius?: string
    /** Individual override for top-right corner radius */
    topRightRadius?: string
    /** Individual override for bottom-right corner radius */
    bottomRightRadius?: string
    /** Individual override for bottom-left corner radius */
    bottomLeftRadius?: string
    /** Show primary-colored border */
    border?: boolean
    /** Internal padding */
    padding?: string
    /** Background color or gradient */
    background?: string
    /** Vuetify elevation level */
    elevation?: number
    /** Enable box-shadow effect */
    shadow?: boolean
    /** Enable hover interaction (lift + shadow enhancement) */
    hoverable?: boolean
  }>(),
  {
    accentCorners: () => [],
    flatCorners: () => [],
    baseRadius: '30px',
    accentRadius: '50px',
    topLeftRadius: undefined,
    topRightRadius: undefined,
    bottomRightRadius: undefined,
    bottomLeftRadius: undefined,
    border: false,
    padding: '1.25rem',
    background: '#ffffff',
    elevation: 0,
    shadow: true,
    hoverable: true,
  }
)

const resolvedCorners = computed(() => {
  const corners: Record<NudgerCorner, string> = {
    'top-left': props.baseRadius,
    'top-right': props.baseRadius,
    'bottom-right': props.baseRadius,
    'bottom-left': props.baseRadius,
  }

  // Apply accent corners
  props.accentCorners.forEach((corner) => {
    corners[corner] = props.accentRadius
  })

  // Apply flat corners
  props.flatCorners.forEach((corner) => {
    corners[corner] = '0'
  })

  // Individual overrides take highest priority
  if (props.topLeftRadius !== undefined) corners['top-left'] = props.topLeftRadius
  if (props.topRightRadius !== undefined) corners['top-right'] = props.topRightRadius
  if (props.bottomRightRadius !== undefined) corners['bottom-right'] = props.bottomRightRadius
  if (props.bottomLeftRadius !== undefined) corners['bottom-left'] = props.bottomLeftRadius

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
    :class="{
      'nudger-card--border': props.border,
      'nudger-card--shadow': props.shadow,
      'nudger-card--hoverable': props.hoverable,
    }"
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
  color: rgb(var(--v-theme-text-neutral-strong))
  transition: box-shadow 0.2s ease, transform 0.2s ease, border-color 0.2s ease

.nudger-card--border
  border: 1px solid rgb(var(--v-theme-primary))

.nudger-card--shadow
  box-shadow: 0 10px 26px rgba(var(--v-theme-primary), 0.06)

.nudger-card--hoverable:hover
  box-shadow: 0 18px 42px rgba(var(--v-theme-primary), 0.12)
  transform: translateY(-1px)
</style>
