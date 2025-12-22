/**
 * Authentication service handling login and token refresh calls.
 */
import { jwtDecode } from 'jwt-decode'

interface JwtPayload {
  roles?: string[]
  username?: string
  sub?: string
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
}

export interface AuthStatePayload {
  roles: string[]
  isLoggedIn: boolean
  username: string | null
}

const decodeAuthStateFromToken = (
  tokenValue: string | null
): AuthStatePayload => {
  if (!tokenValue) {
    return { roles: [], isLoggedIn: false, username: null }
  }

  try {
    const decoded = jwtDecode<JwtPayload>(tokenValue)

    return {
      roles: decoded.roles ?? [],
      isLoggedIn: true,
      username: decoded.username ?? decoded.sub ?? null,
    }
  } catch (err) {
    console.error('Failed to decode JWT', err)
    return { roles: [], isLoggedIn: false, username: null }
  }
}

const login = async (username: string, password: string) => {
  const tokens = await $fetch<AuthTokens>('/auth/login', {
    method: 'POST',
    body: { username, password },
    credentials: 'include',
  })

  return {
    tokens,
    authState: decodeAuthStateFromToken(tokens.accessToken),
  }
}

/**
 * Request a new access token using the refresh token cookie.
 */
const refresh = async () => {
  const tokens = await $fetch<AuthTokens>('/auth/refresh', {
    method: 'POST',
    credentials: 'include',
  })

  return {
    tokens,
    authState: decodeAuthStateFromToken(tokens.accessToken),
  }
}

const logout = async () =>
  $fetch('/auth/logout', {
    method: 'POST',
  })

export const authService = {
  decodeAuthStateFromToken,
  login,
  refresh,
  logout,
}
