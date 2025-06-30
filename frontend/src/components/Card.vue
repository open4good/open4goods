<template>
  <div
    class="p-4 border rounded"
    :class="variant === 'primary' ? 'border-blue-500 bg-blue-50' : ''"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'
import type { PropType } from 'vue'

export interface CardProps {
  variant?: 'default' | 'primary'
}

const props = defineProps({
  variant: {
    type: String as PropType<CardProps['variant']>,
    default: 'default',
    validator: (value: string) => ['default', 'primary'].includes(value)
  }
})

const { variant } = toRefs(props)
</script>
