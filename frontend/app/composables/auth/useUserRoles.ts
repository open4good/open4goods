import { useAuthStore } from '~/stores/useAuthStore'

/**
 * Returns user roles from the auth store.
 * Roles are populated server-side from JWT and kept in sync via refresh.
 */
export const useUserRoles = () => {
  const authStore = useAuthStore()

  const roles = computed<string[]>(() => authStore.roles)

  return { roles }
}
