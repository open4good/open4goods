import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, test, expect, vi } from 'vitest'
import OpendataPage from './opendata.vue'

const mockData = {
  countGtin: 10,
  countIsbn: 2,
  gtinFileSize: '1 KB',
  isbnFileSize: '2 KB',
  gtinLastUpdated: new Date(),
  isbnLastUpdated: new Date()
}

vi.mock('~/composables/opendata/useOpenData', () => ({
  useOpenData: () => ({ data: mockData })
}))

describe('opendata page', () => {
  test('renders table with data', async () => {
    const wrapper = await mountSuspended(OpendataPage)
    expect(wrapper.text()).toContain('Dataset GTIN')
    expect(wrapper.text()).toContain('1 KB')
  })
})
