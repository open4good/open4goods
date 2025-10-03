<template>
  <v-app>
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
import { computed } from 'vue'
import { useDisplay } from 'vuetify'

const drawer = useState('mobileDrawer', () => false)
const drawerStore = useState('mobileDrawer', () => false)

const { mdAndUp } = useDisplay()

const appBarHeight = computed(() => (mdAndUp.value ? 64 : 56))
const appBarCssVariables = computed(() => ({
  '--app-bar-height': `${appBarHeight.value}px`,
}))

const toggleDrawer = () => {
  drawerStore.value = !drawerStore.value
}
</script>

