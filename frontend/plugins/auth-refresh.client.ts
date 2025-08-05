import { jwtDecode } from 'jwt-decode'
import { useAuthService } from '~/services/auth.service'
import { handleErrors } from '~/utils'

/**
 * Periodically checks the access token and refreshes it when expired.
 */
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const token = useCookie<string | null>(config.tokenCookieName)
  const { refresh } = useAuthService()

  const checkExpiration = async () => {
    if (!token.value) return
    try {
      const { exp } = jwtDecode<{ exp?: number }>(token.value)
      if (exp && exp * 1000 < Date.now()) {
        await refresh()
      }
    } catch (err) {
      handleErrors._handleError(err, 'Failed to decode JWT')
    }
  }

  checkExpiration()
  setInterval(checkExpiration, 60_000)
})
