import { describe, it, expect, vi } from 'vitest'
import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import OpendataPage from '../app/pages/opendata/index.vue'

// Mock the API response
mockNuxtImport('useAsyncData', () => {
  return () => ({
    data: {
      value: {
        totalProductCount: 1234567,
        datasetCount: 42,
        totalDatasetSize: '500GB',
      },
    },
    pending: { value: false },
    error: { value: null },
    refresh: vi.fn(),
  })
})

// Mock i18n
mockNuxtImport('useI18n', () => {
  return () => ({
    t: (key: string) => key,
    locale: { value: 'fr-FR' },
  })
})

describe('Opendata Page', () => {
  it('renders critical sections', async () => {
    const wrapper = await mountSuspended(OpendataPage, {
      global: {
        stubs: {
          OpendataHero: true,
          OpendataStatsStrip: true,
          OpendataDatasetHighlights: true,
          OpendataLicenseSection: true,
          OpendataFaqSection: true,
          OpendataOpenSourceStrip: true,
          'v-progress-linear': true,
          'v-container': true,
          'v-alert': true,
          'v-btn': true,
        },
      },
    })

    expect(wrapper.findComponent({ name: 'OpendataHero' }).exists()).toBe(true)
    // Check if stats are passed (indirectly checks useAsyncData usage)
    const statsComp = wrapper.findComponent({ name: 'OpendataStatsStrip' })
    expect(statsComp.exists()).toBe(true)

    // Check if Dataset Highlights are present
    expect(
      wrapper.findComponent({ name: 'OpendataDatasetHighlights' }).exists()
    ).toBe(true)
  })
})
