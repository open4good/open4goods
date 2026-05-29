import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import type {
  AggregationResponseDto,
  FieldMetadataDto,
  Filter,
} from '~~/shared/api-client'

import CategoryFilterCompetition from './CategoryFilterCompetition.vue'

const VChipStub = defineComponent({
  name: 'VChipStub',
  emits: ['click'],
  setup(_, { emit, slots }) {
    return () =>
      h(
        'button',
        { class: 'v-chip-stub', type: 'button', onClick: () => emit('click') },
        slots.default ? slots.default() : []
      )
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  setup: () => () => h('span', { class: 'v-icon-stub' }),
})

const createI18nPlugin = () =>
  createI18n({
    legacy: false,
    locale: 'en',
    messages: {
      en: {
        'category.filters.competition.title': 'Competition',
        'category.filters.competition.low': 'Low',
        'category.filters.competition.medium': 'Medium',
        'category.filters.competition.high': 'High',
      },
    },
  })

const field: FieldMetadataDto = { mapping: 'offersCount', valueType: 'numeric' }

const mountComponent = (
  aggregation: AggregationResponseDto,
  filter?: Filter | null
) =>
  mount(CategoryFilterCompetition, {
    props: { field, aggregation, modelValue: filter ?? null },
    global: {
      plugins: [createI18nPlugin()],
      stubs: {
        'v-chip': VChipStub,
        'v-icon': VIconStub,
        CategoryFilterNumeric: defineComponent({
          name: 'CategoryFilterNumericStub',
          setup: () => () => h('div', { class: 'numeric-fallback' }),
        }),
      },
    },
  })

const coherentAggregation: AggregationResponseDto = {
  buckets: [
    { key: '1', count: 4 },
    { key: '3', count: 6 },
    { key: '7', count: 2 },
  ],
}

describe('CategoryFilterCompetition', () => {
  it('renders three bands with summed counts when coherent', () => {
    const wrapper = mountComponent(coherentAggregation)
    const chips = wrapper.findAll('.v-chip-stub')
    expect(chips).toHaveLength(3)
    expect(wrapper.text()).toContain('Low')
    expect(wrapper.text()).toContain('Medium')
    expect(wrapper.text()).toContain('High')
    // low=4 (key 1), medium=6 (key 3), high=2 (key 7)
    expect(wrapper.text()).toContain('4')
    expect(wrapper.text()).toContain('6')
  })

  it('emits the matching range when a band is clicked', async () => {
    const wrapper = mountComponent(coherentAggregation)
    await wrapper.findAll('.v-chip-stub')[0].trigger('click')
    expect(wrapper.emitted('update:modelValue')?.[0]?.[0]).toEqual({
      field: 'offersCount',
      operator: 'range',
      min: 1,
      max: 2,
    })
  })

  it('emits the open-ended high band range', async () => {
    const wrapper = mountComponent(coherentAggregation)
    await wrapper.findAll('.v-chip-stub')[2].trigger('click')
    expect(wrapper.emitted('update:modelValue')?.[0]?.[0]).toEqual({
      field: 'offersCount',
      operator: 'range',
      min: 5,
      max: undefined,
    })
  })

  it('toggles a band off when it is already active', async () => {
    const wrapper = mountComponent(coherentAggregation, {
      field: 'offersCount',
      operator: 'range',
      min: 1,
      max: 2,
    })
    await wrapper.findAll('.v-chip-stub')[0].trigger('click')
    expect(wrapper.emitted('update:modelValue')?.[0]?.[0]).toBeNull()
  })

  it('falls back to the numeric filter when the spread is not coherent', () => {
    const wrapper = mountComponent({ buckets: [{ key: '1', count: 9 }] })
    expect(wrapper.find('.numeric-fallback').exists()).toBe(true)
    expect(wrapper.findAll('.v-chip-stub')).toHaveLength(0)
  })
})
