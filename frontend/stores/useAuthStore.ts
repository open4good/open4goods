import { defineStore } from 'pinia'

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
})
