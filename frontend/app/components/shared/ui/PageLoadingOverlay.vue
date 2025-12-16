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
          <v-progress-circular
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

const routeLoading = useState<boolean>('routeLoading', () => false)
const { t } = useI18n()

const spinnerLabel = computed(() =>
  String(t('components.pageLoadingOverlay.label'))
)
const scrimColor = computed(() => 'rgba(var(--v-theme-surface-default), 0.72)')

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
