import { defineStore } from 'pinia'
import type { JwtPayload } from 'jwt-decode'

interface AuthState {
  token: string | null
  payload: JwtPayload | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: null,
    payload: null,
  }),
  getters: {
    isAuthenticated: state => !!state.token,
  },
  actions: {
    /**
     * Save the decoded JWT payload and raw token in store
     */
    setAuth(token: string, payload: JwtPayload) {
      this.token = token
      this.payload = payload
    },
    /**
     * Clear any authentication information from store
     */
    resetAuth() {
      this.token = null
      this.payload = null
    },
  },
})
