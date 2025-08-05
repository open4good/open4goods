/**
 * Authentication service handling login and token refresh calls.
 */
import { jwtDecode } from 'jwt-decode'
import { useAuthStore } from '~/stores/useAuthStore'
import { handleErrors } from '~/utils'

interface JwtPayload {
  roles?: string[]
  username?: string
  sub?: string
}

export const useAuthService = () => {
  /**
   * Hydrates the auth store from the access token cookie.
   */
  const syncAuthState = () => {
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
        authStore.$patch({ roles: [], isLoggedIn: false, username: null })
        handleErrors._handleError(err, 'Failed to decode JWT')
      }
    } else {
      authStore.$patch({ roles: [], isLoggedIn: false, username: null })
    }
  }

  const login = async (username: string, password: string) => {
    try {
      const tokens = await $fetch<{ accessToken: string; refreshToken: string }>(
        '/auth/login',
        {
          method: 'POST',
          body: { username, password },
          credentials: 'include',
        }
      )
      const decoded = jwtDecode<JwtPayload>(tokens.accessToken)
      const authStore = useAuthStore()
      authStore.$patch({
        roles: decoded.roles ?? [],
        isLoggedIn: true,
        username: decoded.username ?? decoded.sub ?? null,
      })
      return tokens
    } catch (error) {
      handleErrors._handleError(error, 'Login failed')
    }
  }

  /**
   * Request a new access token using the refresh token cookie.
   */
  const refresh = async () => {
    try {
      const tokens = await $fetch<{ accessToken: string; refreshToken: string }>(
        '/auth/refresh',
        {
          method: 'POST',
          credentials: 'include',
        }
      )
      const decoded = jwtDecode<JwtPayload>(tokens.accessToken)
      const authStore = useAuthStore()
      authStore.$patch({
        roles: decoded.roles ?? [],
        isLoggedIn: true,
        username: decoded.username ?? decoded.sub ?? null,
      })
      return tokens
    } catch (error) {
      handleErrors._handleError(error, 'Failed to refresh token')
    }
  }

  return { syncAuthState, login, refresh }
}
