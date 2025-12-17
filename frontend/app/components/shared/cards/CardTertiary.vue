<script setup lang="ts">
import CardSurface, { type CardSurfaceProps } from './CardSurface.vue'

type CardTertiaryProps = Omit<CardSurfaceProps, 'tone'>

const props = defineProps<CardTertiaryProps>()
</script>

<template>
  <CardSurface tone="tertiary" v-bind="props">
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
