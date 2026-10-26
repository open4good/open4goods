import { mountSuspended } from '@nuxt/test-utils/runtime'
import { useNuxtApp } from '#imports'
import { flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import GuideProductGrid from './GuideProductGrid.vue'

vi.mock('~/components/category/products/CategoryProductCardGrid.vue', () => ({
  default: { name: 'CategoryProductCardGridStub', template: '<div />' },
}))

describe('GuideProductGrid', () => {
  const fetchMock = vi.fn()
  // globalThis.$fetch is swapped on every SSR request: the component must use
  // the client of its own nuxtApp (GOU-1228).
  const globalFetchMock = vi.fn(() =>
    Promise.reject(new Error('globalThis.$fetch must not be used'))
  )
  const nuxtApp = useNuxtApp()
  const originalFetch = nuxtApp.$fetch

  beforeEach(() => {
    fetchMock.mockResolvedValue({ products: { data: [] } })
    nuxtApp.$fetch = fetchMock as unknown as typeof nuxtApp.$fetch
    vi.stubGlobal('$fetch', globalFetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    nuxtApp.$fetch = originalFetch
    fetchMock.mockReset()
    globalFetchMock.mockClear()
  })

  it('searches products through the request client', async () => {
    await mountSuspended(GuideProductGrid, { props: { vertical: 'phones' } })
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/products/search',
      expect.objectContaining({ method: 'POST' })
    )
    expect(globalFetchMock).not.toHaveBeenCalled()
  })
})
