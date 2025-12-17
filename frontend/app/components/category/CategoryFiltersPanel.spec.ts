import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'

import CategoryFiltersPanel from './CategoryFiltersPanel.vue'

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
    return () =>
      h(
        'div',
        { class: 'v-expansion-panels-stub', ...attrs },
        slots.default?.()
      )
  },
})

const VExpansionPanelStub = defineComponent({
  name: 'VExpansionPanel',
  props: { value: { type: String, default: '' } },
  setup(_, { slots }) {
    return () =>
      h('div', { class: 'v-expansion-panel-stub' }, [
        slots.title?.(),
        slots.text?.(),
      ])
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
          class: [
            'v-btn-stub',
            resolveClassList((attrs as Record<string, unknown>).class),
          ].join(' '),
          type: props.type,
          onClick: (event: MouseEvent) => emit('click', event),
        },
        slots.default?.()
      )
  },
})

const CategoryFilterListStub = defineComponent({
  name: 'CategoryFilterList',
  emits: ['update-terms'],
  setup(_, { emit }) {
    return () =>
      h('div', {
        class: 'category-filter-list-stub',
        onClick: () => emit('update-terms', 'brand', ['Acme']),
      })
  },
})

describe('CategoryFiltersPanel', () => {
  it('emits updated filters when CategoryFilterList triggers a terms update', async () => {
    const wrapper = mount(CategoryFiltersPanel, {
      props: {
        filterOptions: null,
        aggregations: [],
        filters: { filters: [] },
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

    await wrapper.find('.category-filter-list-stub').trigger('click')

    const events = wrapper.emitted('update:filters') ?? []
    expect(events).toHaveLength(1)
    expect(events[0]?.[0]).toEqual({
      filters: [
        {
          field: 'brand',
          operator: 'term',
          terms: ['Acme'],
        },
      ],
    })
  })
})
