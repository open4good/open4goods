import { storeToRefs } from 'pinia'
import { useAuthStore } from '~/stores/useAuthStore'

export const useAuth = () => {
  const authStore = useAuthStore()
  const { isLoggedIn, username } = storeToRefs(authStore)

  return {
    isLoggedIn,
    username,
    hasRole: authStore.hasRole,
  }
}
