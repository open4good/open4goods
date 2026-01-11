import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import NudgeToolStepSubsetGroup from './NudgeToolStepSubsetGroup.vue'

const VRowStub = defineComponent({
  name: 'VRow',
  setup(_, { slots }) {
    return () => h('div', { class: 'v-row-stub' }, slots.default?.())
  },
})

const VColStub = defineComponent({
  name: 'VCol',
  setup(_, { slots }) {
    return () => h('div', { class: 'v-col-stub' }, slots.default?.())
  },
})

const VCardStub = defineComponent({
  name: 'VCard',
  emits: ['click', 'keydown'],
  setup(_, { slots, attrs, emit }) {
    return () =>
      h(
        'div',
        {
          ...attrs,
          class: ['v-card-stub', attrs.class],
          onClick: (event: Event) => emit('click', event),
          onKeydown: (event: KeyboardEvent) => emit('keydown', event),
        },
        slots.default?.()
      )
  },
})

const VIconStub = defineComponent({
  name: 'VIcon',
  props: ['icon'],
  setup(props) {
    return () => h('i', { class: 'v-icon-stub', 'data-icon': props.icon })
  },
})

const VTooltipStub = defineComponent({
  name: 'VTooltip',
  setup(_, { slots }) {
    return () =>
      h('div', { class: 'v-tooltip-stub' }, [
        slots.activator?.({ props: {} }),
        slots.default?.(),
      ])
  },
})

describe('NudgeToolStepSubsetGroup', () => {
  it('blocks unchecked selections when zero results', async () => {
    const wrapper = mount(NudgeToolStepSubsetGroup, {
      props: {
        group: {
          id: 'price',
          title: 'Budget',
        },
        subsets: [
          {
            id: 'price-1',
            title: 'Budget',
          },
        ],
        modelValue: [],
        stepNumber: 2,
        isZeroResults: true,
      },
      global: {
        stubs: {
          VRow: VRowStub,
          VCol: VColStub,
          VCard: VCardStub,
          VIcon: VIconStub,
          VTooltip: VTooltipStub,
        },
      },
    })

    await wrapper.find('.nudge-toggle-card').trigger('click')

    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('allows unchecking when zero results', async () => {
    const wrapper = mount(NudgeToolStepSubsetGroup, {
      props: {
        group: {
          id: 'price',
          title: 'Budget',
        },
        subsets: [
          {
            id: 'price-1',
            title: 'Budget',
          },
        ],
        modelValue: ['price-1'],
        stepNumber: 2,
        isZeroResults: true,
      },
      global: {
        stubs: {
          VRow: VRowStub,
          VCol: VColStub,
          VCard: VCardStub,
          VIcon: VIconStub,
          VTooltip: VTooltipStub,
        },
      },
    })

    await wrapper.find('.nudge-toggle-card').trigger('click')

    expect(wrapper.emitted('update:modelValue')).toEqual([[[]]])
  })
})
