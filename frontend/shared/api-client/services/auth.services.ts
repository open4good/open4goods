/**
 * Authentication service handling login and token refresh calls.
 */
import { jwtDecode } from 'jwt-decode'
import { useAuthStore } from '~/stores/useAuthStore'

interface JwtPayload {
  roles?: string[]
  username?: string
  sub?: string
}

export class AuthService {
  /**
   * Hydrates the auth store from the access token cookie.
   */
  syncAuthState() {
    if (import.meta.client) {
      // The httpOnly cookie is not exposed to the browser, therefore we keep the state provided by SSR.
      return
    }
    const config = useRuntimeConfig()
    const token = useCookie<string | null>(config.tokenCookieName)
    const authStore = useAuthStore()

    if (token.value) {
      try {
        const decoded = jwtDecode<JwtPayload>(token.value)
        authStore.$patch({
          roles: decoded.roles ?? [],
          isLoggedIn: true,
          username: decoded.username ?? decoded.sub ?? null,
        })
      } catch (err) {
        console.error('Failed to decode JWT', err)
        authStore.$patch({ roles: [], isLoggedIn: false, username: null })
      }
    } else {
      authStore.$patch({ roles: [], isLoggedIn: false, username: null })
    }
  }

  async login(username: string, password: string) {
    const tokens = await $fetch<{ accessToken: string; refreshToken: string }>('/auth/login', {
      method: 'POST',
      body: { username, password },
      credentials: 'include',
    })
    // Decode the access token and update the store reactively
    const decoded = jwtDecode<JwtPayload>(tokens.accessToken)
    const authStore = useAuthStore()
    authStore.$patch({
      roles: decoded.roles ?? [],
      isLoggedIn: true,
      username: decoded.username ?? decoded.sub ?? null,
    })
    return tokens
  }

  /**
   * Request a new access token using the refresh token cookie.
   */
  async refresh() {
    const tokens = await $fetch<{ accessToken: string; refreshToken: string }>('/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    })
    // Decode the refreshed access token and patch the auth store
    const decoded = jwtDecode<JwtPayload>(tokens.accessToken)
    const authStore = useAuthStore()
    authStore.$patch({
      roles: decoded.roles ?? [],
      isLoggedIn: true,
      username: decoded.username ?? decoded.sub ?? null,
    })
    return tokens
  }

  async logout() {
    await $fetch('/auth/logout', {
      method: 'POST',
      credentials: 'include',
    })

    const authStore = useAuthStore()
    authStore.$reset()

    const config = useRuntimeConfig()
    const tokenCookie = useCookie<string | null>(config.tokenCookieName)
    const refreshCookie = useCookie<string | null>(config.refreshCookieName)
    tokenCookie.value = null
    refreshCookie.value = null
  }
}

export const authService = new AuthService()
