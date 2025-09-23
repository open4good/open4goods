import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const logoutMock = vi.fn()

vi.mock('~~/shared/api-client/services/auth.services', () => ({
  authService: {
    logout: logoutMock,
  },
}))

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    logoutMock.mockReset()
  })

  it('delegates logout handling to the auth service', async () => {
    logoutMock.mockResolvedValue(undefined)
    const { useAuthStore } = await import('./useAuthStore')
    const store = useAuthStore()

    await store.logout()

    expect(logoutMock).toHaveBeenCalledTimes(1)
  })
})
