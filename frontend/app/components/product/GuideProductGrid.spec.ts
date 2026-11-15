import { mountSuspended } from '@nuxt/test-utils/runtime'
import { useNuxtApp } from '#imports'
import { flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import GuideProductGrid from './GuideProductGrid.vue'

const trackEventMock = vi.hoisted(() => vi.fn())
const fetchCategoryByIdMock = vi.hoisted(() => vi.fn())
vi.mock('~/composables/categories/useCategories', () => ({
  useCategories: () => ({ fetchCategoryById: fetchCategoryByIdMock }),
}))
vi.mock('~/composables/useAnalytics', () => ({
  useAnalytics: () => ({ trackEvent: trackEventMock }),
}))

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

  describe('subcategory', () => {
    const subCategoryCategory = {
      verticalHomeUrl: 'lave-vaisselle',
      subCategories: [
        {
          id: 'dishwasher_45',
          slug: 'lave-vaisselle-45-cm',
          activatedFilters: [
            {
              field: 'attributes.indexed.WIDTH.numericValue',
              operator: 'GREATER_THAN_OR_EQUAL',
              value: '43',
            },
          ],
        },
      ],
    }
    beforeEach(() => {
      fetchCategoryByIdMock.mockResolvedValue(subCategoryCategory)
    })

    it('applies activatedFilters, forces offers and sorts by impact score', async () => {
      const extra = { field: 'x', operator: 'term' as const, terms: ['a'] }
      const wrapper = await mountSuspended(GuideProductGrid, {
        props: {
          vertical: 'dishwasher-sub',
          subcategory: 'dishwasher_45',
          filters: [extra],
        },
      })
      await flushPromises()

      const searches = fetchMock.mock.calls.filter(
        c => c[0] === '/api/products/search'
      )
      expect(searches).toHaveLength(1)
      const body = searches[0]![1].body
      expect(body.filters.filters).toEqual([
        expect.objectContaining({
          field: 'attributes.indexed.WIDTH.numericValue',
        }),
        extra,
        { field: 'offersCount', operator: 'range', min: 1 },
      ])
      expect(body.sort.sorts[0].order).toBe('desc')
      expect(wrapper.exists()).toBe(true)
    })

    it('renders nothing and warns for an unknown subcategory', async () => {
      const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
      const wrapper = await mountSuspended(GuideProductGrid, {
        props: { vertical: 'dishwasher-unk', subcategory: 'inconnue' },
      })
      await flushPromises()

      expect(wrapper.find('.guide-product-grid').exists()).toBe(false)
      expect(
        fetchMock.mock.calls.some(c => c[0] === '/api/products/search')
      ).toBe(false)
      expect(warn).toHaveBeenCalled()
      warn.mockRestore()
    })

    it('renders the see-all link and tracks clicks', async () => {
      fetchMock.mockResolvedValue({
        products: { data: [{ gtin: 1, identity: {} }] },
      })
      const wrapper = await mountSuspended(GuideProductGrid, {
        props: { vertical: 'dishwasher-link', subcategory: 'dishwasher_45' },
      })
      await flushPromises()

      const link = wrapper.find('a[data-widget-target="see-all"]')
      expect(link.attributes('href')).toBe(
        '/lave-vaisselle/lave-vaisselle-45-cm'
      )
      await link.trigger('click')

      expect(trackEventMock).toHaveBeenCalledWith('product-widget-click', {
        props: {
          page: expect.any(String),
          vertical: 'dishwasher-link',
          subcategory: 'dishwasher_45',
          target: 'see-all',
        },
      })
    })
  })

  describe('seeAllCategory', () => {
    it('links "Voir tout" to the category page without subcategory', async () => {
      fetchCategoryByIdMock.mockResolvedValue({ verticalHomeUrl: '/fours/' })
      fetchMock.mockResolvedValue({
        products: { data: [{ gtin: 1, identity: {} }] },
      })
      const wrapper = await mountSuspended(GuideProductGrid, {
        props: { vertical: 'oven-see-all', seeAllCategory: true },
      })
      await flushPromises()

      expect(fetchCategoryByIdMock).toHaveBeenCalledWith('oven-see-all')
      expect(
        wrapper.find('a[data-widget-target="see-all"]').attributes('href')
      ).toBe('/fours')
    })

    it('renders no see-all link when the category fails to load', async () => {
      fetchCategoryByIdMock.mockRejectedValue(new Error('down'))
      const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      fetchMock.mockResolvedValue({
        products: { data: [{ gtin: 1, identity: {} }] },
      })
      const wrapper = await mountSuspended(GuideProductGrid, {
        props: { vertical: 'oven-see-all-down', seeAllCategory: true },
      })
      await flushPromises()

      expect(wrapper.find('a[data-widget-target="see-all"]').exists()).toBe(
        false
      )
      errorSpy.mockRestore()
    })
  })
})
