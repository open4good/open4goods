<template>
  <component
    :is="resolvedComponent"
    v-bind="resolvedProps"
    class="docs-content-link"
  >
    <slot />
  </component>
</template>

<script setup lang="ts">
import { computed, resolveComponent } from 'vue'

const props = defineProps<{
  href?: string | null
  target?: string | null
  rel?: string | null
  safeLinks?: boolean
}>()

const isExternal = computed(() => {
  const href = props.href ?? ''
  return (
    href.startsWith('http://') ||
    href.startsWith('https://') ||
    href.startsWith('mailto:') ||
    href.startsWith('tel:')
  )
})

const resolvedComponent = computed(() =>
  isExternal.value ? 'a' : resolveComponent('NuxtLink')
)

const resolvedProps = computed(() => {
  const href = props.href ?? ''

  if (isExternal.value) {
    return {
      href,
      target: props.safeLinks ? '_blank' : (props.target ?? undefined),
      rel: props.safeLinks ? 'noopener noreferrer' : (props.rel ?? undefined),
    }
  }

  return {
    to: href,
  }
})
</script>
