import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import type { Ref } from 'vue'
import { reactive, ref } from 'vue'
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'

const isLoggedIn = ref(false)
const logoutMock = vi.fn()
const routerPush = vi.fn()
const routerReplace = vi.fn()
const routerInstance = {
  push: routerPush,
  replace: routerReplace,
}
const currentRoute = reactive({ path: '/', fullPath: '/' })

type ThemeName = 'light' | 'dark'

const themeName = ref<ThemeName>('light')
const storedThemePreference = ref<ThemeName | undefined>()
const preferredDarkMock = ref(false)

const useStorageMock = vi.fn((_: string, defaultValue: ThemeName) => {
  if (!storedThemePreference.value) {
    storedThemePreference.value = defaultValue
  }

  return storedThemePreference as Ref<ThemeName>
})

function useRouteMock() {
  return currentRoute
}

function useRouterMock() {
  return routerInstance
}

vi.mock('~/composables/useAuth', () => ({
  useAuth: () => ({
    isLoggedIn,
    logout: logoutMock,
  }),
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('vuetify', () => ({
  useTheme: () => ({
    global: {
      name: themeName,
    },
  }),
}))

vi.mock('@vueuse/core', () => ({
  usePreferredDark: () => preferredDarkMock,
  useStorage: useStorageMock,
}))

type NuxtImports = typeof import('#imports')

vi.mock('#imports', async () => {
  const actual = await vi.importActual<NuxtImports>('#imports')

  return {
    ...actual,
    useRoute: useRouteMock,
    useRouter: useRouterMock,
  }
})

vi.mock('#app', () => ({
  useRoute: useRouteMock,
  useRouter: useRouterMock,
}))

vi.mock('nuxt/app', () => ({
  useRoute: useRouteMock,
  useRouter: useRouterMock,
}))

vi.mock('vue-router', () => ({
  useRoute: useRouteMock,
  useRouter: useRouterMock,
}))

let TheHeroMenu: typeof import('./The-hero-menu.vue')['default']
let TheMobileMenu: typeof import('./The-mobile-menu.vue')['default']

beforeAll(async () => {
  TheHeroMenu = (await import('./The-hero-menu.vue')).default
  TheMobileMenu = (await import('./The-mobile-menu.vue')).default
})

describe('Shared menu authentication controls', () => {
  beforeEach(() => {
    isLoggedIn.value = false
    logoutMock.mockReset()
    routerPush.mockReset()
    routerReplace.mockReset()
    logoutMock.mockResolvedValue(undefined)
    themeName.value = 'light'
    storedThemePreference.value = undefined
    preferredDarkMock.value = false
    useStorageMock.mockClear()
  })

  it('does not render logout controls when logged out', async () => {
    const heroWrapper = await mountSuspended(TheHeroMenu)
    const mobileWrapper = await mountSuspended(TheMobileMenu)

    expect(heroWrapper.find('[data-testid="hero-logout"]').exists()).toBe(false)
    expect(mobileWrapper.find('[data-testid="mobile-logout"]').exists()).toBe(false)
  })

  it('logs out and redirects from the hero menu when clicked', async () => {
    isLoggedIn.value = true

    const wrapper = await mountSuspended(TheHeroMenu)
    const button = wrapper.get('[data-testid="hero-logout"]')

    await button.trigger('click')
    await flushPromises()

    expect(logoutMock).toHaveBeenCalledTimes(1)
  })

  it('logs out, redirects and closes the drawer from the mobile menu', async () => {
    isLoggedIn.value = true

    const wrapper = await mountSuspended(TheMobileMenu)
    const logoutItem = wrapper.get('[data-testid="mobile-logout"]')

    await logoutItem.trigger('click')
    await flushPromises()

    expect(logoutMock).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('renders theme toggles and synchronises the stored preference', async () => {
    const heroWrapper = await mountSuspended(TheHeroMenu)

    expect(heroWrapper.find('[data-testid="hero-theme-toggle"]').exists()).toBe(true)

    const darkToggle = heroWrapper.get('[data-testid="hero-theme-toggle-dark"]')

    await darkToggle.trigger('click')
    await flushPromises()

    expect(themeName.value).toBe('dark')
    expect(storedThemePreference.value).toBe('dark')

    const mobileWrapper = await mountSuspended(TheMobileMenu)

    expect(mobileWrapper.find('[data-testid="mobile-theme-toggle"]').exists()).toBe(true)

    const lightToggle = mobileWrapper.get('[data-testid="mobile-theme-toggle-light"]')

    await lightToggle.trigger('click')
    await flushPromises()

    expect(themeName.value).toBe('light')
    expect(storedThemePreference.value).toBe('light')
  })
})
