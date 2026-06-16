export type OrgRole = 'OWNER' | 'ADMIN' | 'DEVELOPER' | 'BILLING'

export interface AuthUser {
  id: string
  email: string
  displayName: string
  avatarUrl: string | null
  platformAdmin: boolean
}

export interface AuthOrganization {
  id: string
  name: string
  slug: string
  balanceCredits: number
}

export interface AuthSession {
  user: AuthUser
  organization: AuthOrganization | null
  role: OrgRole | null
}

interface RawAuthResponse {
  user?: AuthUser
  organization?: AuthOrganization
  role?: OrgRole
}

export function useAuthSession() {
  const session = useState<AuthSession | null>('auth-session', () => null)
  const ssrHeaders = import.meta.server ? useRequestHeaders(['cookie']) : {}
  const backendBaseUrl = resolveBackendBaseUrl()

  const fetchMe = async () => {
    try {
      const data = await $fetch<RawAuthResponse>('/api/v1/auth/me', {
        baseURL: backendBaseUrl,
        credentials: 'include',
        headers: ssrHeaders
      })
      if (data?.user) {
        session.value = {
          user: data.user,
          organization: data.organization ?? null,
          role: data.role ?? null
        }
        return session.value
      }
      session.value = null
      return null
    } catch (err: unknown) {
      const error = err as { status?: number; message?: string }
      const message = error.message || String(error)
      if (error.status !== 401) {
        console.warn('[auth/session] Unable to resolve /auth/me session claims', message)
      }
      if (import.meta.server || error.status === 401) {
        session.value = null
      }
      return null
    }
  }

  const logout = async () => {
    await $fetch('/api/v1/auth/logout', {
      method: 'POST',
      baseURL: backendBaseUrl,
      credentials: 'include',
      headers: ssrHeaders
    })
    session.value = null
  }

  return {
    session,
    fetchMe,
    logout
  }
}

function resolveBackendBaseUrl() {
  const baseUrl = useRuntimeConfig().public.backendBaseUrl
  if (import.meta.server) {
    return resolveServerRuntimeBaseUrl(baseUrl, useRequestURL().origin)
  }
  return resolveRuntimeUrl(baseUrl)
}
