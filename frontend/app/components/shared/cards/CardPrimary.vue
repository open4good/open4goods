<script setup lang="ts">
import CardSurface, { type CardSurfaceProps } from './CardSurface.vue'

type CardPrimaryProps = Omit<CardSurfaceProps, 'tone'>

const props = defineProps<CardPrimaryProps>()
</script>

<template>
  <CardSurface tone="primary" v-bind="props">
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
