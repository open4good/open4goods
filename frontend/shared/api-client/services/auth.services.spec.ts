import { createPinia, setActivePinia } from 'pinia'
import { afterAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '~/stores/useAuthStore'

const runtimeConfig = {
  tokenCookieName: 'access_token',
  refreshCookieName: 'refresh_token',
}

vi.mock('#app', () => ({
  useRuntimeConfig: () => runtimeConfig,
  useCookie: () => ({ value: null }),
}))
vi.mock('#imports', () => ({
  useRuntimeConfig: () => runtimeConfig,
  useCookie: () => ({ value: null }),
}))
vi.mock('nuxt/app', () => ({
  useRuntimeConfig: () => runtimeConfig,
  useCookie: () => ({ value: null }),
}))

const fetchMock = vi.fn()
vi.stubGlobal('$fetch', fetchMock)

let authService: typeof import('./auth.services')['authService']

describe('AuthService.logout', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    fetchMock.mockReset()
    fetchMock.mockResolvedValue(undefined)
    vi.resetModules()
    ;({ authService } = await import('./auth.services'))
  })

  it('clears authentication state and cookies after logout', async () => {
    const authStore = useAuthStore()
    authStore.$patch({
      isLoggedIn: true,
      roles: ['ROLE_USER'],
      username: 'john',
    })

    await authService.logout()

    expect(fetchMock).toHaveBeenCalledWith('/auth/logout', {
      method: 'POST',
      credentials: 'include',
    })
    expect(authStore.isLoggedIn).toBe(false)
    expect(authStore.roles).toEqual([])
    expect(authStore.username).toBeNull()
  })
})

afterAll(() => {
  vi.unstubAllGlobals()
})
