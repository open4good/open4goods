<template>
  <div v-if="failed" class="product-image product-image--missing">
    <v-icon :icon="mdiImageOffOutline" size="32" aria-hidden="true" />
  </div>
  <NuxtImg
    v-else
    :src="src"
    :alt="alt"
    :width="width"
    :height="height"
    :sizes="sizes"
    format="webp"
    :loading="priority ? 'eager' : 'lazy'"
    :fetchpriority="priority ? 'high' : undefined"
    :preload="priority ? { fetchPriority: 'high' } : undefined"
    class="product-image"
    decoding="async"
    @error="failed = true"
  />
</template>

<script setup lang="ts">
import { mdiImageOffOutline } from '@mdi/js'

/**
 * Product image rendered as a real `<img>` in the SSR HTML (Google Images),
 * resized through IPX (`srcset` from `sizes`) with explicit dimensions (no CLS).
 * Falls back to an icon when the image is missing or fails to load.
 */
withDefaults(
  defineProps<{
    src: string
    alt: string
    width?: number
    height?: number
    sizes?: string
    priority?: boolean
  }>(),
  { width: 400, height: 300, sizes: undefined, priority: false }
)

const failed = ref(false)
</script>

<style scoped lang="sass">
.product-image
  display: block
  width: 100%
  height: 100%
  object-fit: contain

  &--missing
    display: flex
    align-items: center
    justify-content: center
    opacity: 0.4
</style>
