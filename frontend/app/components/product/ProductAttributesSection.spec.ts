import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import type { ProductDto } from '~~/shared/api-client'

const runtimeConfigMock = vi.hoisted(() => ({
  public: { staticServer: 'https://static.example' },
}))

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
  props: {
    icon: { type: String, default: '' },
    color: { type: String, default: '' },
    size: { type: [Number, String], default: 24 },
  },
  setup(props) {
    return () =>
      h(
        'i',
        {
          class: 'v-icon-stub',
          'data-icon': props.icon,
          'data-color': props.color,
          'data-size': props.size,
        },
        props.icon
      )
  },
})

const VChipStub = defineComponent({
  name: 'VChipStub',
  setup(_, { slots }) {
    return () => h('span', { class: 'v-chip-stub' }, slots.default?.())
  },
})

const VTooltipStub = defineComponent({
  name: 'VTooltipStub',
  props: {
    text: { type: String, default: '' },
  },
  setup(props, { slots }) {
    return () =>
      h('div', { class: 'v-tooltip-stub', 'data-text': props.text }, [
        slots.activator?.({ props: {} }),
        h('div', { class: 'v-tooltip-stub__content' }, slots.default?.()),
      ])
  },
})

const VBtnStub = defineComponent({
  name: 'VBtnStub',
  props: {
    icon: { type: Boolean, default: false },
    density: { type: String, default: 'default' },
    variant: { type: String, default: 'text' },
  },
  setup(props, { slots }) {
    return () =>
      h(
        'button',
        {
          class: 'v-btn-stub',
          'data-icon': props.icon,
          'data-density': props.density,
          'data-variant': props.variant,
          type: 'button',
        },
        slots.default?.()
      )
  },
})

const VDividerStub = defineComponent({
  name: 'VDividerStub',
  setup() {
    return () => h('hr', { class: 'v-divider-stub' })
  },
})

const VImgStub = defineComponent({
  name: 'VImgStub',
  props: {
    src: { type: String, default: '' },
    alt: { type: String, default: '' },
  },
  setup(props) {
    return () =>
      h('img', { class: 'v-img-stub', src: props.src, alt: props.alt })
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
        props.label
          ? h('span', { class: 'v-text-field-stub__label' }, props.label)
          : null,
        h('input', {
          class: 'v-text-field-stub__input',
          value: props.modelValue,
          onInput,
          type: 'text',
        }),
      ])
  },
})

const VRowStub = defineComponent({
  name: 'VRowStub',
  setup(_, { slots }) {
    return () => h('div', { class: 'v-row-stub' }, slots.default?.())
  },
})

const VColStub = defineComponent({
  name: 'VColStub',
  props: {
    cols: { type: [Number, String], default: 12 },
    sm: { type: [Number, String], default: undefined },
    lg: { type: [Number, String], default: undefined },
  },
  setup(_, { slots }) {
    return () => h('div', { class: 'v-col-stub' }, slots.default?.())
  },
})

const VTimelineStub = defineComponent({
  name: 'VTimelineStub',
  setup(_, { slots }) {
    return () => h('div', { class: 'v-timeline-stub' }, slots.default?.())
  },
})

const VTimelineItemStub = defineComponent({
  name: 'VTimelineItemStub',
  props: {
    dotColor: { type: String, default: '' },
    size: { type: [Number, String], default: 'small' },
    fillDot: { type: Boolean, default: false },
  },
  setup(_, { slots }) {
    return () =>
      h('div', { class: 'v-timeline-item-stub' }, [
        slots.opposite?.(),
        slots.default?.(),
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
      weight: {
        name: 'Weight',
        value: '<strong>2 kg</strong>',
        sourcing: {
          bestValue: '2 kg',
          conflicts: false,
          sources: new Set([
            {
              datasourceName: 'datasource-a',
              value: '2 kg',
              language: 'fr',
              icecatTaxonomyId: 1010,
            },
          ]),
        },
      },
      depth: {
        name: 'Depth',
        numericValue: 45,
        sourcing: {
          bestValue: '45 cm',
          conflicts: true,
          sources: new Set([
            {
              datasourceName: 'datasource-a',
              value: '45 cm',
              language: 'fr',
              icecatTaxonomyId: 2020,
            },
            {
              datasourceName: 'datasource-b',
              value: '44 cm',
              language: 'en',
              icecatTaxonomyId: 2020,
            },
          ]),
        },
      },
      wireless: {
        name: 'Wireless',
        booleanValue: true,
        sourcing: {
          bestValue: 'Yes',
          conflicts: false,
          sources: new Set(),
        },
      },
    },
    classifiedAttributes: [
      {
        name: 'Performance',
        features: [{ name: 'Battery life', value: '10 h' }],
        unFeatures: [{ name: 'Noise level', value: 'High' }],
        attributes: [
          {
            name: 'Power',
            value: '<em>200 W</em>',
            sourcing: {
              bestValue: '200 W',
              conflicts: false,
              sources: new Set([
                {
                  datasourceName: 'datasource-a',
                  value: '200 W',
                },
              ]),
            },
          },
        ],
      },
      {
        name: 'Dimensions',
        features: [],
        unFeatures: [],
        attributes: [
          {
            name: 'Height',
            value: '120 cm',
            sourcing: {
              bestValue: '120 cm',
              conflicts: false,
              sources: new Set(),
            },
          },
        ],
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
              empty:
                'Identity information is not yet available for this product.',
            },
            attributes: {
              empty: 'Indexed attributes will appear here when available.',
            },
          },
          detailed: {
            title: 'Detailed specifications',
            unknownLabel: 'Specification',
            untitledGroup: 'Additional details',
            empty:
              'No detailed specifications are available for this product yet.',
            noResults: 'No specifications match your search.',
          },
          sourcing: {
            bestValue: 'Best value',
            description: 'Details provided by our trusted data sources.',
            columns: {
              source: 'Source',
              value: 'Value',
              language: 'Lang',
              taxonomy: 'Taxo.',
            },
            empty: 'No sourcing information available.',
            status: {
              conflicts: 'Conflicts detected',
              noConflicts: 'No conflicts detected',
            },
            tooltipAriaLabel: 'Show sourcing details',
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
        VTooltip: VTooltipStub,
        VBtn: VBtnStub,
        VDivider: VDividerStub,
        VImg: VImgStub,
        VTextField: VTextFieldStub,
        VRow: VRowStub,
        VCol: VColStub,
        VTimeline: VTimelineStub,
        VTimelineItem: VTimelineItemStub,
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

    const mainText = rows.map(row => row.text()).join(' ')
    expect(mainText).toContain('Weight')
    expect(mainText).toContain('2 kg')
    expect(mainText).toContain('Depth')
    expect(mainText).toContain('45 cm')
    expect(mainText).toContain('Wireless')
    expect(mainText).toContain('Yes')

    const richValue = wrapper.find('.product-attributes__table-value')
    expect(richValue.exists()).toBe(true)
    expect(richValue.text()).toContain('2 kg')
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
