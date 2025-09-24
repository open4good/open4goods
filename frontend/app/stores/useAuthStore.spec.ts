import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const logoutMock = vi.fn()
const tokenCookie = { value: 'token' }
const refreshCookie = { value: 'refresh' }

vi.mock('~~/shared/api-client/services/auth.services', () => ({
  authService: {
    logout: logoutMock,
  },
}))

vi.mock('~/utils/authCookies', () => ({
  useAuthCookies: () => ({ tokenCookie, refreshCookie }),
}))

describe('useAuthStore', () => {
  beforeEach(() => {
    vi.resetModules()
    setActivePinia(createPinia())
    logoutMock.mockReset()
    tokenCookie.value = 'token'
    refreshCookie.value = 'refresh'
  })

  it('delegates logout handling to the auth service and clears cookies', async () => {
    logoutMock.mockResolvedValue(undefined)
    const { useAuthStore } = await import('./useAuthStore')
    const store = useAuthStore()

    await store.logout()

    expect(logoutMock).toHaveBeenCalledTimes(1)
    expect(tokenCookie.value).toBeNull()
    expect(refreshCookie.value).toBeNull()
  })
})
