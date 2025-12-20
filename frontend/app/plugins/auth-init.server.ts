import { authService } from '~~/shared/api-client/services/auth.services'
import { useAuthStore } from '~/stores/useAuthStore'

/**
 * Hydrates the authentication store on app startup using the token cookie.
 */
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const token = useCookie<string | null>(config.public.tokenCookieName)

  const authStore = useAuthStore()

  authStore.$patch(authService.decodeAuthStateFromToken(token.value))
})
