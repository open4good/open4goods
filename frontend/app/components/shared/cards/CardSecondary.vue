<script setup lang="ts">
import CardSurface, { type CardSurfaceProps } from './CardSurface.vue'

type CardSecondaryProps = Omit<CardSurfaceProps, 'tone'>

const props = defineProps<CardSecondaryProps>()
</script>

<template>
  <CardSurface tone="secondary" v-bind="props">
    <template #header>
      <slot name="header" />
    </template>
    <slot />
    <template #actions>
      <slot name="actions" />
    </template>
    <template #footer>
      <slot name="footer" />
    </template>
  </CardSurface>
</template>
