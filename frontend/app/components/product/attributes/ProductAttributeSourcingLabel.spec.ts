import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h, reactive, ref } from 'vue'
import ProductAttributeSourcingLabel from './ProductAttributeSourcingLabel.vue'
import type {
  ProductAttributeSourceDto,
  ProductSourcedAttributeDto,
} from '~~/shared/api-client'

const VTooltipStub = defineComponent({
  name: 'VTooltipStub',
  props: { text: { type: String, default: '' } },
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
    ariaLabel: { type: String, default: '' },
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
          'aria-label': props.ariaLabel,
          type: 'button',
        },
        slots.default?.()
      )
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  props: {
    icon: { type: String, default: '' },
    color: { type: String, default: '' },
  },
  setup(props) {
    return () =>
      h('i', { class: 'v-icon-stub', 'data-color': props.color }, props.icon)
  },
})

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

const VDividerStub = defineComponent({
  name: 'VDividerStub',
  setup() {
    return () => h('hr', { class: 'v-divider-stub' })
  },
})

const VChipStub = defineComponent({
  name: 'VChipStub',
  setup(_, { slots }) {
    return () => h('span', { class: 'v-chip-stub' }, slots.default?.())
  },
})

const i18n = createI18n({
  legacy: false,
  locale: 'en-US',
  messages: {
    'en-US': {
      product: {
        attributes: {
          sourcing: {
            bestValue: 'Best value',
            sourceCount: {
              one: '{count} source',
              other: '{count} sources',
            },
            columns: {
              source: 'Source',
              value: 'Value',
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
    },
  },
})

describe('ProductAttributeSourcingLabel', () => {
  const mountComponent = (
    sourcing?: ProductAttributeSourceDto | null,
    value = 'Fallback',
    extraProps: Record<string, unknown> = {}
  ) =>
    mount(ProductAttributeSourcingLabel, {
      props: { sourcing, value, ...extraProps },
      global: {
        plugins: [i18n],
        stubs: {
          VTooltip: VTooltipStub,
          VBtn: VBtnStub,
          VIcon: VIconStub,
          VCard: VCardStub,
          VTable: VTableStub,
          VDivider: VDividerStub,
          VChip: VChipStub,
        },
      },
    })

  it('renders best value and conflict status', () => {
    const sourcing: ProductAttributeSourceDto = {
      bestValue: 'Argent',
      conflicts: true,
      sources: new Set([
        {
          datasourceName: 'fnac.com',
          value: 'Argent',
          language: 'fr',
          icecatTaxonomyId: 46757,
        },
      ]),
    }

    const wrapper = mountComponent(sourcing, 'Original value')

    expect(wrapper.text()).toContain('Argent')

    const icon = wrapper.find('.v-icon-stub')
    expect(icon.attributes('data-color')).toBe('warning')

    const tableRows = wrapper.findAll('.v-table-stub tbody tr')
    expect(tableRows).toHaveLength(1)
    expect(tableRows[0].text()).toContain('fnac.com')

    const countLabel = wrapper.find(
      '.product-attribute-sourcing__tooltip-count'
    )
    expect(countLabel.text()).toContain('1 source')
  })

  it('hides tooltip when sourcing is missing', () => {
    const wrapper = mountComponent(null, 'Displayed value')
    expect(wrapper.find('.v-btn-stub').exists()).toBe(false)
    expect(wrapper.text()).toContain('Displayed value')
  })

  it('can disable tooltip even when sourcing exists', () => {
    const sourcing: ProductAttributeSourceDto = {
      bestValue: 'Argent',
      conflicts: false,
      sources: [
        {
          datasourceName: 'fnac.com',
          value: 'Argent',
        },
      ],
    }

    const wrapper = mountComponent(sourcing, 'Argent', { enableTooltip: false })
    expect(wrapper.find('.v-btn-stub').exists()).toBe(false)
    expect(wrapper.text()).toContain('Argent')
  })

  it('renders sources when provided through a proxied Set', () => {
    const reactiveSources = reactive(
      new Set<ProductSourcedAttributeDto>([
        {
          datasourceName: 'icecat.biz',
          value: '9',
          language: null,
          icecatTaxonomyId: 3566,
        },
      ])
    )

    const sourcing: ProductAttributeSourceDto = {
      bestValue: '9',
      conflicts: false,
      sources: reactiveSources as unknown as Set<ProductSourcedAttributeDto>,
    }

    const wrapper = mountComponent(sourcing, '9')

    const rows = wrapper.findAll('.v-table-stub tbody tr')
    expect(rows).toHaveLength(1)
    expect(rows[0].text()).toContain('icecat.biz')

    const countLabel = wrapper.find(
      '.product-attribute-sourcing__tooltip-count'
    )
    expect(countLabel.text()).toContain('1 source')
  })

  it('renders sources when provided through a ref of array values', () => {
    const sourcesRef = ref<ProductSourcedAttributeDto[]>([
      {
        datasourceName: 'eprel',
        value: '269',
        language: null,
        name: 'POWERONMODEHDR',
      },
    ])

    const sourcing: ProductAttributeSourceDto = {
      bestValue: '269',
      conflicts: false,
      sources: sourcesRef as unknown as ProductAttributeSourceDto['sources'],
    }

    const wrapper = mountComponent(sourcing, '269')

    const rows = wrapper.findAll('.v-table-stub tbody tr')
    expect(rows).toHaveLength(1)
    expect(rows[0].text()).toContain('eprel')

    const countLabel = wrapper.find(
      '.product-attribute-sourcing__tooltip-count'
    )
    expect(countLabel.text()).toContain('1 source')
  })
})
