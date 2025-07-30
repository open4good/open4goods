export class AuthService {
  async login(username: string, password: string) {
    return await $fetch('/api/auth/login', {
      method: 'POST',
      body: { username, password },
    })
  }
}

export const authService = new AuthService()
