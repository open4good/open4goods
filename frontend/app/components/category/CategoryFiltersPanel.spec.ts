import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import CategoryFiltersPanel from './CategoryFiltersPanel.vue'
import type { CategorySubsetClause } from '~/types/category-subset'
import type { FilterRequestDto } from '~~/shared/api-client'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
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
    return value.map((entry) => resolveClassList(entry)).filter(Boolean).join(' ')
  }

  if (typeof value === 'object') {
    return Object.entries(value as Record<string, unknown>)
      .filter(([, active]) => Boolean(active))
      .map(([name]) => name)
      .join(' ')
  }

  return String(value)
}

const VChipGroupStub = defineComponent({
  name: 'VChipGroup',
  setup(_, { slots, attrs }) {
    const className = ['v-chip-group-stub', resolveClassList((attrs as Record<string, unknown>).class)]
      .filter(Boolean)
      .join(' ')

    return () => h('div', { ...attrs, class: className }, slots.default?.())
  },
})

const VChipStub = defineComponent({
  name: 'VChip',
  props: {
    closable: { type: Boolean, default: false },
    variant: { type: String, default: 'flat' },
  },
  emits: ['click:close'],
  setup(props, { slots, emit, attrs }) {
    const className = ['v-chip-stub', resolveClassList((attrs as Record<string, unknown>).class)]
      .filter(Boolean)
      .join(' ')

    return () =>
      h(
        'button',
        {
          ...attrs,
          class: className,
          type: 'button',
          'data-closable': props.closable ? 'true' : 'false',
          onClick: () => emit('click:close'),
        },
        slots.default?.(),
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

const VExpansionPanelsStub = defineComponent({
  name: 'VExpansionPanels',
  setup(_, { slots, attrs }) {
    return () => h('div', { class: 'v-expansion-panels-stub', ...attrs }, slots.default?.())
  },
})

const VExpansionPanelStub = defineComponent({
  name: 'VExpansionPanel',
  props: { value: { type: String, default: '' } },
  setup(_, { slots }) {
    return () => h('div', { class: 'v-expansion-panel-stub' }, [slots.title?.(), slots.text?.()])
  },
})

const VBtnStub = defineComponent({
  name: 'VBtn',
  props: { type: { type: String, default: 'button' } },
  emits: ['click'],
  setup(props, { slots, emit, attrs }) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          class: ['v-btn-stub', resolveClassList((attrs as Record<string, unknown>).class)].join(' '),
          type: props.type,
          onClick: (event: MouseEvent) => emit('click', event),
        },
        slots.default?.(),
      )
  },
})

const CategoryFilterListStub = defineComponent({
  name: 'CategoryFilterList',
  setup(_, { slots }) {
    return () => h('div', { class: 'category-filter-list-stub' }, slots.default?.())
  },
})

describe('CategoryFiltersPanel', () => {
  const subsetClause: CategorySubsetClause = {
    id: 'subset-0',
    subsetId: 'subset',
    index: 0,
    label: 'Price ≤ 500',
    filter: { field: 'price.min', operator: 'range', max: 500 },
  }

  const manualFilters: FilterRequestDto = {
    filters: [{ field: 'brand', operator: 'term', terms: ['Acme'] }],
  }

  it('renders subset and manual filter chips together and emits removal events', () => {
    const wrapper = mount(CategoryFiltersPanel, {
      props: {
        filterOptions: null,
        aggregations: [],
        filters: manualFilters,
        impactExpanded: false,
        technicalExpanded: false,
        subsetClauses: [subsetClause],
      },
      global: {
        stubs: {
          VChipGroup: VChipGroupStub,
          VChip: VChipStub,
          VIcon: VIconStub,
          VExpansionPanels: VExpansionPanelsStub,
          VExpansionPanel: VExpansionPanelStub,
          VBtn: VBtnStub,
          CategoryFilterList: CategoryFilterListStub,
        },
      },
    })

    const chips = wrapper.findAll('.v-chip-stub')
    expect(chips).toHaveLength(2)

    expect(chips[0]?.text()).toContain('Price ≤ 500')
    expect(chips[1]?.text()).toContain('brand: Acme')

    chips[0]?.trigger('click')

    const removalEvents = wrapper.emitted('remove-subset-clause') ?? []
    expect(removalEvents).toHaveLength(1)
    expect(removalEvents[0]?.[0]).toEqual(subsetClause)
  })
})
