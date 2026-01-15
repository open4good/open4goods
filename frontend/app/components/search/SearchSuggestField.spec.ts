import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import SearchSuggestField from './SearchSuggestField.vue'
import { nextTick } from 'vue'
import type {
  CategorySuggestionItem,
  ProductSuggestionItem,
  SuggestionItem,
} from '~/types/search-suggest'

interface ComponentVM {
  categories: CategorySuggestionItem[]
  products: ProductSuggestionItem[]
  internalSearch: string
  handleSelection: (item: SuggestionItem) => Promise<void>
  loadSuggestions: (query: string) => Promise<void>
}

// Mock dependencies
const fetchMock = vi.fn()
vi.stubGlobal('$fetch', fetchMock)

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string): string => key,
    locale: { value: 'en-US' },
  }),
}))

const pushMock = vi.fn()
const replaceMock = vi.fn()
mockNuxtImport('useRouter', () => {
  return () => ({
    push: pushMock,
    replace: replaceMock,
    currentRoute: { value: { path: '/' } },
  })
})

describe('SearchSuggestField', () => {
  beforeEach(() => {
    fetchMock.mockReset()
    pushMock.mockReset()
  })

  it('fetches and displays suggestions when typing, and does NOT update input on selection', async () => {
    const wrapper = await mountSuspended(SearchSuggestField, {
      props: {
        modelValue: '',
        label: 'Search',
        placeholder: 'Search...',
        ariaLabel: 'Search',
      },
    })

    fetchMock.mockResolvedValue({
      categoryMatches: [
        {
          verticalId: 'test-vertical',
          verticalHomeTitle: 'Test Category',
          verticalHomeUrl: '/category/test',
          imageSmall: '/cat.jpg',
        },
      ],
      productMatches: [],
    })

    const input = wrapper.find('input')
    await input.setValue('apple')
    await nextTick()

    // Wait for debounce (300ms)
    await new Promise(resolve => setTimeout(resolve, 350))
    await nextTick()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/products/suggest',
      expect.objectContaining({
        params: { query: 'apple' },
      })
    )

    const vm = wrapper.vm as unknown as ComponentVM
    // Simulate API return processing (since we stubbed $fetch, we rely on the component successfully handling the promise)
    // Wait for component to update state
    await nextTick()

    // Check if category loaded
    expect(vm.categories.length).toBe(1)

    // Simulate selection of the category
    await vm.handleSelection(vm.categories[0])

    // Verify Redirect
    expect(pushMock).toHaveBeenCalledWith('/category/test')

    // Verify Input did NOT change
    expect(vm.internalSearch).toBe('apple')
    expect(input.element.value).toBe('apple')
  })

  it('redirects to product page on product selection', async () => {
    const wrapper = await mountSuspended(SearchSuggestField, {
      props: {
        modelValue: 'bread',
        label: 'Search',
        placeholder: 'Search...',
        ariaLabel: 'Search',
      },
    })

    fetchMock.mockResolvedValue({
      categoryMatches: [],
      productMatches: [
        {
          title: 'Apple Pie',
          gtin: '123456',
          image: '/apple.jpg',
          brand: 'Bakery',
          model: 'Pie',
          prettyName: 'Apple Pie',
          ecoscoreValue: 80,
        },
      ],
    })

    // Trigger search
    const vm = wrapper.vm as unknown as ComponentVM
    // Calling internal function directly for speed, but standard debounce flow in first test covers integrity
    await vm.loadSuggestions('bread')
    await nextTick()

    expect(vm.products.length).toBe(1)

    // Simulate product selection
    await vm.handleSelection(vm.products[0])

    expect(pushMock).toHaveBeenCalledWith('/123456')
    expect(vm.internalSearch).toBe('bread') // Input remains
  })
})
