<template>
  <v-app :style="appBarCssVariables">
    <The-main-menu-container
      :app-bar-height="appBarHeight"
      @toggle-drawer="toggleDrawer"
    />

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

    <v-main :style="appBarCssVariables">
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
const drawerStore = useState('mobileDrawer', () => false)

const { appBarHeight, appBarCssVariables } = useAppBarHeight()

const toggleDrawer = () => {
  drawerStore.value = !drawerStore.value
}
</script>

