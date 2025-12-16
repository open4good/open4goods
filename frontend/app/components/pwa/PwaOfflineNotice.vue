<template>
  <ClientOnly>
    <Transition name="fade-scale">
      <div
        v-if="showIndicator"
        class="pwa-offline-indicator"
        role="status"
        aria-live="polite"
        data-test="pwa-offline-indicator"
      >
        <div v-if="isMobileLayout" class="pwa-offline-indicator__mobile">
          <div class="pwa-offline-indicator__mobile-body">
            <v-icon
              icon="mdi-wifi-off"
              class="pwa-offline-indicator__icon"
              size="24"
              aria-hidden="true"
            />
            <div>
              <p class="pwa-offline-indicator__eyebrow">
                {{ indicatorLabel }}
              </p>
              <p class="pwa-offline-indicator__description">
                {{ mobileHelper }}
              </p>
            </div>
          </div>
          <div class="pwa-offline-indicator__mobile-actions">
            <v-btn
              icon
              variant="text"
              size="small"
              color="warning"
              :aria-label="retryLabel"
              data-test="pwa-offline-retry"
              @click="handleRetry"
            >
              <v-icon icon="mdi-refresh" aria-hidden="true" />
            </v-btn>
            <v-btn
              icon
              variant="text"
              size="small"
              color="primary"
              :aria-label="dismissLabel"
              data-test="pwa-offline-dismiss"
              @click="dismiss"
            >
              <v-icon icon="mdi-close" aria-hidden="true" />
            </v-btn>
          </div>
        </div>
        <v-tooltip
          v-else
          :model-value="true"
          open-on-hover="false"
          open-on-focus="false"
          open-on-click="false"
          persistent
          location="bottom end"
          class="pwa-offline-indicator__tooltip-wrapper"
        >
          <template #activator="{ props: tooltipProps }">
            <v-btn
              v-bind="tooltipProps"
              icon
              size="large"
              color="warning"
              class="pwa-offline-indicator__activator"
              :aria-label="indicatorLabel"
              data-test="pwa-offline-indicator-button"
              @click="handleRetry"
            >
              <v-icon icon="mdi-wifi-off" aria-hidden="true" />
            </v-btn>
          </template>
          <div
            class="pwa-offline-indicator__tooltip"
            data-test="pwa-offline-tooltip"
          >
            <p class="pwa-offline-indicator__title">
              {{ title }}
            </p>
            <p class="pwa-offline-indicator__description">
              {{ description }}
            </p>
            <div class="pwa-offline-indicator__actions">
              <v-btn
                size="small"
                color="warning"
                variant="flat"
                class="text-capitalize"
                data-test="pwa-offline-retry"
                @click="handleRetry"
              >
                {{ retryLabel }}
              </v-btn>
              <v-btn
                size="small"
                variant="text"
                class="text-capitalize"
                data-test="pwa-offline-dismiss"
                @click="dismiss"
              >
                {{ dismissLabel }}
              </v-btn>
            </div>
          </div>
        </v-tooltip>
      </div>
    </Transition>
  </ClientOnly>
</template>

<script setup lang="ts">
import { usePwaOfflineNoticeBridge } from '~/composables/pwa/usePwaOfflineNoticeBridge'

const display = useDisplay()
const { t, isOnline, offlineDismissed } = usePwaOfflineNoticeBridge()

const isMobileLayout = computed(() => display.smAndDown.value)
const showIndicator = computed(() => !isOnline.value && !offlineDismissed.value)
const title = computed(() => String(t('pwa.offline.title')))
const description = computed(() => String(t('pwa.offline.description')))
const indicatorLabel = computed(() => String(t('pwa.offline.indicatorLabel')))
const mobileHelper = computed(() => String(t('pwa.offline.mobileHelper')))
const retryLabel = computed(() => String(t('pwa.offline.cta')))
const dismissLabel = computed(() => String(t('pwa.offline.dismiss')))

watch(isOnline, online => {
  if (online) {
    offlineDismissed.value = false
  }
})

const handleRetry = () => {
  if (!import.meta.client) {
    return
  }

  if (navigator.onLine) {
    window.location.reload()
  }
}

const dismiss = () => {
  offlineDismissed.value = true
}
</script>

<style scoped lang="sass">
.pwa-offline-indicator
  position: fixed
  top: calc(env(safe-area-inset-top) + 16px)
  right: calc(env(safe-area-inset-right) + 16px)
  z-index: 1300
  display: flex
  align-items: flex-start

  @media (max-width: 600px)
    width: calc(100vw - 16px)
    left: 50%
    right: auto
    transform: translateX(-50%)

.pwa-offline-indicator__activator
  background: rgb(var(--v-theme-surface-glass))
  box-shadow: 0 10px 30px -15px rgba(15, 23, 42, 0.4)

.pwa-offline-indicator__tooltip-wrapper
  pointer-events: none

.pwa-offline-indicator__tooltip
  max-width: 320px
  background: rgb(var(--v-theme-surface-glass-strong))
  border-radius: 16px
  padding: 16px
  box-shadow: 0 20px 45px -30px rgba(15, 23, 42, 0.6)
  pointer-events: auto

.pwa-offline-indicator__title
  font-weight: 600
  margin-bottom: 4px

.pwa-offline-indicator__description
  color: rgba(var(--v-theme-text-neutral-soft), 0.92)
  margin-bottom: 12px

.pwa-offline-indicator__actions
  display: flex
  gap: 8px
  justify-content: flex-end

.pwa-offline-indicator__mobile
  width: 100%
  display: flex
  justify-content: space-between
  gap: 8px
  padding: 12px 16px
  border-radius: 20px
  background: rgba(var(--v-theme-surface-glass), 0.96)
  box-shadow: 0 16px 40px -28px rgba(15, 23, 42, 0.55)

.pwa-offline-indicator__mobile-body
  display: flex
  gap: 12px
  align-items: center

.pwa-offline-indicator__icon
  color: rgb(var(--v-theme-warning))

.pwa-offline-indicator__eyebrow
  font-weight: 600
  font-size: 0.85rem
  margin-bottom: 2px

.pwa-offline-indicator__mobile-actions
  display: flex
  gap: 4px

.fade-scale-enter-active,
.fade-scale-leave-active
  transition: all 0.25s ease

.fade-scale-enter-from,
.fade-scale-leave-to
  opacity: 0
  transform: translate3d(0, -6px, 0)
</style>
