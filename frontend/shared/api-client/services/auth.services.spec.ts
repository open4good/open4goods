import { createPinia, setActivePinia } from 'pinia'
import { afterAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '~/stores/useAuthStore'

interface MockCookie {
  value: string | null
}

let tokenCookie: MockCookie = { value: null }
let refreshCookie: MockCookie = { value: null }

vi.mock('~/utils/authCookies', () => ({
  useAuthCookies: () => ({ tokenCookie, refreshCookie }),
}))

const fetchMock = vi.fn()
vi.stubGlobal('$fetch', fetchMock)

let authService: (typeof import('./auth.services'))['authService']

describe('AuthService.logout', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    fetchMock.mockReset()
    fetchMock.mockResolvedValue(undefined)
    vi.resetModules()
    tokenCookie = { value: 'token' }
    refreshCookie = { value: 'refresh' }
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

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledWith('/auth/logout', { method: 'POST' })
    expect(authStore.isLoggedIn).toBe(false)
    expect(authStore.roles).toEqual([])
    expect(authStore.username).toBeNull()
    expect(tokenCookie.value).toBeNull()
    expect(refreshCookie.value).toBeNull()
  })
})

afterAll(() => {
  vi.unstubAllGlobals()
})
