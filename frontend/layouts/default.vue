<template>
  <v-app>
    <TheNavbar />
    <v-main>
      <slot />
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
// Layout with simple navigation bar
import TheNavbar from '~/components/shared/TheNavbar.vue'
</script>
