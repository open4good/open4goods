<template>
  <v-app>
    <v-navigation-drawer
      v-model="drawer"
      location="start"
      temporary
      width="300"
      class="mobile-menu-drawer"
    >
      <the-mobile-menu @close="drawer = false" />
    </v-navigation-drawer>

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
const drawer = useState('mobileDrawer', () => false)
</script>

