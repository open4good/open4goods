import { computed, effectScope, watch } from 'vue'
import { storeToRefs } from 'pinia'
import type { I18nInContextOverlay } from '~/lib/i18n-inctx/overlay'
import { useAuthStore } from '~/stores/useAuthStore'

const normalizeRoles = (roles: string[] | undefined | null): string[] => {
  if (!roles || roles.length === 0) {
    return []
  }

  return roles
    .map((role) => role.trim())
    .filter((role): role is string => role.length > 0)
}

export default defineNuxtPlugin((nuxtApp) => {
  if (import.meta.server) {
    return
  }

  const runtimeConfig = useRuntimeConfig()
  const authStore = useAuthStore()
  const { isLoggedIn, roles } = storeToRefs(authStore)

  const scope = effectScope()
  let overlayInstance: I18nInContextOverlay | null = null
  let loadToken = 0

  const stopOverlay = () => {
    scope.stop()

    if (overlayInstance) {
      overlayInstance.teardown()
      overlayInstance = null
    }
  }

  scope.run(() => {
    const eligibleRoles = normalizeRoles(runtimeConfig.public.editRoles as string[] | undefined)

    const canEditInContext = computed(() => {
      if (!isLoggedIn.value) {
        return false
      }

      const userRoles = normalizeRoles(roles.value)

      if (userRoles.length === 0 || eligibleRoles.length === 0) {
        return false
      }

      return userRoles.some((role) => eligibleRoles.includes(role))
    })

    watch(
      canEditInContext,
      async (eligible) => {
        const currentToken = ++loadToken

        if (eligible) {
          if (overlayInstance) {
            return
          }

          const overlayModule = await import('~/lib/i18n-inctx/overlay')

          if (currentToken !== loadToken) {
            return
          }

          overlayInstance = await overlayModule.install(nuxtApp)
          return
        }

        if (overlayInstance) {
          overlayInstance.teardown()
          overlayInstance = null
        }
      },
      { immediate: true }
    )
  })

  if (import.meta.hot) {
    import.meta.hot.dispose(() => {
      stopOverlay()
    })
  }
})
