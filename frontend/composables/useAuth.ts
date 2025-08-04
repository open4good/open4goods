import { storeToRefs } from 'pinia'
import { useAuthStore } from '~/stores/useAuthStore'

export const useAuth = () => {
  const authStore = useAuthStore()
  const { isLoggedIn } = storeToRefs(authStore)

  return {
    isLoggedIn,
    hasRole: authStore.hasRole,
  }
}
