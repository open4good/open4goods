<template>
  <v-sheet
    class="container-1-section"
    :class="[$vuetify.display.smAndDown ? 'mobile-view' : 'desktop-view']"
    color="grey-lighten-3"
    rounded="xl"
  >
    <!-- Container content goes here -->
  </v-sheet>
</template>

<script setup lang="ts">
// No specific props needed based on the current Figma design
</script>

<style lang="sass" scoped>
.container-1-section
  position: relative
  width: 100%
  height: 100%

  &.mobile-view
    // Mobile-specific styles if needed
    padding: 8px

  &.desktop-view
    // Desktop-specific styles if needed
    padding: 16px

  // Ensure responsive behavior
  @media (max-width: 600px)
    padding: 8px

  @media (min-width: 601px)
    padding: 16px
</style>
