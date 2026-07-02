<template>
  <ClientOnly>
    <div class="pwa-prompts" aria-live="polite">
      <Transition name="slide-up">
        <v-alert
          v-if="shouldRenderInstallBanner"
          class="pwa-prompts__alert"
          border="start"
          color="primary"
          variant="tonal"
          :title="installTitle"
          prominent
          data-test="pwa-install-banner"
        >
          <p class="pwa-prompts__description">
            {{ installDescription }}
          </p>
          <template #actions>
            <v-btn
              color="primary"
              size="small"
              :loading="installInProgress"
              data-test="pwa-install-cta"
              @click="handleInstall"
            >
              {{ installCta }}
            </v-btn>
            <v-btn
              variant="text"
              size="small"
              class="text-capitalize"
              data-test="pwa-install-dismiss"
              @click="dismissInstall"
            >
              {{ installDismissLabel }}
            </v-btn>
          </template>
        </v-alert>
      </Transition>

      <Transition name="fade">
        <v-alert
          v-if="showUpdateBanner"
          class="pwa-prompts__alert"
          border="start"
          color="warning"
          variant="tonal"
          :title="updateTitle"
          prominent
          data-test="pwa-update-banner"
        >
          <p class="pwa-prompts__description">
            {{ updateDescription }}
          </p>
          <template #actions>
            <v-btn
              color="warning"
              size="small"
              data-test="pwa-update-cta"
              @click="handleUpdate"
            >
              {{ updateCta }}
            </v-btn>
          </template>
        </v-alert>
      </Transition>

      <Transition name="fade">
        <v-alert
          v-if="installSuccessMessage"
          class="pwa-prompts__alert"
          border="start"
          color="success"
          variant="tonal"
          data-test="pwa-install-success"
        >
          {{ installSuccessMessage }}
        </v-alert>
      </Transition>

      <Transition name="fade">
        <v-alert
          v-if="installErrorMessage"
          class="pwa-prompts__alert"
          border="start"
          color="error"
          variant="tonal"
          data-test="pwa-install-error"
        >
          {{ installErrorMessage }}
        </v-alert>
      </Transition>
    </div>

    <v-snackbar
      v-model="showOfflineReadySnackbar"
      location="bottom left"
      :timeout="4000"
      color="success"
      variant="elevated"
      data-test="pwa-offline-ready"
    >
      <p class="pwa-prompts__snackbar-title">{{ offlineReadyTitle }}</p>
      <p class="pwa-prompts__description">{{ offlineReadyDescription }}</p>
    </v-snackbar>
  </ClientOnly>
</template>

<script setup lang="ts">
import { usePwaInstallPromptBridge } from '~/composables/pwa/usePwaInstallPromptBridge'

const {
  t,
  installOutcome,
  installError,
  installInProgress,
  offlineReady,
  updateAvailable,
  dismissInstall,
  requestInstall,
  applyUpdate,
} = usePwaInstallPromptBridge()

const shouldRenderInstallBanner = computed(() => false)
const showUpdateBanner = computed(() => updateAvailable.value)
const installTitle = computed(() => String(t('pwa.install.title')))
const installDescription = computed(() => String(t('pwa.install.description')))
const installCta = computed(() => String(t('pwa.install.cta')))
const installDismissLabel = computed(() => String(t('pwa.install.dismiss')))
const updateTitle = computed(() => String(t('pwa.install.updateTitle')))
const updateDescription = computed(() =>
  String(t('pwa.install.updateDescription'))
)
const updateCta = computed(() => String(t('pwa.install.updateCta')))
const offlineReadyTitle = computed(() => String(t('pwa.install.offlineReady')))
const offlineReadyDescription = computed(() =>
  String(t('pwa.install.offlineReadyDescription'))
)
const installSuccessMessage = computed(() =>
  installOutcome.value === 'accepted' ? String(t('pwa.install.success')) : ''
)
const installErrorMessage = computed(() =>
  installError.value ? String(t('pwa.install.error')) : ''
)

// The offline-ready toast is informational, not actionable: show it at most
// once per session, and only after the page has settled (idle or 2s after
// mount) so it never collides with above-the-fold content on first paint.
const OFFLINE_READY_SESSION_KEY = 'pwa-offline-ready-shown'
const hasShownOfflineReady = ref(false)
const offlineReadyDelayElapsed = ref(false)
const showOfflineReadySnackbar = ref(false)

onMounted(() => {
  if (!import.meta.client) {
    return
  }

  hasShownOfflineReady.value =
    sessionStorage.getItem(OFFLINE_READY_SESSION_KEY) === 'true'

  const markDelayElapsed = () => {
    offlineReadyDelayElapsed.value = true
  }

  if ('requestIdleCallback' in window) {
    window.requestIdleCallback(markDelayElapsed, { timeout: 2000 })
  } else {
    setTimeout(markDelayElapsed, 2000)
  }
})

watch([offlineReady, offlineReadyDelayElapsed], ([ready, delayElapsed]) => {
  if (ready && delayElapsed && !hasShownOfflineReady.value) {
    showOfflineReadySnackbar.value = true
    hasShownOfflineReady.value = true
    if (import.meta.client) {
      sessionStorage.setItem(OFFLINE_READY_SESSION_KEY, 'true')
    }
  }
})

const handleInstall = async () => {
  await requestInstall()
}

const handleUpdate = async () => {
  await applyUpdate()
}
</script>

<style scoped lang="sass">
.pwa-prompts
  position: fixed
  right: 16px
  bottom: calc(env(safe-area-inset-bottom) + 16px)
  display: flex
  flex-direction: column
  gap: 8px
  width: min(360px, calc(100vw - 32px))
  z-index: 1100

  @media (max-width: 600px)
    right: 8px
    left: 8px
    width: auto

.pwa-prompts__alert
  border-radius: 16px

.pwa-prompts__description
  margin-bottom: 0
  font-size: 0.95rem

.pwa-prompts__snackbar-title
  font-weight: 600
  margin-bottom: 4px

.slide-up-enter-active,
.slide-up-leave-active
  transition: all 0.3s ease

.slide-up-enter-from,
.slide-up-leave-to
  opacity: 0
  transform: translateY(16px)

.fade-enter-active,
.fade-leave-active
  transition: opacity 0.2s ease

.fade-enter-from,
.fade-leave-to
  opacity: 0
</style>
