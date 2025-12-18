import { describe, it, expect, vi } from 'vitest'
import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import GtinPage from '../app/pages/opendata/gtin.vue'
import IsbnPage from '../app/pages/opendata/isbn.vue'

// Mock the API response
mockNuxtImport('useAsyncData', () => {
  return () => ({
    data: {
      value: {
        dataset: {
          recordCount: 1000,
          lastUpdated: '2025-01-01',
          fileSize: '10MB',
          downloadUrl: 'https://example.com/data.zip',
          headers: ['gtin', 'brand'],
        },
        overview: {
          downloadLimits: {
            concurrentDownloads: 5,
            downloadSpeed: '10MB/s',
          },
        },
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

const globalStubs = {
  OpendataDatasetHero: true,
  OpendataDatasetSummary: true,
  OpendataDownloadComparison: true,
  OpendataFaqSection: true,
  OpendataLicenseSection: true,
  'v-progress-linear': true,
  'v-container': true,
  'v-alert': true,
  'v-btn': true,
  'v-icon': true,
  'v-table': true,
}

describe('Opendata Dataset Pages', () => {
  it('GTIN page renders correctly', async () => {
    const wrapper = await mountSuspended(GtinPage, {
      global: { stubs: globalStubs },
    })

    expect(
      wrapper.findComponent({ name: 'OpendataDatasetHero' }).exists()
    ).toBe(true)
    expect(
      wrapper.findComponent({ name: 'OpendataDownloadComparison' }).exists()
    ).toBe(true)
  })

  it('ISBN page renders correctly', async () => {
    const wrapper = await mountSuspended(IsbnPage, {
      global: { stubs: globalStubs },
    })

    expect(
      wrapper.findComponent({ name: 'OpendataDatasetHero' }).exists()
    ).toBe(true)
    expect(
      wrapper.findComponent({ name: 'OpendataDownloadComparison' }).exists()
    ).toBe(true)
  })
})
