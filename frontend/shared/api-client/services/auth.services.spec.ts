import { createPinia, setActivePinia } from 'pinia'
import { afterAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '~/stores/useAuthStore'

const runtimeConfig = {
  tokenCookieName: 'access_token',
  refreshCookieName: 'refresh_token',
}

type CookieMock = { value: string | null }

const cookieStore = new Map<string, CookieMock>()
const useRuntimeConfigMock = vi.fn(() => runtimeConfig)
const useCookieMockImpl = (name: string) => {
  if (!cookieStore.has(name)) {
    cookieStore.set(name, { value: null })
  }
  return cookieStore.get(name) as CookieMock
}
const useCookieMock = vi.fn(useCookieMockImpl)

vi.mock('#app', async () => {
  const actual = await vi.importActual<typeof import('#app')>('#app')
  return {
    ...actual,
    useRuntimeConfig: useRuntimeConfigMock,
    useCookie: useCookieMock,
  }
})

vi.mock('#imports', async () => {
  const actual = await vi.importActual<typeof import('#imports')>('#imports')
  return {
    ...actual,
    useRuntimeConfig: useRuntimeConfigMock,
    useCookie: useCookieMock,
  }
})

vi.mock('nuxt/app', async () => {
  const actual = await vi.importActual<typeof import('nuxt/app')>('nuxt/app')
  return {
    ...actual,
    useRuntimeConfig: useRuntimeConfigMock,
    useCookie: useCookieMock,
  }
})

const fetchMock = vi.fn()
vi.stubGlobal('$fetch', fetchMock)
vi.stubGlobal('useRuntimeConfig', useRuntimeConfigMock)
vi.stubGlobal('useCookie', useCookieMock)

let authService: typeof import('./auth.services')['authService']

describe('AuthService.logout', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    fetchMock.mockReset()
    fetchMock.mockResolvedValue(undefined)
    useRuntimeConfigMock.mockReset()
    useRuntimeConfigMock.mockReturnValue(runtimeConfig)
    cookieStore.clear()
    useCookieMock.mockReset()
    useCookieMock.mockImplementation(useCookieMockImpl)
    ;({ authService } = await import('./auth.services'))
  })

  it('clears authentication state and cookies after logout', async () => {
    const authStore = useAuthStore()
    authStore.$patch({
      isLoggedIn: true,
      roles: ['ROLE_USER'],
      username: 'john',
    })

    const tokenCookie = { value: 'token-value' as string | null }
    const refreshCookie = { value: 'refresh-value' as string | null }
    useCookieMock
      .mockImplementationOnce(() => tokenCookie)
      .mockImplementationOnce(() => refreshCookie)
      .mockImplementationOnce(() => tokenCookie)
      .mockImplementationOnce(() => refreshCookie)
    useCookieMock(runtimeConfig.tokenCookieName)
    useCookieMock(runtimeConfig.refreshCookieName)

    await authService.logout()

    expect(fetchMock).not.toHaveBeenCalled()
    expect(useCookieMock).toHaveBeenCalledTimes(4)
    expect(useCookieMock).toHaveBeenNthCalledWith(1, runtimeConfig.tokenCookieName)
    expect(useCookieMock).toHaveBeenNthCalledWith(2, runtimeConfig.refreshCookieName)
    expect(useCookieMock).toHaveBeenNthCalledWith(3, runtimeConfig.tokenCookieName)
    expect(useCookieMock).toHaveBeenNthCalledWith(4, runtimeConfig.refreshCookieName)
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
