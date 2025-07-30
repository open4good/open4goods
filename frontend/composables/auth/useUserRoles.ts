/**
 * Decode user roles from the JWT stored in cookies
 */
export const useUserRoles = () => {
  const jwtCookie = useCookie<string | undefined>('jwt')
  const roles = computed(() => {
    const token = jwtCookie.value
    if (!token) {
      return [] as string[]
    }
    try {
      const payload = JSON.parse(atob(token.split('.')[1] ?? '')) as {
        roles?: string[]
      }
      return payload.roles ?? []
    } catch (err) {
      console.error('Failed to decode JWT', err)
      return [] as string[]
    }
  })

  return {
    roles,
  }
}
