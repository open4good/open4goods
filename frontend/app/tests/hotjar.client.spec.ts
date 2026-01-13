import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  HOTJAR_RECORDING_COOKIE_NAME,
  HOTJAR_RECORDING_COOKIE_VALUE,
} from '~~/shared/utils/hotjar-recording'

type VueApp = {
  use: (plugin: unknown, options?: unknown) => void
}

type NuxtAppStub = {
  vueApp: VueApp
}

// Mock useCookie
const useCookieMock = vi.hoisted(() => vi.fn())
mockNuxtImport('useCookie', () => useCookieMock)

// Mock useRuntimeConfig
const runtimeConfigMock = vi.hoisted(() => ({
  value: {
    public: {
      hotjar: {
        enabled: false,
        siteId: 0,
        snippetVersion: 6,
      },
    },
  },
}))
mockNuxtImport('useRuntimeConfig', () => () => runtimeConfigMock.value)

// Mock defineNuxtPlugin (pass through)
mockNuxtImport('defineNuxtPlugin', () => (plugin: unknown) => plugin)

const isDoNotTrackEnabledMock = vi.hoisted(() => vi.fn())
const hotjarPlugin = vi.hoisted(() => ({ install: vi.fn() }))

vi.mock('vue-hotjar', () => ({
  default: hotjarPlugin,
}))

vi.mock('~/utils/do-not-track', () => ({
  isDoNotTrackEnabled: isDoNotTrackEnabledMock,
}))

describe('hotjar client plugin', () => {
  const createNuxtApp = (): NuxtAppStub => ({
    vueApp: {
      use: vi.fn(),
    },
  })

  const setRuntimeConfig = (config: unknown) => {
    runtimeConfigMock.value = config
  }

  beforeEach(() => {
    vi.resetModules()
    // Reset config to default disabled state
    runtimeConfigMock.value = {
      public: {
        hotjar: {
          enabled: false,
          siteId: 0,
          snippetVersion: 6,
        },
      },
    }
    useCookieMock.mockReset()
    isDoNotTrackEnabledMock.mockReset()
    isDoNotTrackEnabledMock.mockReturnValue(false)
  })

  it('initializes Hotjar when cookie is present and enabled', async () => {
    const nuxtApp = createNuxtApp()
    setRuntimeConfig({
      public: {
        hotjar: {
          enabled: true,
          siteId: 123456,
          snippetVersion: 6,
        },
      },
    })
    useCookieMock.mockImplementation(name => {
      if (name === HOTJAR_RECORDING_COOKIE_NAME) {
        return { value: HOTJAR_RECORDING_COOKIE_VALUE }
      }
      return { value: undefined }
    })

    const plugin = (await import('../plugins/hotjar.client')).default

    plugin.setup?.(nuxtApp)

    expect(nuxtApp.vueApp.use).toHaveBeenCalledWith(hotjarPlugin, {
      id: 123456,
      isProduction: true,
      snippetVersion: 6,
    })
  })

  it('skips Hotjar when the cookie is missing', async () => {
    const nuxtApp = createNuxtApp()
    setRuntimeConfig({
      public: {
        hotjar: {
          enabled: true,
          siteId: 123456,
          snippetVersion: 6,
        },
      },
    })
    useCookieMock.mockReturnValue({ value: undefined })

    const plugin = (await import('../plugins/hotjar.client')).default

    plugin.setup?.(nuxtApp)

    expect(nuxtApp.vueApp.use).not.toHaveBeenCalled()
  })

  it('skips Hotjar when Do Not Track is enabled', async () => {
    const nuxtApp = createNuxtApp()
    setRuntimeConfig({
      public: {
        hotjar: {
          enabled: true,
          siteId: 123456,
          snippetVersion: 6,
        },
      },
    })
    useCookieMock.mockReturnValue({ value: HOTJAR_RECORDING_COOKIE_VALUE })
    isDoNotTrackEnabledMock.mockReturnValue(true)

    const plugin = (await import('../plugins/hotjar.client')).default

    plugin.setup?.(nuxtApp)

    expect(nuxtApp.vueApp.use).not.toHaveBeenCalled()
  })
})
