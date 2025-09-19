import { storeToRefs } from 'pinia'
import { useAuthStore } from '~/stores/useAuthStore'

export const useAuth = () => {
  const authStore = useAuthStore()
  const { isLoggedIn, username, roles } = storeToRefs(authStore)

  return {
    isLoggedIn,
    username,
    roles,
    hasRole: authStore.hasRole,
  }
}
