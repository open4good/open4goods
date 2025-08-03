/**
 * Authentication service handling login and token refresh calls.
 */
export class AuthService {
  async login(username: string, password: string) {
    return await $fetch('/auth/login', {
      method: 'POST',
      body: { username, password },
      credentials: 'include',
    })
  }

  /**
   * Request a new access token using the refresh token cookie.
   */
  async refresh() {
    return await $fetch('/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    })
  }
}

export const authService = new AuthService()
