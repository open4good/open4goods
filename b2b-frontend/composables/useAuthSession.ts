export type AccessLevel = 'public' | 'user' | 'client' | 'admin'

export interface AuthSession {
  subject: string
  clientId: string
  level: AccessLevel
  roles: string[]
  email?: string
  name?: string
  picture?: string
}

interface OidcLoginPayload {
  provider: 'google' | string
  idToken: string
}

export function useAuthSession() {
  const session = useState<AuthSession | null>('auth-session', () => null)
  const ssrHeaders = import.meta.server ? useRequestHeaders(['cookie']) : {}
  const backendBaseUrl = resolveBackendBaseUrl()

  const fetchMe = async () => {
    try {
      const data = await $fetch<AuthSession>('/api/v1/auth/me', {
        baseURL: backendBaseUrl,
        credentials: 'include',
        headers: ssrHeaders
      })
      session.value = data
      return data
    } catch (err: unknown) {
      const error = err as { status?: number; message?: string }
      const message = error.message || String(error)
      if (error.status !== 401) {
        console.warn('[auth/session] Unable to resolve /auth/me session claims', message)
      }

      // Only clear session if it's a 401 (Unauthorized) or if we are on server
      // Transient network errors on client shouldn't wipe out the hydrated session
      if (import.meta.server || error.status === 401) {
        session.value = null
      }
      return null
    }
  }

  const loginWithOidc = async (payload: OidcLoginPayload) => {
    await $fetch('/api/v1/auth/oidc', {
      method: 'POST',
      baseURL: backendBaseUrl,
      credentials: 'include',
      body: payload,
      headers: ssrHeaders
    })
    return fetchMe()
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
    loginWithOidc,
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
