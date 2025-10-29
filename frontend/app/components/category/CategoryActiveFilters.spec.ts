import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'

import CategoryActiveFilters from './CategoryActiveFilters.vue'
import type { FilterRequestDto, ProductFieldOptionsResponse } from '~~/shared/api-client'
import type { CategorySubsetClause } from '~/types/category-subset'

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

const VChipStub = defineComponent({
  name: 'VChip',
  props: {
    closable: { type: Boolean, default: false },
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
  setup(props, { slots }) {
    return () =>
      h('div', { class: 'v-tooltip-stub', 'data-text': props.text }, [
        slots.activator?.({ props: {} }),
        slots.default?.(),
      ])
  },
})

describe('CategoryActiveFilters', () => {
  const filterOptions: ProductFieldOptionsResponse = {
    global: [
      { mapping: 'brand', title: 'Brand' },
      { mapping: 'price.min', title: 'Price min' },
    ],
    impact: [],
    technical: [],
  }

  const manualFilters: FilterRequestDto = {
    filters: [
      { field: 'brand', operator: 'term', terms: ['Nudger'] },
      { field: 'price.min', operator: 'range', min: 10, max: 100 },
    ],
  }

  const subsetClauses: CategorySubsetClause[] = [
    {
      id: 'subset-1-0',
      subsetId: 'subset-1',
      index: 0,
      label: 'Circular products',
      filter: { field: 'recycled', operator: 'term', terms: ['yes'] },
    },
  ]

  const mountComponent = () =>
    mount(CategoryActiveFilters, {
      props: {
        filterOptions,
        filters: manualFilters,
        subsetClauses,
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

  const mountWithNoChips = () =>
    mount(CategoryActiveFilters, {
      props: {
        filterOptions,
        filters: {},
        subsetClauses: [],
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

  it('renders subset and manual filter chips and emits removal events', () => {
    const wrapper = mountComponent()

    const chips = wrapper.findAll('.v-chip-stub')
    expect(chips).toHaveLength(3)

    chips[0]?.trigger('click')
    expect(wrapper.emitted('remove-subset-clause')?.[0]?.[0]).toEqual(subsetClauses[0])

    chips[1]?.trigger('click')
    expect(wrapper.emitted('remove-manual-filter')?.[0]?.[0]).toEqual({
      field: 'brand',
      type: 'term',
      term: 'Nudger',
    })
  })

  it('emits clear-all when the dismiss button is pressed', () => {
    const wrapper = mountComponent()

    wrapper.get('.v-btn-stub').trigger('click')

    expect(wrapper.emitted('clear-all')).toBeTruthy()
  })

  it('hides the container when no filters or subsets are active', () => {
    const wrapper = mountWithNoChips()

    expect(wrapper.find('[data-testid="category-active-filters"]').exists()).toBe(false)
  })
})
