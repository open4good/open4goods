import { flushPromises, mount } from '@vue/test-utils'
import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { defineComponent, h } from 'vue'
import type { PropType } from 'vue'
import type { SearchSuggestResponseDto } from '~~/shared/api-client'

import SearchSuggestField from './SearchSuggestField.vue'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string, params: Record<string, unknown> = {}) => {
      const messages: Record<string, string> = {
        'search.suggestions.sections.categories': 'Categories',
        'search.suggestions.sections.products': 'Products',
        'search.suggestions.minimum': 'Type at least {min} characters.',
        'search.suggestions.error': 'Unable to load suggestions.',
        'search.suggestions.empty': 'No suggestions yet.',
        'search.suggestions.unknownCategory': 'Category',
        'search.suggestions.unknownProduct': 'Product',
        'search.suggestions.categoryAria': 'Open the {category} category',
        'search.suggestions.productAria': 'View the {product} product detail',
      }

      const template = messages[key] ?? key

      return template.replace(/\{(\w+)\}/gu, (_, match) => String(params[match] ?? ''))
    },
  }),
}))

mockNuxtImport('useRequestURL', () => () => new URL('https://example.com/'))

const VAutocompleteStub = defineComponent({
  name: 'VAutocompleteStub',
  props: {
    modelValue: { type: Object as PropType<unknown>, default: null },
    items: { type: Array as PropType<unknown[]>, default: () => [] },
    loading: { type: Boolean, default: false },
    label: { type: String, default: '' },
    placeholder: { type: String, default: '' },
    ariaLabel: { type: String, default: '' },
    menuProps: { type: [Object, Array], default: () => ({}) },
    hideNoData: { type: Boolean, default: false },
    noDataText: { type: String, default: '' },
    search: { type: String, default: '' },
  },
  emits: ['update:modelValue', 'update:search', 'click:clear'],
  setup(props, { slots }) {
    return () =>
      h(
        'div',
        { class: 'v-autocomplete-stub' },
        slots['no-data'] ? slots['no-data']() : [],
      )
  },
})

const createStub = (tag: string) =>
  defineComponent({
    name: `${tag}-stub`,
    props: { class: { type: String, default: '' } },
    setup(props, { slots, attrs }) {
      return () =>
        h(tag, { class: props.class, ...attrs }, slots.default ? slots.default() : [])
    },
  })

const sampleResponse: SearchSuggestResponseDto = {
  categoryMatches: [
    {
      verticalId: 'tv',
      verticalHomeTitle: 'Téléviseurs',
      verticalHomeUrl: 'televiseurs',
      imageSmall: 'https://cdn.example.com/tv.jpg',
    },
  ],
  productMatches: [
    {
      brand: 'Panasonic',
      model: 'TX-55Z90',
      gtin: '5025232963430',
      coverImagePath: '/images/tv.webp',
      ecoscoreValue: 2.5,
    },
  ],
}

describe('SearchSuggestField', () => {
  const mountField = async () =>
    mount(SearchSuggestField, {
      props: {
        modelValue: '',
        label: 'Search',
        placeholder: 'Search products',
        ariaLabel: 'Search products',
        minChars: 2,
      },
      global: {
        stubs: {
          VAutocomplete: VAutocompleteStub,
          VListItem: createStub('div'),
          VAvatar: createStub('div'),
          VImg: createStub('img'),
          VIcon: createStub('span'),
          ImpactScore: createStub('div'),
        },
      },
    })

  beforeEach(() => {
    vi.useFakeTimers()
    vi.stubGlobal('$fetch', vi.fn().mockResolvedValue(sampleResponse))
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('fetches suggestions when the value meets the minimum length', async () => {
    const wrapper = await mountField()
    const autocomplete = wrapper.getComponent(VAutocompleteStub)

    autocomplete.vm.$emit('update:search', 'tv')
    await wrapper.vm.$nextTick()

    await wrapper.setProps({ modelValue: 'tv' })
    vi.advanceTimersByTime(350)
    await flushPromises()

    expect(globalThis.$fetch).toHaveBeenCalledWith('/api/search/suggest', {
      params: { query: 'tv' },
    })
  })

  it('normalises category and product suggestions for display', async () => {
    const wrapper = await mountField()
    const autocomplete = wrapper.getComponent(VAutocompleteStub)

    autocomplete.vm.$emit('update:search', 'tv')
    await wrapper.vm.$nextTick()
    await wrapper.setProps({ modelValue: 'tv' })
    vi.advanceTimersByTime(350)
    await flushPromises()

    const items = autocomplete.props('items') as Array<{ type: string }>
    expect(items).toHaveLength(2)
    expect(items[0]?.type).toBe('category')
    expect(items[1]?.type).toBe('product')
  })

  it('emits events when a suggestion is selected or cleared', async () => {
    const wrapper = await mountField()
    const autocomplete = wrapper.getComponent(VAutocompleteStub)

    autocomplete.vm.$emit('update:search', 'tv')
    await wrapper.vm.$nextTick()
    await wrapper.setProps({ modelValue: 'tv' })
    vi.advanceTimersByTime(350)
    await flushPromises()

    const items = autocomplete.props('items') as Array<Record<string, unknown>>
    const category = items.find((item) => item.type === 'category')
    const product = items.find((item) => item.type === 'product')

    autocomplete.vm.$emit('update:modelValue', category)
    autocomplete.vm.$emit('update:modelValue', product)
    autocomplete.vm.$emit('click:clear')

    expect(wrapper.emitted('select-category')?.[0]?.[0]).toEqual(category)
    expect(wrapper.emitted('select-product')?.[0]?.[0]).toEqual(product)
    expect(wrapper.emitted('clear')).toBeTruthy()
  })
})
