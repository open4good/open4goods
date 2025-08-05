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
  private syncAuthState() {
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
    const res = await $fetch('/auth/login', {
      method: 'POST',
      body: { username, password },
      credentials: 'include',
    })
    this.syncAuthState()
    return res
  }

  /**
   * Request a new access token using the refresh token cookie.
   */
  async refresh() {
    const res = await $fetch('/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    })
    this.syncAuthState()
    return res
  }
}

export const authService = new AuthService()
