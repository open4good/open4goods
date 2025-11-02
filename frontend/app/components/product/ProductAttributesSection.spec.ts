import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import type { ProductDto } from '~~/shared/api-client'

const runtimeConfigMock = vi.hoisted(() => ({ public: { staticServer: 'https://static.example' } }))

vi.mock('#app', () => ({
  useRuntimeConfig: () => runtimeConfigMock,
}))

vi.mock('#imports', () => ({
  useRuntimeConfig: () => runtimeConfigMock,
}))

const VCardStub = defineComponent({
  name: 'VCardStub',
  setup(_, { slots }) {
    return () => h('div', { class: 'v-card-stub' }, slots.default?.())
  },
})

const VTableStub = defineComponent({
  name: 'VTableStub',
  setup(_, { slots }) {
    return () => h('table', { class: 'v-table-stub' }, slots.default?.())
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  props: { icon: { type: String, default: '' } },
  setup(props) {
    return () => h('i', { class: 'v-icon-stub' }, props.icon)
  },
})

const VChipStub = defineComponent({
  name: 'VChipStub',
  setup(_, { slots }) {
    return () => h('span', { class: 'v-chip-stub' }, slots.default?.())
  },
})

const VImgStub = defineComponent({
  name: 'VImgStub',
  props: {
    src: { type: String, default: '' },
    alt: { type: String, default: '' },
  },
  setup(props) {
    return () => h('img', { class: 'v-img-stub', src: props.src, alt: props.alt })
  },
})

const VTextFieldStub = defineComponent({
  name: 'VTextFieldStub',
  props: {
    modelValue: { type: String, default: '' },
    label: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const onInput = (event: Event) => {
      const target = event.target as HTMLInputElement | null
      emit('update:modelValue', target?.value ?? '')
    }

    return () =>
      h('label', { class: 'v-text-field-stub' }, [
        props.label ? h('span', { class: 'v-text-field-stub__label' }, props.label) : null,
        h('input', {
          class: 'v-text-field-stub__input',
          value: props.modelValue,
          onInput,
          type: 'text',
        }),
      ])
  },
})

const buildProduct = (): ProductDto => ({
  gtin: 1234567890123,
  base: {
    gtin: 1234567890123,
    creationDate: Date.UTC(2023, 3, 12),
    lastChange: Date.UTC(2024, 4, 1),
  },
  identity: {
    brand: 'BrandX',
    model: 'Model Y',
    akaBrands: new Set(['Brand X Alternative']),
    akaModels: new Set(['Model Y Prime']),
  },
  attributes: {
    indexedAttributes: {
      weight: { name: 'Weight', value: '2 kg' },
      depth: { name: 'Depth', numericValue: 45 },
      wireless: { name: 'Wireless', booleanValue: true },
    },
    classifiedAttributes: [
      {
        name: 'Performance',
        features: [{ name: 'Battery life', value: '10 h' }],
        unFeatures: [{ name: 'Noise level', value: 'High' }],
        attributes: [{ name: 'Power', value: '200 W' }],
      },
      {
        name: 'Dimensions',
        features: [],
        unFeatures: [],
        attributes: [{ name: 'Height', value: '120 cm' }],
      },
    ],
  },
})

const i18n = createI18n({
  legacy: false,
  locale: 'en-US',
  messages: {
    'en-US': {
      product: {
        attributes: {
          title: 'Technical specifications',
          subtitle: 'Browse every specification and filter with keywords.',
          searchPlaceholder: 'Search a specification',
          features: 'Highlights',
          unfeatures: 'Watchouts',
          main: {
            title: 'Key specifications',
            identity: {
              title: 'Identity card',
              brand: 'Brand',
              akaBrands: 'Also referenced as',
              model: 'Model',
              akaModels: 'Other names',
              knownSince: 'Known since',
              lastUpdated: 'Last update',
              gtin: 'GTIN',
              gtinLabel: 'GTIN barcode reference',
              gtinImageAlt: 'GTIN barcode for {gtin}',
              empty: 'Identity information is not yet available for this product.',
            },
            attributes: {
              title: 'Indexed attributes',
              empty: 'Indexed attributes will appear here when available.',
            },
          },
          detailed: {
            title: 'Detailed specifications',
            unknownLabel: 'Specification',
            untitledGroup: 'Additional details',
            empty: 'No detailed specifications are available for this product yet.',
            noResults: 'No specifications match your search.',
          },
        },
      },
      common: {
        boolean: {
          true: 'Yes',
          false: 'No',
        },
      },
    },
  },
})

const mountComponent = async (product: ProductDto) => {
  const module = await import('./ProductAttributesSection.vue')
  const Component = module.default

  const wrapper = await mountSuspended(Component, {
    props: { product },
    global: {
      plugins: [i18n],
      stubs: {
        VCard: VCardStub,
        VTable: VTableStub,
        VIcon: VIconStub,
        VChip: VChipStub,
        VImg: VImgStub,
        VTextField: VTextFieldStub,
      },
    },
  })

  return wrapper
}

describe('ProductAttributesSection', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(Date.UTC(2024, 5, 1)))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders identity information and GTIN image', async () => {
    const wrapper = await mountComponent(buildProduct())

    const identityRows = wrapper.findAll('.product-attributes__identity-row')
    expect(identityRows).not.toHaveLength(0)
    expect(wrapper.text()).toContain('BrandX')
    expect(wrapper.text()).toContain('Brand X Alternative')
    expect(wrapper.text()).toContain('Model Y')
    expect(wrapper.text()).toContain('Model Y Prime')
    expect(wrapper.text()).toContain('GTIN')

    const image = wrapper.find('img.v-img-stub')
    expect(image.exists()).toBe(true)
    expect(image.attributes('src')).toContain('/1234567890123-gtin.png')
  })

  it('renders the main attributes table with formatted values', async () => {
    const wrapper = await mountComponent(buildProduct())

    const rows = wrapper.findAll('table.v-table-stub tbody tr')
    expect(rows.length).toBeGreaterThanOrEqual(3)

    const mainText = rows.map((row) => row.text()).join(' ')
    expect(mainText).toContain('Weight')
    expect(mainText).toContain('2 kg')
    expect(mainText).toContain('Depth')
    expect(mainText).toContain('45')
    expect(mainText).toContain('Wireless')
    expect(mainText).toContain('Yes')
  })

  it('filters detailed groups with the search input', async () => {
    const wrapper = await mountComponent(buildProduct())

    const input = wrapper.find('input.v-text-field-stub__input')
    await input.setValue('Power')
    await flushPromises()

    const cards = wrapper.findAll('.product-attributes__detail-card')
    expect(cards).toHaveLength(1)
    expect(cards[0].text()).toContain('Power')

    await input.setValue('unknown spec')
    await flushPromises()

    const emptyState = wrapper.find('.product-attributes__empty--detailed')
    expect(emptyState.exists()).toBe(true)
    expect(emptyState.text()).toBe('No specifications match your search.')
  })
})
