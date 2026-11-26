import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ref, nextTick } from 'vue'
import { useNuxtApp } from '#app'
import CategoryPage from './CategoryPage.vue'

// GOU-2269: the SSR list must not be refetched (nor replaced by skeletons) at hydration.
const csrfFetchMock = vi.hoisted(() => vi.fn())

const category = {
  id: 'climatiseurs',
  verticalHomeTitle: 'Climatiseurs',
  subCategories: [],
  subsets: [],
}
const ssrProducts = {
  products: { data: [], total: 0 },
  aggregations: [],
}
const filterOptions = { global: [], impact: [], technical: [] }

vi.mock('~/composables/categories/useCategories', () => ({
  useCategories: () => ({
    currentCategory: ref(category),
    error: ref(null),
    selectCategoryBySlug: vi.fn(),
  }),
}))
// A hub without editorial file (GOU-2716) renders no block.
const asyncData = (key: string) => {
  if (key.startsWith('category-products')) return ssrProducts
  if (key.startsWith('category-hub')) return null
  return category
}
mockNuxtImport('useAsyncData', () => async (key: string) => ({
  data: ref(asyncData(key)),
  error: ref(null),
}))
mockNuxtImport('useLazyAsyncData', () => (key: string) => ({
  data: ref(key.includes('filter-options') ? filterOptions : filterOptions),
  execute: vi.fn().mockResolvedValue(undefined),
  status: ref('success'),
}))

const searchCalls = () =>
  csrfFetchMock.mock.calls.filter(([url]) => url === '/api/products/cards')

describe('CategoryPage hydration (GOU-2269)', () => {
  beforeEach(() => {
    csrfFetchMock.mockReset()
    csrfFetchMock.mockResolvedValue(ssrProducts)
    window.location.hash = ''
    ;(useNuxtApp() as unknown as { $fetch: unknown }).$fetch = csrfFetchMock
  })

  it('sends at most one silent search at mount and never shows skeletons', async () => {
    const wrapper = await mountSuspended(CategoryPage, {
      props: { slug: 'climatiseurs' },
      global: {
        stubs: {
          VNavigationDrawer: { template: '<div><slot /></div>' },
          VBottomSheet: true,
          VDialog: true,
        },
      },
    })
    await new Promise(resolve => setTimeout(resolve, 600))
    await nextTick()

    expect(searchCalls().length).toBe(1)
    expect(wrapper.find('.v-skeleton-loader').exists()).toBe(false)
  })
})
