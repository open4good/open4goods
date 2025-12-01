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
            <v-btn color="warning" size="small" data-test="pwa-update-cta" @click="handleUpdate">
              {{ updateCta }}
            </v-btn>
          </template>
        </v-alert>
      </Transition>

      <Transition name="fade">
        <v-alert
          v-if="offlineReady"
          class="pwa-prompts__alert"
          border="start"
          color="success"
          variant="tonal"
          :title="offlineReadyTitle"
          data-test="pwa-offline-ready"
        >
          {{ offlineReadyDescription }}
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
const updateDescription = computed(() => String(t('pwa.install.updateDescription')))
const updateCta = computed(() => String(t('pwa.install.updateCta')))
const offlineReadyTitle = computed(() => String(t('pwa.install.offlineReady')))
const offlineReadyDescription = computed(() => String(t('pwa.install.offlineReadyDescription')))
const installSuccessMessage = computed(() => installOutcome.value === 'accepted' ? String(t('pwa.install.success')) : '')
const installErrorMessage = computed(() => installError.value ? String(t('pwa.install.error')) : '')

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
