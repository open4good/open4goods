<script setup lang="ts">
/**
 * Space holder for the client-only desktop home hero.
 *
 * Used both as the `<ClientOnly>` fallback (server render) and as the loading
 * state of the async hero chunk, so the page keeps the same height until the
 * hero paints. Its min-height mirrors `.home-hero` on desktop (100dvh): a
 * shorter holder, or none while the chunk loads, made the whole page collapse
 * and re-expand on load (CLS 1.5 on desktop).
 */
</script>

<template>
  <section class="home-hero-fallback" aria-hidden="true" />
</template>

<style scoped lang="sass">
.home-hero-fallback
  min-height: 100dvh
  background: rgb(var(--v-theme-surface-default))
</style>
