import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import CategoryFiltersPanel from './CategoryFiltersPanel.vue'
import type { FilterRequestDto } from '~~/shared/api-client'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

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
  const manualFilters: FilterRequestDto = {
    filters: [{ field: 'brand', operator: 'term', terms: ['Acme'] }],
  }

  it('emits updated filters when a range filter changes', () => {
    const wrapper = mount(CategoryFiltersPanel, {
      props: {
        filterOptions: null,
        aggregations: [],
        filters: manualFilters,
        impactExpanded: false,
        technicalExpanded: false,
      },
      global: {
        stubs: {
          VIcon: VIconStub,
          VExpansionPanels: VExpansionPanelsStub,
          VExpansionPanel: VExpansionPanelStub,
          VBtn: VBtnStub,
          CategoryFilterList: CategoryFilterListStub,
        },
      },
    })

    const lists = wrapper.findAllComponents(CategoryFilterListStub)
    expect(lists).not.toHaveLength(0)

    lists[0]?.vm.$emit('update-range', 'price.min', { min: 10 })

    let emitted = wrapper.emitted('update:filters') ?? []
    expect(emitted[0]?.[0]).toEqual({
      filters: [
        { field: 'brand', operator: 'term', terms: ['Acme'] },
        { field: 'price.min', operator: 'range', min: 10, max: undefined },
      ],
    })

    lists[0]?.vm.$emit('update-range', 'price.min', {})

    emitted = wrapper.emitted('update:filters') ?? []
    expect(emitted[1]?.[0]).toEqual({
      filters: [{ field: 'brand', operator: 'term', terms: ['Acme'] }],
    })
  })
})
