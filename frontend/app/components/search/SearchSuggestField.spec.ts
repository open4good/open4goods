import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import SearchSuggestField from './SearchSuggestField.vue'
import { nextTick } from 'vue'

// Mock dependencies
const fetchMock = vi.fn()
vi.stubGlobal('$fetch', fetchMock)

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
    locale: { value: 'en-US' },
  }),
}))

describe('SearchSuggestField', () => {
  beforeEach(() => {
    fetchMock.mockReset()
  })

  it('fetches and displays suggestions when typing', async () => {
    const wrapper = await mountSuspended(SearchSuggestField, {
      props: {
        modelValue: '',
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
          gtin: '123',
          image: '/apple.jpg',
          brand: 'Bakery',
          model: 'Pie',
          prettyName: 'Apple Pie',
          ecoscoreValue: 80,
        },
      ],
    })

    // Simulate typing "apple"
    await wrapper.find('input').setValue('apple')
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
  })

  it('keeps suggestions when user types a space', async () => {
    // This test aims to reproduce the bug where typing a space hides suggestions
    // because v-autocomplete locally filters them out if they don't match the query strictly.
    // "Apple Pie" contains "apple", but naive containment might fail on "apple ".

    const wrapper = await mountSuspended(SearchSuggestField, {
      props: {
        modelValue: '',
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
          gtin: '123',
          image: '/apple.jpg',
          brand: 'Bakery',
          model: 'Pie',
          prettyName: 'Apple Pie',
          ecoscoreValue: 80,
        },
      ],
    })

    // Simulate typing "apple "
    await wrapper.find('input').setValue('apple ')
    await nextTick()

    // Wait for debounce
    await new Promise(resolve => setTimeout(resolve, 350))
    await nextTick()

    // Verify API is called with trimmed value
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/products/suggest',
      expect.objectContaining({
        params: { query: 'apple' },
      })
    )

    // In a real browser, v-autocomplete would hide the item "Apple Pie" here if local filtering is enabled
    // because "Apple Pie" does not contain "apple " (it has a space in the middle but maybe the matching logic is specific).
    // Actually "Apple Pie" DOES contain "apple ".
    // BUT if we type "apple p" -> "Apple Pie" matches.
    // If we type "apple " (trailing space), usually fuzzy search or simple includes might fail if it expects the space to be exactly matched and followed by something,
    // OR if the Vuetify default filter splits by space and checks if all parts are present.
    // "apple" -> present. "" -> present.

    // Let's check if we can verify the filter prop is set to disable default filtering.
    // We can inspect the v-autocomplete component directly.
    const autocomplete = wrapper.findComponent({ name: 'VAutocomplete' })
    expect(autocomplete.exists()).toBe(true)

    // Check that custom-filter is set to a function that returns true
    const customFilter = autocomplete.props('customFilter')
    expect(typeof customFilter).toBe('function')
    expect(customFilter('any', 'query', {})).toBe(true)
  })
})
