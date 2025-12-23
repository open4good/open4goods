/**
 * Authentication service handling login and token refresh calls.
 */
export interface AuthStatePayload {
  roles: string[]
  isLoggedIn: boolean
  username: string | null
}

const login = async (username: string, password: string) => {
  const authState = await $fetch<AuthStatePayload>('/auth/login', {
    method: 'POST',
    body: { username, password },
    credentials: 'include',
  })

  return authState
}

/**
 * Request a new access token using the refresh token cookie.
 */
const refresh = async () => {
  const authState = await $fetch<AuthStatePayload>('/auth/refresh', {
    method: 'POST',
    credentials: 'include',
  })

  return authState
}

const logout = async () =>
  $fetch('/auth/logout', {
    method: 'POST',
  })

export const authService = {
  login,
  refresh,
  logout,
}
