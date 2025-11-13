<template>
  <ClientOnly>
    <Transition name="slide-down">
      <v-alert
        v-if="showBanner"
        class="pwa-offline-banner"
        border="start"
        color="warning"
        variant="tonal"
        :title="title"
        data-test="pwa-offline-banner"
      >
        <p class="pwa-offline-banner__description">
          {{ description }}
        </p>
        <template #actions>
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
        </template>
      </v-alert>
    </Transition>
  </ClientOnly>
</template>

<script setup lang="ts">
import { usePwaOfflineNoticeBridge } from '~/composables/pwa/usePwaOfflineNoticeBridge'

const { t, isOnline, offlineDismissed } = usePwaOfflineNoticeBridge()

const showBanner = computed(() => !isOnline.value && !offlineDismissed.value)
const title = computed(() => String(t('pwa.offline.title')))
const description = computed(() => String(t('pwa.offline.description')))
const retryLabel = computed(() => String(t('pwa.offline.cta')))
const dismissLabel = computed(() => String(t('pwa.offline.dismiss')))

watch(isOnline, (online) => {
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
.pwa-offline-banner
  position: fixed
  top: calc(env(safe-area-inset-top) + 16px)
  left: 50%
  transform: translateX(-50%)
  width: min(480px, calc(100vw - 32px))
  z-index: 1200
  border-radius: 16px

  @media (max-width: 600px)
    width: calc(100vw - 16px)

.pwa-offline-banner__description
  margin: 0

.slide-down-enter-active,
.slide-down-leave-active
  transition: all 0.25s ease

.slide-down-enter-from,
.slide-down-leave-to
  opacity: 0
  transform: translate(-50%, -20px)
</style>
