import { jwtDecode } from 'jwt-decode'

interface JwtPayload {
  roles?: string[]
}

export const useUserRoles = () => {
  const config = useRuntimeConfig()
  const token = useCookie<string | null>(config.tokenCookieName)

  const roles = computed<string[]>(() => {
    if (!token.value) return []
    try {
      const decoded = jwtDecode<JwtPayload>(token.value)
      return decoded.roles ?? []
    } catch (err) {
      console.error('Failed to decode JWT', err)
      return []
    }
  })

  return { roles }
}
