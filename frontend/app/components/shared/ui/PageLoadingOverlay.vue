<template>
  <v-overlay
    data-testid="page-loading-overlay"
    class="page-loading-overlay"
    :model-value="routeLoading"
    :scrim="scrimColor"
    persistent
  >
    <div
      v-if="routeLoading"
      class="page-loading-overlay__content"
      role="status"
      aria-live="polite"
    >
      <v-progress-circular
        data-testid="page-loading-spinner"
        color="primary"
        size="64"
        indeterminate
        :aria-label="spinnerLabel"
      />
      <span class="visually-hidden">{{ spinnerLabel }}</span>
    </div>
  </v-overlay>
</template>

<script setup lang="ts">
const routeLoading = useState<boolean>('routeLoading', () => false)
const { t } = useI18n()

const spinnerLabel = computed(() => String(t('components.pageLoadingOverlay.label')))
const scrimColor = computed(() => 'rgba(var(--v-theme-surface-default), 0.72)')
</script>

<style scoped lang="sass">
.page-loading-overlay :deep(.v-overlay__content)
  width: 100vw
  height: 100vh
  display: flex
  align-items: center
  justify-content: center

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
