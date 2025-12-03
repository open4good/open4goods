import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'

const cookiesWithDefaults: string[] = []

describe('PWA install prompt SSR', () => {
  beforeEach(() => {
    vi.resetModules()
    cookiesWithDefaults.length = 0
  })

  it('does not write the install dismissal cookie during server render', async () => {
    const originalWindow = globalThis.window

    // @ts-expect-error - window is intentionally unset to emulate SSR
    globalThis.window = undefined

    mockNuxtImport('useCookie', () => (name: string, options?: { default?: () => boolean }) => {
      if (options?.default) {
        cookiesWithDefaults.push(name)
      }

      return ref(undefined)
    })

    const { usePwaPrompt } = await import('~~/app/composables/usePwaPrompt')

    usePwaPrompt()

    expect(cookiesWithDefaults).toEqual([])

    // @ts-expect-error - window is restored after SSR emulation
    globalThis.window = originalWindow
  })
})
