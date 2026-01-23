import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import type {
  AggregationResponseDto,
  FieldMetadataDto,
  Filter,
} from '~~/shared/api-client'

import CategoryFilterCondition from './CategoryFilterCondition.vue'

const VCheckboxStub = defineComponent({
  name: 'VCheckboxStub',
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:modelValue'],
  setup(props, { emit, slots }) {
    const toggle = () => emit('update:modelValue', !props.modelValue)
    return () =>
      h(
        'button',
        { class: 'v-checkbox-stub', type: 'button', onClick: toggle },
        slots.label ? slots.label() : []
      )
  },
})

const createI18nPlugin = () =>
  createI18n({
    legacy: false,
    locale: 'en',
    messages: {
      en: {
        'category.filters.fields.condition': 'Offer condition',
        'category.filters.condition.new': 'New',
        'category.filters.condition.used': 'Used',
      },
    },
  })

const mountComponent = (filter?: Filter | null) => {
  const aggregation: AggregationResponseDto = {
    buckets: [
      { key: 'NEW', count: 5 },
      { key: 'OCCASION', count: 2 },
    ],
  }

  const field: FieldMetadataDto = {
    mapping: 'price.conditions',
    title: '',
    valueType: 'keyword',
  }

  return mount(CategoryFilterCondition, {
    props: {
      field,
      aggregation,
      modelValue: filter ?? null,
    },
    global: {
      plugins: [createI18nPlugin()],
      stubs: {
        'v-checkbox': VCheckboxStub,
      },
    },
  })
}

describe('CategoryFilterCondition', () => {
  it('renders condition labels and counts', () => {
    const wrapper = mountComponent()

    const text = wrapper.text()
    expect(text).toContain('Offer condition')
    expect(text).toContain('New')
    expect(text).toContain('Used')
    expect(text).toContain('5')
    expect(text).toContain('2')
  })

  it('emits term filters when toggled', async () => {
    const wrapper = mountComponent()
    const checkboxes = wrapper.findAll('.v-checkbox-stub')

    await checkboxes[0].trigger('click')

    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted?.[0]?.[0]).toEqual({
      field: 'price.conditions',
      operator: 'term',
      terms: ['NEW'],
    })
  })
})
