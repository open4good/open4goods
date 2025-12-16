import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'

import CategoryActiveFilters from './CategoryActiveFilters.vue'
import type { CategorySubsetClause } from '~/types/category-subset'
import type { FieldMetadataDto, FilterRequestDto } from '~~/shared/api-client'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
    n: (value: number, options?: Intl.NumberFormatOptions) =>
      new Intl.NumberFormat('en', options).format(value),
  }),
}))

const resolveClassList = (value: unknown): string => {
  if (!value) {
    return ''
  }

  if (typeof value === 'string') {
    return value
  }

  if (Array.isArray(value)) {
    return value
      .map(entry => resolveClassList(entry))
      .filter(Boolean)
      .join(' ')
  }

  if (typeof value === 'object') {
    return Object.entries(value as Record<string, unknown>)
      .filter(([, active]) => Boolean(active))
      .map(([name]) => name)
      .join(' ')
  }

  return String(value)
}

const VChipStub = defineComponent({
  name: 'VChip',
  props: { closable: { type: Boolean, default: false } },
  emits: ['click:close'],
  setup(props, { attrs, slots, emit }) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          class: [
            'v-chip-stub',
            resolveClassList((attrs as Record<string, unknown>).class),
          ].join(' '),
          type: 'button',
          'data-closable': props.closable ? 'true' : 'false',
          onClick: () => emit('click:close'),
        },
        slots.default?.()
      )
  },
})

const VBtnStub = defineComponent({
  name: 'VBtn',
  emits: ['click'],
  setup(_, { attrs, slots, emit }) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          class: [
            'v-btn-stub',
            resolveClassList((attrs as Record<string, unknown>).class),
          ].join(' '),
          type: 'button',
          onClick: (event: MouseEvent) => emit('click', event),
        },
        slots.default?.()
      )
  },
})

const VIconStub = defineComponent({
  name: 'VIcon',
  props: { icon: { type: String, default: '' } },
  setup(props) {
    return () => h('span', { class: 'v-icon-stub', 'data-icon': props.icon })
  },
})

const VTooltipStub = defineComponent({
  name: 'VTooltip',
  props: { text: { type: String, default: '' } },
  setup(_, { slots }) {
    return () =>
      h('div', { class: 'v-tooltip-stub' }, [
        slots.activator?.({ props: {} }),
        slots.default?.(),
      ])
  },
})

describe('CategoryActiveFilters', () => {
  const subsetClause: CategorySubsetClause = {
    id: 'subset-0',
    subsetId: 'subset',
    index: 0,
    label: 'Subset filter',
    filter: { field: 'price', operator: 'term', terms: ['500'] },
  }

  const manualFilters: FilterRequestDto = {
    filters: [{ field: 'brand', operator: 'term', terms: ['Acme'] }],
  }

  const fieldMetadata: Record<string, FieldMetadataDto> = {
    brand: { mapping: 'brand', title: 'Brand' } as FieldMetadataDto,
  }

  type ComponentProps = {
    filters: FilterRequestDto | null
    subsetClauses: CategorySubsetClause[]
    fieldMetadata: Record<string, FieldMetadataDto>
  }

  const mountComponent = (overrides: Partial<ComponentProps> = {}) =>
    mount(CategoryActiveFilters, {
      props: {
        filters: manualFilters,
        subsetClauses: [subsetClause],
        fieldMetadata,
        ...overrides,
      },
      global: {
        stubs: {
          VChip: VChipStub,
          VBtn: VBtnStub,
          VIcon: VIconStub,
          VTooltip: VTooltipStub,
        },
      },
    })

  it('renders subset and manual chips and emits removal events', async () => {
    const wrapper = mountComponent()

    const chips = wrapper.findAll('.v-chip-stub')
    expect(chips).toHaveLength(2)

    expect(chips[0]?.text()).toContain('Subset filter')
    expect(chips[1]?.text()).toContain('Brand: Acme')

    await chips[0]?.trigger('click')
    await chips[1]?.trigger('click')

    const subsetEvents = wrapper.emitted('remove-subset-clause') ?? []
    expect(subsetEvents).toHaveLength(1)
    expect(subsetEvents[0]?.[0]).toEqual(subsetClause)

    const manualEvents = wrapper.emitted('remove-filter') ?? []
    expect(manualEvents).toHaveLength(1)
    expect(manualEvents[0]).toEqual(['brand', 'term', 'Acme'])
  })

  it('emits clear-all when the clear button is clicked', async () => {
    const wrapper = mountComponent()

    await wrapper.find('.v-btn-stub').trigger('click')

    const events = wrapper.emitted('clear-all') ?? []
    expect(events).toHaveLength(1)
  })

  it('renders nothing when there are no active filters', () => {
    const wrapper = mountComponent({ filters: {}, subsetClauses: [] })

    expect(wrapper.html()).toBe('<!--v-if-->')
  })

  it('formats range filters with one decimal place in chip labels', () => {
    const wrapper = mountComponent({
      filters: {
        filters: [
          {
            field: 'weight',
            operator: 'range',
            min: 1.234,
            max: 5.678,
          },
        ],
      },
      subsetClauses: [],
      fieldMetadata: {
        weight: { mapping: 'weight', title: 'Weight' } as FieldMetadataDto,
      },
    })

    const chipTexts = wrapper.findAll('.v-chip-stub').map(chip => chip.text())
    expect(chipTexts).toContain('Weight: 1.2 → 5.7')
  })
})
