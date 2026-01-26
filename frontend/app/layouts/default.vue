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

    <v-main class="app-main-adjust">
      <slot />
    </v-main>

    <TheMainFooter>
      <template #footer>
        <TheMainFooterContent />
      </template>
    </TheMainFooter>

    <div class="panels-stack">
      <CategoryComparePanel v-if="showComparePanel" />
      <AiReviewGenerationPanel />
    </div>

    <AiReviewCompletionDialog />
    <PwaOfflineNotice />
    <PwaInstallPrompt />
  </v-app>
</template>

<script setup lang="ts">
import { useAiReviewGenerationStore } from '~/stores/useAiReviewGenerationStore'

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

const aiReviewStore = useAiReviewGenerationStore()

onMounted(() => {
  aiReviewStore.resumePending()
})
</script>

<style scoped lang="sass">
.panels-stack
  position: fixed
  inset-inline-end: 1.5rem
  inset-block-end: 1.5rem
  display: flex
  flex-direction: column
  align-items: flex-end
  gap: 1rem
  z-index: 900
  pointer-events: none
  width: min(400px, calc(100% - 3rem))

  @media (max-width: 600px)
    inset-inline: 1rem
    inset-block-end: 1rem
    width: calc(100% - 2rem)

.app-main-adjust
  padding-top: 64px !important

  @media (max-width: 959px)
    padding-top: 56px !important

.mobile-menu-drawer
  padding-top: calc(env(safe-area-inset-top) + 8px)
</style>
