import { defineStore } from 'pinia'

interface AuthState {
  roles: string[]
  isLoggedIn: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    roles: [],
    isLoggedIn: false,
  }),
  getters: {
    hasRole: (state) => (role: string) => state.roles.includes(role),
  },
})
