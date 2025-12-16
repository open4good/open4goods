<script setup lang="ts">
import { computed, mergeProps, useAttrs } from 'vue'
import type { Component } from 'vue'

type HeroSurfaceVariant = 'aurora' | 'halo' | 'prism' | 'pulse' | 'mesh' | 'orbit'

defineOptions({ name: 'HeroSurface' })

const props = withDefaults(
  defineProps<{
    tag?: keyof HTMLElementTagNameMap | Component
    variant?: HeroSurfaceVariant
    bleed?: boolean
  }>(),
  {
    tag: 'section',
    variant: 'aurora',
    bleed: false,
  },
)

const attrs = useAttrs()

const heroSurfaceClasses = computed(() => ({
  'hero-surface': true,
  [`hero-surface--${props.variant}`]: Boolean(props.variant),
  'hero-surface--bleed': props.bleed,
}))

const rootProps = computed(() =>
  mergeProps(attrs, {
    class: heroSurfaceClasses.value,
  }),
)
</script>

<template>
  <component :is="props.tag" v-bind="rootProps">
    <slot />
  </component>
</template>
