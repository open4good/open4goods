import { computed, watch } from 'vue'
import { defineNuxtPlugin, useRuntimeConfig } from '#app'

import { useI18nInContextEditorState } from '~/composables/i18n-inctx/useI18nInContextEditorState'
import { useAuthStore } from '~/stores/useAuthStore'

export default defineNuxtPlugin(() => {
  if (import.meta.server) {
    return
  }

  const authStore = useAuthStore()
  const runtimeConfig = useRuntimeConfig()
  const { close, reset } = useI18nInContextEditorState()

  const allowedRoles = computed(() =>
    Array.isArray(runtimeConfig.public.editRoles)
      ? (runtimeConfig.public.editRoles as string[]).filter(Boolean)
      : []
  )

  const hasEditorRole = computed(() =>
    allowedRoles.value.length > 0 && allowedRoles.value.some((role) => authStore.roles.includes(role))
  )

  const isEligible = computed(() => authStore.isLoggedIn && hasEditorRole.value)

  let overlayModule: Awaited<ReturnType<typeof import('~/lib/i18n-inctx/overlay')>> | null = null
  let scanInterval: ReturnType<typeof setInterval> | null = null

  async function ensureOverlayModule() {
    overlayModule ??= await import('~/lib/i18n-inctx/overlay')
    return overlayModule
  }

  async function enableOverlay() {
    const module = await ensureOverlayModule()
    module.install()

    if (scanInterval === null) {
      scanInterval = window.setInterval(() => {
        module.install()
      }, 1000)
    }
  }

  async function disableOverlay() {
    if (scanInterval !== null) {
      clearInterval(scanInterval)
      scanInterval = null
    }

    if (overlayModule) {
      overlayModule.teardown()
    }

    close()
    reset()
  }

  const stopWatcher = watch(
    isEligible,
    (eligible) => {
      if (eligible) {
        void enableOverlay()
      } else {
        void disableOverlay()
      }
    },
    { immediate: true }
  )

  if (import.meta.hot) {
    import.meta.hot.dispose(() => {
      stopWatcher()
      void disableOverlay()
    })
  }
})
