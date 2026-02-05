import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi, afterEach } from 'vitest'

// Mock defineNuxtPlugin to just return the setup function
mockNuxtImport('defineNuxtPlugin', () => (plugin: any) => plugin)

describe('error-handler plugin', () => {
  const consoleGroupMock = vi
    .spyOn(console, 'group')
    .mockImplementation(() => {})
  const consoleErrorMock = vi
    .spyOn(console, 'error')
    .mockImplementation(() => {})
  const consoleGroupEndMock = vi
    .spyOn(console, 'groupEnd')
    .mockImplementation(() => {})

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('registers vue:error and app:error hooks', async () => {
    const nuxtApp = {
      hook: vi.fn(),
    }

    const plugin = (await import('../plugins/error-handler')).default
    plugin(nuxtApp)

    expect(nuxtApp.hook).toHaveBeenCalledWith('vue:error', expect.any(Function))
    expect(nuxtApp.hook).toHaveBeenCalledWith('app:error', expect.any(Function))
  })

  it('logs error to console when vue:error hook is triggered', async () => {
    let vueErrorCallback: Function | undefined
    const nuxtApp = {
      hook: vi.fn((name, callback) => {
        if (name === 'vue:error') {
          vueErrorCallback = callback
        }
      }),
    }

    const plugin = (await import('../plugins/error-handler')).default
    plugin(nuxtApp)

    expect(vueErrorCallback).toBeDefined()

    const error = new Error('Test Vue Error')
    const instance = { name: 'TestComponent' }
    const info = 'render function'

    vueErrorCallback!(error, instance, info)

    expect(consoleGroupMock).toHaveBeenCalledWith('Global Vue Error Handler')
    expect(consoleErrorMock).toHaveBeenCalledWith('Error:', error)
    expect(consoleErrorMock).toHaveBeenCalledWith(
      'Component Instance:',
      instance
    )
    expect(consoleErrorMock).toHaveBeenCalledWith('Info:', info)
    expect(consoleGroupEndMock).toHaveBeenCalled()
  })

  it('logs error to console when app:error hook is triggered', async () => {
    let appErrorCallback: Function | undefined
    const nuxtApp = {
      hook: vi.fn((name, callback) => {
        if (name === 'app:error') {
          appErrorCallback = callback
        }
      }),
    }

    const plugin = (await import('../plugins/error-handler')).default
    plugin(nuxtApp)

    expect(appErrorCallback).toBeDefined()

    const error = new Error('Test App Error')

    appErrorCallback!(error)

    expect(consoleGroupMock).toHaveBeenCalledWith(
      'Global Nuxt App Error Handler'
    )
    expect(consoleErrorMock).toHaveBeenCalledWith('Error:', error)
    expect(consoleGroupEndMock).toHaveBeenCalled()
  })
})
