import { authService } from '~~/shared/api-client/services/auth.services'

/**
 * Hydrates the authentication store on app startup using the token cookie.
 */
export default defineNuxtPlugin(() => {
  authService.syncAuthState()
})
