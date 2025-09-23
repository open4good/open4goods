<template>
  <v-app>
      <!-- Mobile menu -->
      <v-navigation-drawer
        v-model="drawer"
        location="start"
        temporary
        width="300"
        class="mobile-menu-drawer"
      >
        <the-mobile-menu @close="drawer = false" />
      </v-navigation-drawer>
    <div class="app-bar-container">
      <The-main-menu-container @toggle-drawer="toggleDrawer" />
    </div>
    <v-layout>

      <!-- Main content -->
      <v-main class="main-content">
        <slot />
      </v-main>
    </v-layout>
  </v-app>
</template>

<script setup lang="ts">
const drawer = useState("mobileDrawer", () => false);
const drawerStore = useState("mobileDrawer", () => false);

const toggleDrawer = () => {
  drawerStore.value = !drawerStore.value;
};
</script>

<style lang="sass" scoped>
.app-bar-container
  position: fixed
  top: 0
  left: 0
  right: 0
  width: 100%
  height: 18vh
  z-index: 1000

.main-content
  margin-top: 18vh
</style>
