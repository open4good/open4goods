<template>
  <v-app>
    <PageLoadingOverlay />
    <The-main-menu-container @toggle-drawer="toggleDrawer" />

    <!-- Mobile menu -->
    <ClientOnly>
      <template v-if="isMobileNavigation">
        <v-navigation-drawer
          v-model="drawer"
          location="start"
          :temporary="isMobileNavigation"
          :scrim="isMobileNavigation"
          :width="drawerWidth"
          floating
          class="mobile-menu-drawer"
          :style="drawerInlineStyles"
        >
          <the-mobile-menu @close="drawer = false" />
        </v-navigation-drawer>
      </template>
    </ClientOnly>

    <ClientOnly>
      <template #fallback>
        <div class="pre-hydration-app-bar-spacer" aria-hidden="true" />
      </template>
    </ClientOnly>

    <v-main class="layout-main-content">
      <slot />
    </v-main>

    <TheMainFooter>
      <template #footer>
        <TheMainFooterContent />
      </template>
    </TheMainFooter>

    <CategoryComparePanel v-if="showComparePanel" />
    <PwaOfflineNotice />
    <PwaInstallPrompt />
  </v-app>
</template>

<script setup lang="ts">
const drawer = useState('mobileDrawer', () => false)
const route = useRoute()
const device = useDevice()
const display = useDisplay()

const showComparePanel = computed(() => {
  const routeName = route.name?.toString() ?? ''
  return !routeName.startsWith('compare')
})

const toggleDrawer = () => {
  drawer.value = !drawer.value
}

const isMobileNavigation = computed(
  () => device.isMobileOrTablet || display.mdAndDown.value
)
const drawerWidth = computed(() => (isMobileNavigation.value ? 320 : 360))
const drawerInlineStyles = computed(() => ({
  paddingBottom: isMobileNavigation.value
    ? 'calc(env(safe-area-inset-bottom) + 24px)'
    : '24px',
}))

watch(
  () => isMobileNavigation.value,
  isMobileView => {
    if (!isMobileView) {
      drawer.value = false
    }
  },
  { immediate: true }
)
</script>

<style scoped lang="sass">

.layout-main-content
  padding-top: calc(var(--v-layout-top, 64px) + env(safe-area-inset-top))
  background-color: transparent

  @media (max-width: 959px)
    padding-top: calc(var(--v-layout-top, 56px) + env(safe-area-inset-top))

.pre-hydration-app-bar-spacer
  height: 64px

  @media (max-width: 959px)
    height: 56px

.mobile-menu-drawer
  padding-top: calc(env(safe-area-inset-top) + 8px)
</style>
