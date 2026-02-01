import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'

import CategoryResultsToolbar from './CategoryResultsToolbar.vue'

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
          onClick: () => emit('click'),
        },
        slots.default?.()
      )
  },
})

const VBtnToggleStub = defineComponent({
  name: 'VBtnToggle',
  emits: ['update:modelValue'],
  setup(_, { attrs, slots }) {
    return () =>
      h(
        'div',
        {
          ...attrs,
          class: [
            'v-btn-toggle-stub',
            resolveClassList((attrs as Record<string, unknown>).class),
          ].join(' '),
        },
        slots.default?.()
      )
  },
})

const VSelectStub = defineComponent({
  name: 'VSelect',
  props: {
    modelValue: { type: String, default: null },
  },
  emits: ['update:modelValue'],
  setup(props, { attrs, emit }) {
    return () =>
      h('div', {
        ...attrs,
        class: [
          'v-select-stub',
          resolveClassList((attrs as Record<string, unknown>).class),
        ].join(' '),
        'data-model': props.modelValue ?? '',
        onClick: () => emit('update:modelValue', 'offersCount'),
      })
  },
})

const VTextFieldStub = defineComponent({
  name: 'VTextField',
  props: {
    modelValue: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { attrs, emit }) {
    return () =>
      h('input', {
        ...attrs,
        class: [
          'v-text-field-stub',
          resolveClassList((attrs as Record<string, unknown>).class),
        ].join(' '),
        value: props.modelValue,
        onInput: () => emit('update:modelValue', 'query'),
      })
  },
})

const VTooltipStub = defineComponent({
  name: 'VTooltip',
  setup(_, { slots }) {
    return () =>
      h('div', { class: 'v-tooltip-stub' }, slots.activator?.({ props: {} }))
  },
})

const VIconStub = defineComponent({
  name: 'VIcon',
  setup(_, { attrs }) {
    return () => h('span', { ...attrs, class: 'v-icon-stub' })
  },
})

const VBadgeStub = defineComponent({
  name: 'VBadge',
  setup(_, { attrs, slots }) {
    return () =>
      h('span', { ...attrs, class: 'v-badge-stub' }, slots.default?.())
  },
})

const CategoryResultsCountStub = defineComponent({
  name: 'CategoryResultsCount',
  props: {
    count: { type: Number, default: 0 },
  },
  setup(props) {
    return () =>
      h('span', { class: 'category-results-count-stub' }, `${props.count}`)
  },
})

describe('CategoryResultsToolbar', () => {
  const mountComponent = () =>
    mount(CategoryResultsToolbar, {
      props: {
        isDesktop: false,
        resultsCount: 12,
        viewMode: 'cards',
        sortItems: [
          { title: 'Price', value: 'price.minPrice.price' },
          { title: 'Offers', value: 'offersCount' },
        ],
        sortField: 'price.minPrice.price',
        sortOrder: 'desc',
        searchTerm: 'phone',
        showFiltersButton: true,
        filtersCount: 2,
      },
      global: {
        stubs: {
          VBtn: VBtnStub,
          VBtnToggle: VBtnToggleStub,
          VSelect: VSelectStub,
          VTextField: VTextFieldStub,
          VTooltip: VTooltipStub,
          VIcon: VIconStub,
          VBadge: VBadgeStub,
          CategoryResultsCount: CategoryResultsCountStub,
        },
      },
    })

  it('emits filter toggle when the mobile filters button is clicked', async () => {
    const wrapper = mountComponent()

    await wrapper
      .find('[data-testid="results-toolbar-filter-button"]')
      .trigger('click')

    expect(wrapper.emitted('toggle-filters')).toHaveLength(1)
  })

  it('emits search, sort, and view updates', async () => {
    const wrapper = mountComponent()

    await wrapper.find('.v-text-field-stub').trigger('input')
    expect(wrapper.emitted('update:searchTerm')?.[0]).toEqual(['query'])

    await wrapper.find('.v-select-stub').trigger('click')
    expect(wrapper.emitted('update:sortField')?.[0]).toEqual(['offersCount'])

    await wrapper.setProps({ isDesktop: true })
    const toggleButtons = wrapper.findAllComponents(VBtnToggleStub)
    const viewToggle = toggleButtons.find(
      toggle =>
        toggle.attributes()['data-testid'] === 'results-toolbar-view-toggle'
    )
    viewToggle?.vm.$emit('update:modelValue', 'table')
    expect(wrapper.emitted('update:viewMode')?.[0]).toEqual(['table'])

    const sortToggle = toggleButtons.find(
      toggle =>
        toggle.attributes()['data-testid'] === 'results-toolbar-sort-order'
    )
    sortToggle?.vm.$emit('update:modelValue', 'asc')
    expect(wrapper.emitted('update:sortOrder')?.[0]).toEqual(['asc'])
  })
})
