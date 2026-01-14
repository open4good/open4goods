import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi, afterEach } from 'vitest'
import { computed, ref } from 'vue'
import TheMainLogo from './The-main-logo.vue'

// Mock dependencies
vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

// We want to test the REAL useLogoAsset behavior, but we might need to mock some underlying composables it uses
// derived from checking useThemedAsset.ts, it uses useTheme from vuetify
vi.mock('vuetify', () => ({
  useTheme: () => ({
    global: {
      name: ref('light'),
    },
  }),
}))

// We also need to mock useSeasonalEventPack since it's used by useThemedAsset
vi.mock('~/composables/useSeasonalEventPack', () => ({
  useSeasonalEventPack: () => computed(() => 'default'),
}))

// We need to mock useEventPackI18n
vi.mock('~/composables/useEventPackI18n', () => ({
  useEventPackI18n: () => ({
    resolveString: () => null,
  }),
}))

describe('TheMainLogo', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders the logo image with a valid src', async () => {
    const wrapper = await mountSuspended(TheMainLogo)

    const img = wrapper.find('img.main-logo')
    expect(img.exists()).toBe(true)

    const src = img.attributes('src')
    // It should point to the light theme logo we confirmed exists
    console.log('Resolved Logo SRC:', src)

    // The asset is inlined as a data URI by the build system
    expect(src).toBeTruthy()
    expect(src).toContain('data:image/svg+xml')
    expect(src).not.toContain('undefined')
    expect(src).not.toContain('null')
  })
})
