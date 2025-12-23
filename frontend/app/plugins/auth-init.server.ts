import { useAuthStore } from '~/stores/useAuthStore'

/**
 * Hydrates the authentication store on app startup using the token cookie.
 * JWT decoding happens server-side only for security.
 */
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const token = useCookie<string | null>(config.public.tokenCookieName)

  const authStore = useAuthStore()

  // Server-side: decode JWT using server utility
  if (import.meta.server) {
    authStore.$patch(decodeAuthStateFromToken(token.value))
  }
})
