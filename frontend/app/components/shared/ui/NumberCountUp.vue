<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTransition, TransitionPresets } from '@vueuse/core'

const props = withDefaults(
  defineProps<{
    to: number
    duration?: number
    delay?: number
  }>(),
  {
    duration: 2000,
    delay: 0,
  }
)

const output = useTransition(
  computed(() => props.to),
  {
    duration: props.duration,
    delay: props.delay,
    transition: TransitionPresets.easeOutExpo,
  }
)

const { locale } = useI18n()
const formatted = computed(() => {
  try {
    return new Intl.NumberFormat(locale.value).format(Math.round(output.value))
  } catch {
    return Math.round(output.value).toString()
  }
})
</script>

<template>
  <span>{{ formatted }}</span>
</template>
