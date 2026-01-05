import { flushPromises, mount } from '@vue/test-utils'
import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { defineComponent, h, ref } from 'vue'
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
        'search.suggestions.scanner.openLabel': 'Scan a barcode',
        'search.suggestions.scanner.closeLabel': 'Close the scanner',
        'search.suggestions.scanner.title': 'Scan a barcode',
        'search.suggestions.scanner.helper':
          'Align the barcode within the frame.',
        'search.suggestions.scanner.loading': 'Preparing camera…',
        'search.suggestions.scanner.error': 'Camera unavailable.',
        'search.suggestions.voice.startLabel': 'Start voice search',
        'search.suggestions.voice.stopLabel': 'Stop voice search',
        'search.suggestions.voice.unsupported': 'Voice unavailable',
        'search.suggestions.voice.error': 'Voice error',
      }

      const template = messages[key] ?? key

      return template.replace(/\{(\w+)\}/gu, (_, match) =>
        String(params[match] ?? '')
      )
    },
  }),
}))

const displayMock = { smAndDown: ref(false) }

vi.mock('vuetify', () => ({
  useDisplay: () => displayMock,
}))

let routerPushMock: ReturnType<typeof vi.fn>

mockNuxtImport('useRouter', () => () => ({
  push: (...args: unknown[]) => routerPushMock(...args),
}))

mockNuxtImport('useRequestURL', () => () => new URL('https://example.com/'))
mockNuxtImport('useRuntimeConfig', () => () => ({
  public: {
    staticServer: 'https://static.example.com',
  },
}))

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
  emits: [
    'update:modelValue',
    'update:search',
    'click:clear',
    'keydown:enter',
    'blur',
    'focus',
  ],
  setup(props, { slots, attrs }) {
    const { class: className, ...restAttrs } = attrs

    return () =>
      h(
        'div',
        {
          ...restAttrs,
          class: ['v-autocomplete-stub', className as string | undefined],
        },
        [
          slots['append-inner']
            ? h('div', { class: 'append-inner-slot' }, slots['append-inner']())
            : null,
          slots['no-data']
            ? h('div', { class: 'no-data-slot' }, slots['no-data']())
            : null,
        ]
      )
  },
})

const VHoverStub = defineComponent({
  name: 'VHoverStub',
  setup(_, { slots, attrs }) {
    return () =>
      h(
        'div',
        attrs,
        slots.default ? slots.default({ isHovering: false, props: {} }) : []
      )
  },
})

const createStub = (tag: string) =>
  defineComponent({
    name: `${tag}-stub`,
    props: { class: { type: String, default: '' } },
    setup(props, { slots, attrs }) {
      return () =>
        h(
          tag,
          { class: props.class, ...attrs },
          slots.default ? slots.default() : []
        )
    },
  })

const PwaBarcodeScannerStub = defineComponent({
  name: 'PwaBarcodeScannerStub',
  props: { active: { type: Boolean, default: false } },
  emits: ['decode', 'error'],
  setup(props, { emit }) {
    return () =>
      h(
        'div',
        {
          class: 'pwa-barcode-scanner-stub',
          'data-active': String(props.active),
          onClick: () => emit('decode', '9876543210987'),
        },
        []
      )
  },
})

const VDialogStub = defineComponent({
  name: 'VDialogStub',
  props: { modelValue: { type: Boolean, default: false } },
  emits: ['update:modelValue'],
  setup(props, { slots }) {
    return () =>
      props.modelValue
        ? h(
            'div',
            { class: 'v-dialog-stub', 'data-open': String(props.modelValue) },
            slots.default ? slots.default() : []
          )
        : null
  },
})

const sampleResponse: SearchSuggestResponseDto = {
  categoryMatches: [
    {
      verticalId: 'tv',
      verticalHomeTitle: 'Téléviseurs',
      verticalHomeUrl: 'televiseurs',
      imageSmall: '/assets/tv.jpg',
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
          VHover: VHoverStub,
          VAutocomplete: VAutocompleteStub,
          VListItem: createStub('div'),
          VAvatar: createStub('div'),
          VImg: createStub('img'),
          VIcon: createStub('span'),
          VBtn: createStub('button'),
          VCard: createStub('div'),
          VDialog: VDialogStub,
          ImpactScore: createStub('div'),
          PwaBarcodeScanner: PwaBarcodeScannerStub,
        },
        components: {
          ClientOnly: defineComponent({
            name: 'ClientOnlyStub',
            setup(_, { slots }) {
              return () => (slots.default ? slots.default() : [])
            },
          }),
        },
      },
    })

  beforeEach(() => {
    vi.useFakeTimers()
    vi.stubGlobal('$fetch', vi.fn().mockResolvedValue(sampleResponse))
    routerPushMock = vi.fn()
    displayMock.smAndDown.value = false
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('does not fetch suggestions when disabled', async () => {
    const wrapper = await mount(SearchSuggestField, {
      props: {
        modelValue: '',
        label: 'Search',
        placeholder: 'Search products',
        ariaLabel: 'Search products',
        minChars: 2,
        enableSuggest: false,
      },
      global: {
        stubs: {
          VHover: VHoverStub,
          VAutocomplete: VAutocompleteStub,
          VListItem: createStub('div'),
          VAvatar: createStub('div'),
          VImg: createStub('img'),
          VIcon: createStub('span'),
          VBtn: createStub('button'),
          VCard: createStub('div'),
          VDialog: VDialogStub,
          ImpactScore: createStub('div'),
          PwaBarcodeScanner: PwaBarcodeScannerStub,
        },
        components: {
          ClientOnly: defineComponent({
            name: 'ClientOnlyStub',
            setup(_, { slots }) {
              return () => (slots.default ? slots.default() : [])
            },
          }),
        },
      },
    })

    const autocomplete = wrapper.getComponent(VAutocompleteStub)
    autocomplete.vm.$emit('update:search', 'tv')
    await wrapper.vm.$nextTick()
    vi.advanceTimersByTime(350)
    await flushPromises()

    expect(globalThis.$fetch).not.toHaveBeenCalled()
  })

  it('fetches suggestions when the value meets the minimum length', async () => {
    const wrapper = await mountField()
    const autocomplete = wrapper.getComponent(VAutocompleteStub)

    autocomplete.vm.$emit('update:search', 'tv')
    await wrapper.vm.$nextTick()

    await wrapper.setProps({ modelValue: 'tv' })
    vi.advanceTimersByTime(350)
    await flushPromises()

    expect(globalThis.$fetch).toHaveBeenCalledWith('/api/products/suggest', {
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
    expect((items[0] as Record<string, string>).image).toBe(
      'https://static.example.com/assets/tv.jpg'
    )
    expect((items[1] as Record<string, string>).image).toBe(
      'https://static.example.com/images/tv.webp'
    )
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
    const category = items.find(item => item.type === 'category')
    const product = items.find(item => item.type === 'product')

    autocomplete.vm.$emit('update:modelValue', category)
    autocomplete.vm.$emit('update:modelValue', product)
    autocomplete.vm.$emit('click:clear')

    expect(wrapper.emitted('select-category')?.[0]?.[0]).toEqual(category)
    expect(wrapper.emitted('select-product')?.[0]?.[0]).toEqual(product)
    expect(wrapper.emitted('clear')).toBeTruthy()
  })

  it('does not show the empty state before reaching the minimum length', async () => {
    const wrapper = await mountField()

    expect(wrapper.html()).not.toContain('Type at least')
  })

  it('shows an empty state when no suggestions are available after the minimum length', async () => {
    vi.stubGlobal(
      '$fetch',
      vi.fn().mockResolvedValue({
        categoryMatches: [],
        productMatches: [],
      })
    )

    const wrapper = await mountField()
    const autocomplete = wrapper.getComponent(VAutocompleteStub)

    autocomplete.vm.$emit('update:search', 'tv')
    await wrapper.vm.$nextTick()
    await wrapper.setProps({ modelValue: 'tv' })
    vi.advanceTimersByTime(350)
    await flushPromises()

    expect(wrapper.html()).toContain('No suggestions yet.')
  })

  it('keeps the typed value when the field loses focus', async () => {
    const wrapper = await mountField()
    const autocomplete = wrapper.getComponent(VAutocompleteStub)

    autocomplete.vm.$emit('focus')
    autocomplete.vm.$emit('update:search', 'tv')
    await wrapper.vm.$nextTick()

    await wrapper.setProps({ modelValue: 'tv' })

    autocomplete.vm.$emit('blur')
    await wrapper.vm.$nextTick()

    autocomplete.vm.$emit('update:search', '')
    await wrapper.vm.$nextTick()
    await flushPromises()
    vi.runAllTimers()

    const emittedUpdates = wrapper.emitted('update:modelValue') ?? []
    const lastUpdate = emittedUpdates.at(-1)

    expect(lastUpdate).toEqual(['tv'])
    expect(autocomplete.props('search')).toBe('tv')
  })

  it('does not emit submit when a suggestion is selected via the keyboard', async () => {
    const wrapper = await mountField()
    const autocomplete = wrapper.getComponent(VAutocompleteStub)

    autocomplete.vm.$emit('update:search', 'tv')
    await wrapper.vm.$nextTick()
    await wrapper.setProps({ modelValue: 'tv' })
    vi.advanceTimersByTime(350)
    await flushPromises()

    const items = autocomplete.props('items') as Array<Record<string, unknown>>
    const product = items.find(item => item.type === 'product')

    autocomplete.vm.$emit(
      'keydown:enter',
      new KeyboardEvent('keydown', { key: 'Enter' })
    )
    autocomplete.vm.$emit('update:modelValue', product)
    vi.runAllTimers()
    await flushPromises()

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.emitted('select-product')).toBeTruthy()
  })

  it('renders the scanner button on mobile viewports', async () => {
    displayMock.smAndDown.value = true
    const wrapper = await mountField()

    expect(wrapper.find('[data-test="search-scanner-button"]').exists()).toBe(
      true
    )
  })

  it('renders the voice button on mobile when enabled', async () => {
    displayMock.smAndDown.value = true
    const wrapper = await mountField()

    expect(wrapper.find('[data-test="search-voice-button"]').exists()).toBe(
      true
    )
  })

  it('opens the scanner dialog when the button is clicked', async () => {
    displayMock.smAndDown.value = true
    const wrapper = await mountField()

    await wrapper.find('[data-test="search-scanner-button"]').trigger('click')

    expect(wrapper.find('.v-dialog-stub').exists()).toBe(true)
  })

  it('navigates to the GTIN route when a code is scanned', async () => {
    const wrapper = await mountField()

    const instance = wrapper.vm as unknown as {
      handleScannerDecode: (value: string | null) => void
    }

    instance.handleScannerDecode(' 1234567890123 ')

    expect(routerPushMock).toHaveBeenCalledWith('/1234567890123')
  })
})
