import { authService } from '~~/shared/api-client/services/auth.services'
import { useAuthStore } from '~/stores/useAuthStore'

/**
 * Periodically refreshes the access token.
 * Note: JWT expiration is checked server-side; client just refreshes periodically.
 */
export default defineNuxtPlugin(() => {
  const authStore = useAuthStore()

  const refreshAuth = async () => {
    if (!authStore.isLoggedIn) return
    try {
      const authState = await authService.refresh()
      authStore.$patch(authState)
    } catch (err) {
      console.error('Failed to refresh auth', err)
      // If refresh fails, clear auth state
      authStore.$patch({ roles: [], isLoggedIn: false, username: null })
    }
  }

  // Refresh every 5 minutes to keep session active
  refreshAuth()
  setInterval(refreshAuth, 5 * 60_000)
})
