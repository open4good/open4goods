import { TheMainFooter } from '../.nuxt/components';
<template>
  <v-app>
    <v-main>
      <slot />
    </v-main>

    <TheMainFooter>
      <template #footer>
        <the-main-footer-content />
      </template>
    </TheMainFooter>
  </v-app>
</template>

<script setup lang="ts">
// Layout par défaut avec Vuetify
</script>
