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

  it('always adds the offersCount filter when offers are required', async () => {
    const diagonal = {
      field: 'attributes.indexed.DIAGONALE_POUCES.numericValue',
      operator: 'range' as const,
      min: 55,
      max: 55,
    }
    await mountSuspended(GuideProductGrid, {
      props: { vertical: 'tv', filters: [diagonal], requireOffers: true },
    })
    await flushPromises()

    const body = fetchMock.mock.calls[0]![1].body
    expect(body.filters.filters).toEqual([
      diagonal,
      { field: 'offersCount', operator: 'range', min: 1 },
    ])
  })

  it('sends no filters by default (backward compatible)', async () => {
    await mountSuspended(GuideProductGrid, { props: { vertical: 'laptops' } })
    await flushPromises()

    expect(fetchMock.mock.calls[0]![1].body.filters).toBeUndefined()
  })

  it('renders nothing when empty and hideWhenEmpty is set', async () => {
    const wrapper = await mountSuspended(GuideProductGrid, {
      props: { vertical: 'tv', hideWhenEmpty: true },
    })
    await flushPromises()

    expect(wrapper.find('.guide-product-grid__empty').exists()).toBe(false)
  })
})
