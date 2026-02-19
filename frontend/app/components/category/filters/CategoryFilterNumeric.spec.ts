import { describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import type { PropType } from 'vue'
import type {
  AggregationResponseDto,
  FieldMetadataDto,
  Filter,
} from '~~/shared/api-client'

import CategoryFilterNumeric from './CategoryFilterNumeric.vue'

vi.mock('~/composables/usePluralizedTranslation', () => ({
  usePluralizedTranslation: () => ({
    translatePlural: () => '',
  }),
}))

const createI18nPlugin = () =>
  createI18n({
    legacy: false,
    locale: 'en',
    messages: { en: { 'category.filters.rangeAriaSuffix': 'range' } },
  })

const RangeSliderStub = defineComponent({
  name: 'RangeSliderStub',
  props: {
    modelValue: {
      type: Array as PropType<[number, number]>,
      default: () => [0, 0],
    },
  },
  emits: ['update:modelValue', 'end'],
  setup(props, { slots }) {
    return () =>
      h('div', { class: 'v-range-slider-stub' }, [
        slots['thumb-label']?.({ modelValue: props.modelValue }),
      ])
  },
})

const mountComponent = (filter: Filter) => {
  const aggregation: AggregationResponseDto = {
    min: 0,
    max: 10,
    buckets: [],
  }

  const field: FieldMetadataDto = {
    mapping: 'weight',
    title: 'Weight',
  }

  const i18n = createI18nPlugin()

  return mount(CategoryFilterNumeric, {
    props: {
      field,
      aggregation,
      modelValue: filter,
    },
    global: {
      plugins: [i18n],
      stubs: {
        ClientOnly: defineComponent({
          name: 'ClientOnlyStub',
          setup(_, { slots }) {
            return () => slots.default?.()
          },
        }),
        VueECharts: defineComponent({
          name: 'VueEChartsStub',
          setup() {
            return () => h('div', { class: 'echarts-stub' })
          },
        }),
        'v-range-slider': RangeSliderStub,
      },
    },
  })
}

describe('CategoryFilterNumeric', () => {
  it('displays the range label with at most one decimal', () => {
    const filter: Filter = {
      field: 'weight',
      operator: 'range',
      min: 1.234,
      max: 5.678,
    }

    const wrapper = mountComponent(filter)
    expect(wrapper.find('.category-filter-numeric__range').text()).toBe(
      '1.2 kg → 5.7 kg'
    )
  })
})
