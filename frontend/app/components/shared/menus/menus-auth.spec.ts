import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import type { Ref } from 'vue'
import { computed, reactive, ref } from 'vue'
import {
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  it,
  vi,
  type MockInstance,
} from 'vitest'

import type { ThemeName } from '~~/shared/constants/theme'

const isLoggedIn = ref(false)
const logoutMock = vi.fn()
const username = ref<string | null>(null)
const roles = ref<string[]>([])
const routerPush = vi.fn()
const routerReplace = vi.fn()
const routerInstance = {
  push: routerPush,
  replace: routerReplace,
}
const currentRoute = reactive({ path: '/', fullPath: '/' })

const themeName = ref<ThemeName>('light')
const storedThemePreference = ref<ThemeName>('light')
function createUseStorageMock(): MockInstance<
  (key: string, defaultValue: ThemeName) => Ref<ThemeName>
> {
  return vi.fn((_: string, defaultValue: ThemeName) => {
    if (!storedThemePreference.value) {
      storedThemePreference.value = defaultValue
    }

    return storedThemePreference as Ref<ThemeName>
  })
}
function createUseCookieMock(): MockInstance<
  (key: string) => Ref<ThemeName | null>
> {
  return vi.fn((_: string) => themeCookiePreference as Ref<ThemeName | null>)
}
function getStorageMockRegistry() {
  return globalThis as {
    __menusAuthUseStorageMock__?: ReturnType<typeof createUseStorageMock>
  }
}
function ensureCookieMock() {
  const registry = getCookieMockRegistry()

  if (!registry.__menusAuthUseCookieMock__) {
    registry.__menusAuthUseCookieMock__ = createUseCookieMock()
  }

  return registry.__menusAuthUseCookieMock__
}
function getCookieMockRegistry() {
  return globalThis as {
    __menusAuthUseCookieMock__?: ReturnType<typeof createUseCookieMock>
  }
}
const themeCookiePreference = ref<ThemeName | null>(null)
const preferredDarkMock = ref(false)

const fetchMock = vi.fn()
const fetchCategoriesMock = vi.fn().mockResolvedValue([])
const categoriesLoadingMock = ref(false)
const fetchNavigationMock = vi.fn().mockResolvedValue(null)
const categoryNavigationMock = ref(null)
const navigationLoadingMock = ref(false)
const useMenuSearchControlsSpy = vi.fn()
const menuSearchControlsInstances: Record<string, unknown>[] = []

function useCategoriesComposable() {
  return {
    categories: computed(() => []),
    fetchCategories: fetchCategoriesMock,
    loading: categoriesLoadingMock,
    error: computed(() => null),
    activeCategoryId: computed(() => null),
    currentCategory: computed(() => null),
    clearError: vi.fn(),
    resetCategorySelection: vi.fn(),
  }
}

function useCategoryNavigationComposable() {
  return {
    navigation: categoryNavigationMock,
    loading: navigationLoadingMock,
    error: computed(() => null),
    fetchNavigation: fetchNavigationMock,
  }
}

const vMenuStub = {
  template:
    '<div class="v-menu-stub"><slot name="activator" :props="{}" /><div class="v-menu-stub__content"><slot /></div></div>',
}

function useRouteMock() {
  return currentRoute
}

function useRouterMock() {
  return routerInstance
}

;(globalThis as { useRoute?: typeof useRouteMock }).useRoute = useRouteMock
;(globalThis as { useRouter?: typeof useRouterMock }).useRouter = useRouterMock

vi.mock('~/composables/useAuth', () => ({
  useAuth: () => ({
    isLoggedIn,
    logout: logoutMock,
    username,
    roles,
  }),
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
    locale: ref('en-US'),
  }),
}))

vi.mock('vuetify', () => ({
  useTheme: () => ({
    global: {
      name: themeName,
    },
  }),
}))

vi.mock('@vueuse/core', () => {
  const registry = getStorageMockRegistry()
  const localUseStorageMock = createUseStorageMock()
  registry.__menusAuthUseStorageMock__ = localUseStorageMock

  return {
    usePreferredDark: () => preferredDarkMock,
    useStorage: localUseStorageMock,
  }
})

vi.mock('~/composables/categories/useCategories', () => ({
  useCategories: useCategoriesComposable,
}))

vi.mock('~/composables/categories/useCategoryNavigation', () => ({
  useCategoryNavigation: useCategoryNavigationComposable,
}))

const installPromptVisible = ref(false)
const isInstallSupported = ref(false)
const installInProgress = ref(false)
const requestInstallMock = vi.fn()

vi.mock('~/composables/pwa/usePwaInstallPromptBridge', () => ({
  usePwaInstallPromptBridge: () => ({
    installPromptVisible,
    isInstallSupported,
    installInProgress,
    requestInstall: requestInstallMock,
  }),
}))

const useStorageMock = getStorageMockRegistry()
  .__menusAuthUseStorageMock__ as ReturnType<typeof createUseStorageMock>

type NuxtImports = typeof import('#imports')

vi.mock('#imports', async () => {
  const actual = await vi.importActual<NuxtImports>('#imports')
  const cookieMock = ensureCookieMock()

  return {
    ...actual,
    useRoute: useRouteMock,
    useRouter: useRouterMock,
    useCookie: cookieMock,
    useNuxtApp: () => ({
      $fetch: (...args: unknown[]) => fetchMock(...args),
    }),
  }
})

vi.mock('#app', () => {
  const cookieMock = ensureCookieMock()

  return {
    useRoute: useRouteMock,
    useRouter: useRouterMock,
    useCookie: cookieMock,
    useNuxtApp: () => ({
      $fetch: (...args: unknown[]) => fetchMock(...args),
    }),
  }
})

vi.mock('nuxt/app', () => {
  const cookieMock = ensureCookieMock()

  return {
    useRoute: useRouteMock,
    useRouter: useRouterMock,
    useCookie: cookieMock,
    useNuxtApp: () => ({
      $fetch: (...args: unknown[]) => fetchMock(...args),
    }),
  }
})

vi.mock('~/composables/menus/useMenuSearchControls', async () => {
  const actual = await vi.importActual<
    typeof import('~/composables/menus/useMenuSearchControls')
  >('~/composables/menus/useMenuSearchControls')

  const useMenuSearchControlsMock = vi.fn((...args) => {
    useMenuSearchControlsSpy(...args)
    const result = actual.useMenuSearchControls(...args)
    menuSearchControlsInstances.push(result)
    return result
  })

  return {
    ...actual,
    useMenuSearchControls: useMenuSearchControlsMock,
  }
})

const useCookieMock = ensureCookieMock()

vi.mock('vue-router', () => ({
  useRoute: useRouteMock,
  useRouter: useRouterMock,
}))

let TheHeroMenu: (typeof import('./The-hero-menu.vue'))['default']
let TheMobileMenu: (typeof import('./The-mobile-menu.vue'))['default']

beforeAll(async () => {
  TheHeroMenu = (await import('./The-hero-menu.vue')).default
  TheMobileMenu = (await import('./The-mobile-menu.vue')).default
})

describe('Shared menu authentication controls', () => {
  let reloadSpy: ReturnType<typeof vi.spyOn> | undefined
  const heroMountOptions = {
    attachTo: document.body,
    global: {
      stubs: {
        VMenu: vMenuStub,
        SearchSuggestField: {
          template: '<div class="search-suggest-field-stub" />',
        },
      },
    },
  }
  const mobileMountOptions = {
    global: {
      stubs: {
        VMenu: vMenuStub,
        SearchSuggestField: {
          template: '<div class="search-suggest-field-stub" />',
        },
      },
    },
  }

  beforeEach(() => {
    isLoggedIn.value = false
    logoutMock.mockReset()
    routerPush.mockReset()
    routerReplace.mockReset()
    logoutMock.mockResolvedValue(undefined)
    themeName.value = 'light'
    storedThemePreference.value = 'light'
    themeCookiePreference.value = null
    preferredDarkMock.value = false
    useStorageMock.mockClear()
    useCookieMock.mockClear()
    fetchMock.mockReset()
    fetchCategoriesMock.mockClear()
    fetchNavigationMock.mockClear()
    username.value = null
    roles.value = []
    categoryNavigationMock.value = null
    navigationLoadingMock.value = false
    installPromptVisible.value = false
    isInstallSupported.value = false
    installInProgress.value = false
    requestInstallMock.mockReset()
    useMenuSearchControlsSpy.mockClear()
    menuSearchControlsInstances.length = 0
    currentRoute.path = '/'
    currentRoute.fullPath = '/'

    if (reloadSpy) {
      reloadSpy.mockRestore()
      reloadSpy = undefined
    }

    if (typeof window !== 'undefined') {
      reloadSpy = vi
        .spyOn(window.location, 'reload')
        .mockImplementation(() => undefined)

      if (!('visualViewport' in window)) {
        ;(
          window as unknown as {
            visualViewport?: Pick<
              NonNullable<Window['visualViewport']>,
              'addEventListener' | 'removeEventListener'
            >
          }
        ).visualViewport = {
          addEventListener: () => undefined,
          removeEventListener: () => undefined,
        }
      }
    }

    ;(
      globalThis as { $fetch?: (...args: unknown[]) => Promise<unknown> }
    ).$fetch = (...args: unknown[]) => fetchMock(...args)
  })

  afterEach(() => {
    reloadSpy?.mockRestore()
    reloadSpy = undefined
    delete (globalThis as { $fetch?: unknown }).$fetch
  })

  it('does not render account controls when logged out', async () => {
    const heroWrapper = await mountSuspended(TheHeroMenu, heroMountOptions)
    const mobileWrapper = await mountSuspended(
      TheMobileMenu,
      mobileMountOptions
    )

    expect(
      heroWrapper.find('[data-testid="hero-account-menu-activator"]').exists()
    ).toBe(false)
    expect(mobileWrapper.find('[data-testid="mobile-logout"]').exists()).toBe(
      false
    )
    expect(
      mobileWrapper.find('[data-testid="mobile-clear-cache"]').exists()
    ).toBe(false)
  })

  it('renders the mobile install CTA when available', async () => {
    installPromptVisible.value = true
    isInstallSupported.value = true

    const wrapper = await mountSuspended(TheMobileMenu, mobileMountOptions)
    const installButton = wrapper.get('[data-testid="mobile-install-cta"]')

    await installButton.trigger('click')
    await flushPromises()

    expect(requestInstallMock).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('reuses the shared menu search composable in hero and mobile menus', async () => {
    const heroWrapper = await mountSuspended(TheHeroMenu, heroMountOptions)
    expect(heroWrapper.exists()).toBe(true)

    const mobileWrapper = await mountSuspended(
      TheMobileMenu,
      mobileMountOptions
    )
    expect(mobileWrapper.exists()).toBe(true)

    expect(useMenuSearchControlsSpy).toHaveBeenCalledTimes(2)
    expect(useMenuSearchControlsSpy.mock.calls[0]?.[0]).toBeUndefined()
    expect(useMenuSearchControlsSpy.mock.calls[1]?.[0]).toMatchObject({
      onNavigate: expect.any(Function),
    })
  })

  it('logs out and redirects from the hero menu when clicked', async () => {
    isLoggedIn.value = true
    username.value = 'Jane Doe'
    roles.value = ['admin']

    const wrapper = await mountSuspended(TheHeroMenu, heroMountOptions)

    const logoutItem = wrapper.get('[data-testid="hero-account-logout"]')
    await logoutItem.trigger('click')
    await flushPromises()

    expect(logoutMock).toHaveBeenCalledTimes(1)
  })

  it('logs out, redirects and closes the drawer from the mobile menu', async () => {
    isLoggedIn.value = true
    username.value = 'Jane Doe'
    roles.value = ['admin']

    const wrapper = await mountSuspended(TheMobileMenu, mobileMountOptions)
    const logoutItem = wrapper.get('[data-testid="mobile-logout"]')

    await logoutItem.trigger('click')
    await flushPromises()

    expect(logoutMock).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('hides the hero search when navigating back to the home route', async () => {
    currentRoute.path = '/products'
    currentRoute.fullPath = '/products'

    const startIndex = menuSearchControlsInstances.length
    await mountSuspended(TheHeroMenu, heroMountOptions)
    const heroControls = menuSearchControlsInstances[startIndex] as {
      showMenuSearch: Ref<boolean>
    }

    currentRoute.path = '/'
    currentRoute.fullPath = '/'
    await flushPromises()
    await flushPromises()

    expect(heroControls?.showMenuSearch.value).toBe(false)
  })

  it('clears the mobile search query through the shared handler', async () => {
    currentRoute.path = '/products'
    currentRoute.fullPath = '/products'

    const startIndex = menuSearchControlsInstances.length
    await mountSuspended(TheMobileMenu, mobileMountOptions)
    const mobileControls = menuSearchControlsInstances[startIndex] as {
      searchQuery: Ref<string>
      handleSearchClear: () => void
    }

    expect(mobileControls).toBeTruthy()

    mobileControls.searchQuery.value = 'impact score'
    mobileControls.handleSearchClear()

    expect(mobileControls.searchQuery.value).toBe('')
  })

  it('clears the caches from the hero menu and reloads the page', async () => {
    isLoggedIn.value = true
    username.value = 'Cache Admin'
    roles.value = ['maintainer']
    fetchMock.mockResolvedValue({ success: true })

    const wrapper = await mountSuspended(TheHeroMenu, heroMountOptions)

    const clearItem = wrapper.get('[data-testid="hero-clear-cache"]')
    await clearItem.trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/admin/cache/reset',
      expect.objectContaining({ method: 'POST' })
    )
    expect(reloadSpy).toHaveBeenCalledTimes(1)
  })

  it('clears the caches from the mobile menu and reloads the page', async () => {
    isLoggedIn.value = true
    username.value = 'Cache Admin'
    roles.value = ['maintainer']
    fetchMock.mockResolvedValue({ success: true })

    const wrapper = await mountSuspended(TheMobileMenu, mobileMountOptions)
    const clearCacheItem = wrapper.get('[data-testid="mobile-clear-cache"]')

    await clearCacheItem.trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/admin/cache/reset',
      expect.objectContaining({ method: 'POST' })
    )
    expect(reloadSpy).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('renders theme toggles and synchronises the stored preference', async () => {
    const heroWrapper = await mountSuspended(TheHeroMenu)

    expect(heroWrapper.find('[data-testid="hero-theme-toggle"]').exists()).toBe(
      true
    )

    const darkToggle = heroWrapper.get('[data-testid="hero-theme-toggle-dark"]')

    await darkToggle.trigger('click')
    await flushPromises()

    expect(storedThemePreference.value).toBe('dark')
    expect(themeName.value).toBe('dark')

    const mobileWrapper = await mountSuspended(TheMobileMenu)

    expect(
      mobileWrapper.find('[data-testid="mobile-theme-toggle"]').exists()
    ).toBe(true)

    const lightToggle = mobileWrapper.get(
      '[data-testid="mobile-theme-toggle-light"]'
    )

    await lightToggle.trigger('click')
    await flushPromises()

    expect(themeName.value).toBe('light')
    expect(storedThemePreference.value).toBe('light')
  })
})
