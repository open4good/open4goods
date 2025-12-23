import { jwtDecode } from 'jwt-decode'

interface JwtPayload {
  roles?: string[]
  username?: string
  sub?: string
  exp?: number
}

export interface AuthStatePayload {
  roles: string[]
  isLoggedIn: boolean
  username: string | null
}

/**
 * Decodes JWT token server-side and extracts auth state.
 * This should ONLY be used on the server to prevent exposing JWT decoding to client.
 */
export const decodeAuthStateFromToken = (
  tokenValue: string | null | undefined
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

/**
 * Checks if JWT token is expired.
 * This should ONLY be used on the server.
 */
export const isTokenExpired = (
  tokenValue: string | null | undefined
): boolean => {
  if (!tokenValue) return true

  try {
    const { exp } = jwtDecode<JwtPayload>(tokenValue)
    if (!exp) return false
    return exp * 1000 < Date.now()
  } catch (err) {
    console.error('Failed to decode JWT for expiration check', err)
    return true
  }
}
