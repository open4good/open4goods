import { defineStore } from 'pinia'
import { useAuthCookies } from '~/utils/authCookies'

interface AuthState {
  roles: string[]
  isLoggedIn: boolean
  username: string | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    roles: [],
    isLoggedIn: false,
    username: null,
  }),
  getters: {
    hasRole: (state) => (role: string) => state.roles.includes(role),
  },
  actions: {
    async logout() {
      const { tokenCookie, refreshCookie } = useAuthCookies()
      const { authService } = await import('~~/shared/api-client/services/auth.services')
      await authService.logout()
      tokenCookie.value = null
      refreshCookie.value = null
    },
  },
})
