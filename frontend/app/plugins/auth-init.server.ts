import { authService } from '~~/shared/api-client/services/auth.services'

/**
 * Hydrates the authentication store on app startup using the token cookie.
 */
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const token = useCookie<string | null>(config.public.tokenCookieName)

  authService.syncAuthState(token.value)
})
