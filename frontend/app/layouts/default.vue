<template>
  <v-app>
    <The-main-menu-container @toggle-drawer="toggleDrawer" />

    <!-- Mobile menu -->
    <ClientOnly>
      <v-navigation-drawer
        v-model="drawer"
        location="start"
        temporary
        width="300"
        class="mobile-menu-drawer"
      >
        <the-mobile-menu @close="drawer = false" />
      </v-navigation-drawer>
    </ClientOnly>

    <ClientOnly>
      <template #fallback>
        <div class="pre-hydration-app-bar-spacer" aria-hidden="true" />
      </template>
    </ClientOnly>

    <v-main>
      <slot />
    </v-main>

    <TheMainFooter>
      <template #footer>
        <TheMainFooterContent />
      </template>
    </TheMainFooter>
  </v-app>
</template>

<script setup lang="ts">
const drawer = useState('mobileDrawer', () => false)
const drawerStore = useState("mobileDrawer", () => false);

const toggleDrawer = () => {
  drawerStore.value = !drawerStore.value;
};
</script>

<style scoped lang="sass">
.pre-hydration-app-bar-spacer
  height: 64px

  @media (max-width: 959px)
    height: 56px
</style>

