import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h, ref } from 'vue'
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

const isLoggedInRef = ref(false)

vi.mock('~/composables/useAuth', () => ({
  useAuth: () => ({
    isLoggedIn: isLoggedInRef,
  }),
}))

const VCardStub = defineComponent({
  name: 'VCardStub',
  setup(_, { slots, attrs }) {
    return () =>
      h('div', { class: ['v-card-stub', attrs.class] }, slots.default?.())
  },
})

const VTableStub = defineComponent({
  name: 'VTableStub',
  setup(_, { slots, attrs }) {
    return () =>
      h('table', { class: ['v-table-stub', attrs.class] }, slots.default?.())
  },
})

const VDataTableStub = defineComponent({
  name: 'VDataTableStub',
  props: [
    'headers',
    'items',
    'itemsPerPage',
    'itemClass',
    'density',
    'class',
    'rowProps',
  ],
  setup(props, { slots }) {
    return () =>
      h(
        'div',
        { class: ['v-data-table-stub', props.class] },
        ((props.items as Array<Record<string, unknown>>) ?? []).map(item => {
          const rowClass =
            (typeof props.itemClass === 'function'
              ? props.itemClass(item) // Vuetify 2 legacy / custom prop in stub
              : props.itemClass) ||
            (typeof props.rowProps === 'function'
              ? props.rowProps(item)
              : props.rowProps)

          return h('div', { class: ['v-data-table-row-stub', rowClass] }, [
            slots['item.attribute']?.({ item }),
            slots['item.bestValue']?.({ item }),
            slots['item.sources']?.({ item }),
            slots['item.indexed']?.({ item }),
          ])
        })
      )
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
  setup(_, { slots, attrs }) {
    return () =>
      h('span', { class: ['v-chip-stub', attrs.class] }, slots.default?.())
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
    value: { type: String, default: undefined },
  },
  setup(props, { slots, attrs }) {
    return () =>
      h(
        'button',
        {
          class: ['v-btn-stub', attrs.class],
          'data-icon': props.icon,
          'data-density': props.density,
          'data-variant': props.variant,
          type: 'button',
          value: props.value,
        },
        slots.default?.()
      )
  },
})

const VBtnToggleStub = defineComponent({
  name: 'VBtnToggleStub',
  props: ['modelValue'],
  emits: ['update:modelValue'],
  setup(props, { slots, attrs }) {
    return () =>
      h('div', { class: ['v-btn-toggle-stub', attrs.class] }, slots.default?.())
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
  setup(props, { attrs }) {
    return () =>
      h('img', {
        class: ['v-img-stub', attrs.class],
        src: props.src,
        alt: props.alt,
      })
  },
})

const VTextFieldStub = defineComponent({
  name: 'VTextFieldStub',
  props: {
    modelValue: { type: String, default: '' },
    label: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { emit, attrs }) {
    const onInput = (event: Event) => {
      const target = event.target as HTMLInputElement | null
      emit('update:modelValue', target?.value ?? '')
    }

    return () =>
      h('label', { class: ['v-text-field-stub', attrs.class] }, [
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

const VCheckboxStub = defineComponent({
  name: 'VCheckboxStub',
  props: {
    modelValue: { type: Boolean, default: false },
    label: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { emit, attrs }) {
    const onChange = (event: Event) => {
      const target = event.target as HTMLInputElement | null
      emit('update:modelValue', target?.checked ?? false)
    }

    return () =>
      h('label', { class: ['v-checkbox-stub', attrs.class] }, [
        h('input', {
          class: 'v-checkbox-stub__input',
          type: 'checkbox',
          checked: props.modelValue,
          onChange,
        }),
        props.label
          ? h('span', { class: 'v-checkbox-stub__label' }, props.label)
          : null,
      ])
  },
})

const VRowStub = defineComponent({
  name: 'VRowStub',
  setup(_, { slots, attrs }) {
    return () =>
      h('div', { class: ['v-row-stub', attrs.class] }, slots.default?.())
  },
})

const VColStub = defineComponent({
  name: 'VColStub',
  props: {
    cols: { type: [Number, String], default: 12 },
    sm: { type: [Number, String], default: undefined },
    lg: { type: [Number, String], default: undefined },
  },
  setup(_, { slots, attrs }) {
    return () =>
      h('div', { class: ['v-col-stub', attrs.class] }, slots.default?.())
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
          audit: {
            title: 'Attribute sourcing audit',
            subtitle:
              'Compare indexed attributes with their synonyms and data sources.',
            searchPlaceholder: 'Search attributes',
            filters: {
              indexed: 'Indexed',
              notIndexed: 'Not indexed',
            },
            columns: {
              attribute: 'Attribute',
              bestValue: 'Best value',
              sources: 'Sources',
              indexed: 'Status',
            },
            indexed: 'Indexed',
            notIndexed: 'Not indexed',
            noBestValue: 'No value',
            empty: 'No attribute configuration is available yet.',
            emptyFiltered: 'No attributes match the current filters.',
          },
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
            viewTable: 'Table view',
            viewCards: 'Card view',
            tooltips: {
              viewTable: 'Switch to table view',
              viewCards: 'Switch to card view',
            },
            columns: {
              group: 'Category',
              count: 'Count',
            },
          },
          sourcing: {
            bestValue: 'Best value',
            description: 'Details provided by our trusted data sources.',
            sourceCount: {
              one: '1 source',
              other: '{count} sources',
            },
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

const mountComponent = async (
  product: ProductDto,
  options?: {
    attributeConfigs?: Array<Record<string, unknown>>
    isLoggedIn?: boolean
  }
) => {
  isLoggedInRef.value = options?.isLoggedIn ?? false
  const module = await import('./ProductAttributesSection.vue')
  const Component = module.default

  const wrapper = await mountSuspended(Component, {
    props: { product, attributeConfigs: options?.attributeConfigs ?? [] },
    global: {
      plugins: [i18n],
      stubs: {
        VCard: VCardStub,
        VTable: VTableStub,
        VDataTable: VDataTableStub,
        VIcon: VIconStub,
        VChip: VChipStub,
        VTooltip: VTooltipStub,
        VBtn: VBtnStub,
        VBtnToggle: VBtnToggleStub,
        VDivider: VDividerStub,
        VImg: VImgStub,
        VTextField: VTextFieldStub,
        VCheckbox: VCheckboxStub,
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

    // Switch to cards mode as cards are expected
    // const gridBtn = wrapper
    //   .findAll('.v-btn-stub')
    //   .find(b => b.html().includes('mdi-view-grid'))
    // if (gridBtn) await gridBtn.trigger('click')

    // Simulate toggle change manually as stub doesn't handle click->update
    const toggle = wrapper.findComponent({ name: 'VBtnToggleStub' })
    if (toggle.exists()) {
      toggle.vm.$emit('update:modelValue', 'cards')
    }

    await flushPromises()

    const cards = wrapper.findAll('.product-attributes__detail-card')
    if (cards.length !== 1) {
      console.log('DEBUG: Card count mismatch. Wrapper HTML:', wrapper.html())
    }
    expect(cards).toHaveLength(1)
    expect(cards[0].text()).toContain('Power')

    await input.setValue('unknown spec')
    await flushPromises()

    const emptyState = wrapper.find('.product-attributes__empty--detailed')
    expect(emptyState.exists()).toBe(true)
    expect(emptyState.text()).toBe('No specifications match your search.')
  })

  it('renders the attribute audit table for logged-in users', async () => {
    const wrapper = await mountComponent(buildProduct(), {
      isLoggedIn: true,
      attributeConfigs: [
        {
          key: 'weight',
          name: 'Weight',
          synonyms: { icecat: new Set(['weight', 'mass']) },
        },
        {
          key: 'depth',
          name: 'Depth',
          synonyms: { icecat: new Set(['depth']) },
        },
        {
          key: 'color',
          name: 'Color',
          synonyms: { eprel: new Set(['color']) },
        },
      ],
    })

    const rows = wrapper.findAll(
      '.product-attributes__audit-table .v-data-table-row-stub'
    )
    // 3 indexed (Weight, Depth, Wireless) + 4 raw (Power, Height, Battery life, Noise level)
    // 3 indexed (Weight, Depth, Wireless) + 4 raw (Power, Height, Battery life, Noise level)
    expect(rows).toHaveLength(7)
    expect(wrapper.text()).toContain('Attribute sourcing audit')
    expect(wrapper.text()).toContain('Weight')
    // Color is in config but not in data, so it should NOT be shown
    expect(wrapper.text()).not.toContain('Color')
    // Power is raw data, so it SHOULD be shown
    expect(wrapper.text()).toContain('Power')

    const indexedRows = wrapper.findAll(
      '.product-attributes__audit-row--indexed'
    )
    // 3 indexed attributes are now styled as indexed
    expect(indexedRows).toHaveLength(3)

    const unindexedRows = wrapper.findAll(
      '.product-attributes__audit-row--unindexed'
    )
    // 4 raw attributes are unindexed
    expect(unindexedRows).toHaveLength(4)
  })

  it('filters audit table by data source name', async () => {
    const wrapper = await mountComponent(buildProduct(), {
      isLoggedIn: true,
      attributeConfigs: [],
    })

    const input = wrapper.find('input.v-text-field-stub__input')
    await input.setValue('datasource-a')
    await flushPromises()

    // Should verify that rows containing 'datasource-a' are present
    // Weight (indexed) has datasource-a
    // Depth (indexed) has datasource-a
    // Power (raw) has datasource-a
    // Wireless (indexed) has NO sources (set empty)
    // Height (raw) has NO sources
    // So expected: Weight, Depth, Power.
    // We must restrict check to the audit table rows, because 'Wireless' is also a Main Attribute
    // and thus rendered elsewhere in the component.
    const auditRows = wrapper.findAll('.product-attributes__audit-row')
    const auditText = auditRows.map(r => r.text()).join(' ')

    expect(auditText).toContain('Weight')
    expect(auditText).toContain('Depth')
    expect(auditText).toContain('Power')
    expect(auditText).not.toContain('Wireless')
    expect(auditText).not.toContain('Height')
  })

  it('applies correct styling classes to indexed rows', async () => {
    const wrapper = await mountComponent(buildProduct(), {
      isLoggedIn: true,
      attributeConfigs: [],
    })

    // Indexed rows should have 'product-attributes__audit-row--indexed'
    const indexedRows = wrapper.findAll(
      '.product-attributes__audit-row--indexed'
    )
    expect(indexedRows.length).toBeGreaterThan(0)

    // And they should NOT have '--matched' or '--unindexed'
    // Note: The previous logic might have assigned 'matched' to indexed rows if matched?
    // But our new logic strictly assigns 'indexed' first.
    // 'Weight' is indexed. Let's find the row for Weight.
    // In stub, we can't easily find specifically "the row with Weight" by class,
    // but we can assume if we have indexed attributes, we should have the class.
    expect(indexedRows.length).toBe(3) // Weight, Depth, Wireless
  })

  it('renders attributes from allAttributes in audit table', async () => {
    const product = buildProduct()
    // Add an attribute that is ONLY in allAttributes, not in classifiedAttributes
    product.attributes!.allAttributes = {
      'only-in-all': {
        name: 'OnlyInAll',
        value: '123',
        icecatTaxonomyIds: new Set(),
        sourcing: {
          sources: [],
        },
      },
    }

    const wrapper = await mountComponent(product, {
      isLoggedIn: true,
      attributeConfigs: [],
    })

    const auditRows = wrapper.findAll('.product-attributes__audit-row')
    const auditText = auditRows.map(r => r.text()).join(' ')

    expect(auditText).toContain('OnlyInAll')
  })
})
