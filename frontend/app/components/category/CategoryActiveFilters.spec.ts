import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'

import CategoryActiveFilters from './CategoryActiveFilters.vue'
import type { CategorySubsetClause } from '~/types/category-subset'
import type { FilterRequestDto, ProductFieldOptionsResponse } from '~~/shared/api-client'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

const VChipStub = defineComponent({
  name: 'VChip',
  emits: ['click:close'],
  setup(_, { slots, emit, attrs }) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          class: ['v-chip-stub', attrs.class].filter(Boolean).join(' '),
          type: 'button',
          onClick: () => emit('click:close'),
        },
        slots.default?.(),
      )
  },
})

const VBtnStub = defineComponent({
  name: 'VBtn',
  emits: ['click'],
  setup(_, { slots, emit, attrs }) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          type: 'button',
          class: ['v-btn-stub', attrs.class].filter(Boolean).join(' '),
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
  setup(_, { slots }) {
    return () => h('div', { class: 'v-tooltip-stub' }, slots.default?.())
  },
})

describe('CategoryActiveFilters', () => {
  const subsetClause: CategorySubsetClause = {
    id: 'subset-0',
    subsetId: 'subset',
    index: 0,
    label: 'Energy rating A',
    filter: { field: 'energy', operator: 'term', terms: ['A'] },
  }

  const manualFilters: FilterRequestDto = {
    filters: [{ field: 'brand', operator: 'term', terms: ['Acme'] }],
  }

  const filterOptions: ProductFieldOptionsResponse = {
    global: [
      {
        mapping: 'brand',
        type: 'keyword',
        label: 'Brand',
      },
    ],
    impact: [],
    technical: [],
  }

  it('renders active filters and emits events for removal and clearing', () => {
    const wrapper = mount(CategoryActiveFilters, {
      props: {
        filters: manualFilters,
        subsetClauses: [subsetClause],
        filterOptions,
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

    const chips = wrapper.findAll('.v-chip-stub')
    expect(chips).toHaveLength(2)
    expect(chips[0]?.text()).toContain('Energy rating A')
    expect(chips[1]?.text()).toContain('brand: Acme')

    chips[0]?.trigger('click')
    const subsetEvents = wrapper.emitted('remove-subset-clause') ?? []
    expect(subsetEvents).toHaveLength(1)
    expect(subsetEvents[0]?.[0]).toEqual(subsetClause)

    chips[1]?.trigger('click')
    const filterEvents = wrapper.emitted('update:filters') ?? []
    expect(filterEvents).toHaveLength(1)
    expect(filterEvents[0]?.[0]).toEqual({})

    const clearButton = wrapper.get('.v-btn-stub')
    clearButton.trigger('click')

    const clearEvents = wrapper.emitted('clear-all') ?? []
    expect(clearEvents).toHaveLength(1)
  })
})
