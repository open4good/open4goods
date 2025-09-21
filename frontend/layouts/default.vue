<template>
  <v-app>
    <v-main class="fill-height">
      <slot />
      <TheMainFooter>
        <template #footer>
          <the-main-footer-content />
        </template>
      </TheMainFooter>
    </v-main>

    
  </v-app>
</template>

<script setup lang="ts">
// Layout par défaut avec Vuetify
</script>

