import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'

const runtimeConfigMock = vi.hoisted(() => ({
  public: { googleSsoEnabled: false },
}))

vi.mock('#app', () => ({
  useRuntimeConfig: () => runtimeConfigMock,
}))

vi.mock('#imports', async importOriginal => ({
  ...(await importOriginal<Record<string, unknown>>()),
  useRuntimeConfig: () => runtimeConfigMock,
}))

vi.mock('nuxt/app', async importOriginal => ({
  ...(await importOriginal<Record<string, unknown>>()),
  useRuntimeConfig: () => runtimeConfigMock,
}))
vi.mock('#app/nuxt', async importOriginal => ({
  ...(await importOriginal<Record<string, unknown>>()),
  useRuntimeConfig: () => runtimeConfigMock,
}))

vi.mock('~~/shared/api-client/services/auth.services', () => ({
  authService: { login: vi.fn(), logout: vi.fn() },
}))

vi.mock('~/stores/useAuthStore', () => ({
  useAuthStore: () => ({ $patch: vi.fn() }),
}))

describe('auth/login page', () => {
  it('hides the Google sign-in button when the flag is disabled', async () => {
    runtimeConfigMock.public.googleSsoEnabled = false
    const LoginPage = (await import('./login.vue')).default

    const wrapper = await mountSuspended(LoginPage)

    expect(wrapper.find('[data-testid="google-sign-in"]').exists()).toBe(
      false
    )
  })

  it('shows the Google sign-in button, linked to the server route, when the flag is enabled', async () => {
    runtimeConfigMock.public.googleSsoEnabled = true
    const LoginPage = (await import('./login.vue')).default

    const wrapper = await mountSuspended(LoginPage)

    const button = wrapper.find('[data-testid="google-sign-in"]')
    expect(button.exists()).toBe(true)
    expect(button.attributes('href')).toBe('/auth/google?redirect=%2F')
  })
})
