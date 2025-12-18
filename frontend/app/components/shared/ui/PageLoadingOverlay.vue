<template>
  <ClientOnly>
    <teleport to="body">
      <div
        v-if="routeLoading"
        data-testid="page-loading-overlay"
        class="page-loading-overlay"
        role="status"
        aria-live="polite"
        aria-busy="true"
        :style="{ backgroundColor: scrimColor }"
      >
        <div class="page-loading-overlay__content">
          <v-avatar
            v-if="showLoaderIcon"
            data-testid="page-loading-icon-wrapper"
            class="page-loading-overlay__avatar"
            color="surface-glass"
            size="96"
            variant="flat"
          >
            <v-img
              data-testid="page-loading-icon"
              :src="loaderIcon"
              :alt="spinnerLabel"
              cover
              eager
            />
          </v-avatar>
          <v-progress-circular
            v-else
            data-testid="page-loading-spinner"
            color="primary"
            size="64"
            indeterminate
            :aria-label="spinnerLabel"
          />
          <span class="visually-hidden">{{ spinnerLabel }}</span>
        </div>
      </div>
    </teleport>
  </ClientOnly>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useThemeAsset } from '~~/app/composables/useThemedAsset'

const routeLoading = useState<boolean>('routeLoading', () => false)
const { t } = useI18n()

const spinnerLabel = computed(() =>
  String(t('components.pageLoadingOverlay.label'))
)
const scrimColor = computed(() => 'rgba(var(--v-theme-surface-default), 0.72)')
const loaderIcon = useThemeAsset('launcherIcon')
const showLoaderIcon = computed(() => Boolean(loaderIcon.value))

const previousOverflow = ref<string | null>(null)

const lockScroll = () => {
  if (!import.meta.client) {
    return
  }

  const root = document.documentElement
  if (!root) {
    return
  }

  if (previousOverflow.value === null) {
    previousOverflow.value = root.style.overflow || ''
  }

  root.style.overflow = 'hidden'
}

const unlockScroll = () => {
  if (!import.meta.client) {
    return
  }

  const root = document.documentElement
  if (!root) {
    return
  }

  if (previousOverflow.value !== null) {
    root.style.overflow = previousOverflow.value
    previousOverflow.value = null
  }
}

if (import.meta.client) {
  watch(
    routeLoading,
    isLoading => {
      if (isLoading) {
        lockScroll()
      } else {
        unlockScroll()
      }
    },
    { immediate: true }
  )
}

onBeforeUnmount(() => {
  unlockScroll()
})
</script>

<style scoped lang="sass">
.page-loading-overlay
  position: fixed
  inset: 0
  z-index: 5000
  display: flex
  align-items: center
  justify-content: center
  backdrop-filter: blur(4px)
  pointer-events: all

.page-loading-overlay__content
  display: flex
  flex-direction: column
  align-items: center
  gap: 0.75rem
  padding: 1.5rem
  border-radius: 999px
  background: rgba(var(--v-theme-surface-glass), 0.85)
  box-shadow: 0 12px 40px rgba(var(--v-theme-shadow-primary-600), 0.12)

.page-loading-overlay__avatar
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.24)
  box-shadow: inset 0 0 0 2px rgba(var(--v-theme-surface-default), 0.48)

.visually-hidden
  position: absolute
  width: 1px
  height: 1px
  padding: 0
  margin: -1px
  overflow: hidden
  clip: rect(0, 0, 0, 0)
  white-space: nowrap
  border: 0
</style>
