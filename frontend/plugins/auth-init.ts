import { jwtDecode } from 'jwt-decode'
import type { JwtPayload } from 'jwt-decode'

/**
 * Initialize authentication state from the access token cookie
 */
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const token = useCookie<string | null>(config.tokenCookieName)
  const authStore = useAuthStore()

  if (token.value) {
    try {
      const payload = jwtDecode<JwtPayload>(token.value)
      authStore.setAuth(token.value, payload)
    } catch (err) {
      console.error('Failed to decode access token', err)
      authStore.resetAuth()
    }
  }
})
