import { useAuthService } from '~/services/auth.service'

/**
 * Hydrates the authentication store on app startup using the token cookie.
 */
export default defineNuxtPlugin(() => {
  const { syncAuthState } = useAuthService()
  syncAuthState()
})
