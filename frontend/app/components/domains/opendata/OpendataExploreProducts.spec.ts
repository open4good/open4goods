import { mountSuspended, registerEndpoint } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import OpendataExploreProducts from './OpendataExploreProducts.vue'

const trackOpenDataOutboundClick = vi.fn()

vi.mock('~/composables/useAnalytics', () => ({
  useAnalytics: () => ({ trackOpenDataOutboundClick }),
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
    locale: { value: 'fr-FR' },
  }),
}))

vi.mock('#i18n', () => ({
  useLocalePath: () => (input: string) => input,
}))

registerEndpoint('/api/opendata/explore', () => [
  {
    id: 'tv',
    products: [
      { gtin: '1234567890123', name: 'TV One', href: '/televiseurs/tv-one' },
    ],
  },
  { id: 'oven', products: [] },
])

describe('OpendataExploreProducts', () => {
  beforeEach(() => trackOpenDataOutboundClick.mockClear())

  it('renders the category links and the plain GTIN of the products', async () => {
    const wrapper = await mountSuspended(OpendataExploreProducts, {
      props: { page: 'opendata' },
    })

    expect(wrapper.find('a[href="/televiseurs"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/lave-linges"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/climatiseurs"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('1234567890123')
    expect(wrapper.find('[data-category="oven"] ul').exists()).toBe(false)
  })

  it('tracks category and product clicks without the GTIN', async () => {
    const wrapper = await mountSuspended(OpendataExploreProducts, {
      props: { page: 'gtin' },
    })

    await wrapper.find('a[href="/televiseurs/tv-one"]').trigger('click')
    expect(trackOpenDataOutboundClick).toHaveBeenCalledWith({
      block: 'explore-products',
      target: 'product',
      page: 'gtin',
      category: 'tv',
    })

    await wrapper.find('a[href="/televiseurs"]').trigger('click')
    expect(trackOpenDataOutboundClick).toHaveBeenLastCalledWith({
      block: 'explore-products',
      target: 'category',
      page: 'gtin',
      category: 'tv',
    })
  })
})
