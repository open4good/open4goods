import { jwtDecode } from 'jwt-decode'
import { authService } from '~~/shared/api-client/services/auth.services'

/**
 * Periodically checks the access token and refreshes it when expired.
 */
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const token = useCookie<string | null>(config.public.tokenCookieName)

  const checkExpiration = async () => {
    if (!token.value) return
    try {
      const { exp } = jwtDecode<{ exp?: number }>(token.value)
      if (exp && exp * 1000 < Date.now()) {
        await authService.refresh()
      }
    } catch (err) {
      console.error('Failed to decode JWT', err)
    }
  }

  checkExpiration()
  setInterval(checkExpiration, 60_000)
})
