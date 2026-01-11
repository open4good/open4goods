import { describe, expect, it } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { defineComponent, h } from 'vue'
import NudgeToolStepCondition from './NudgeToolStepCondition.vue'

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

describe('NudgeToolStepCondition', () => {
  it('blocks unchecked selections when zero results', async () => {
    const wrapper = await mountSuspended(NudgeToolStepCondition, {
      props: {
        modelValue: [],
        isZeroResults: true,
      },
      global: {
        stubs: {
          VRow: VRowStub,
          VCol: VColStub,
          VCard: VCardStub,
          VIcon: VIconStub,
        },
        mocks: {
          $t: (t: string) => t,
        },
      },
    })

    await wrapper.find('.nudge-toggle-card').trigger('click')

    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('allows unchecking when zero results', async () => {
    const wrapper = await mountSuspended(NudgeToolStepCondition, {
      props: {
        modelValue: ['new'],
        isZeroResults: true,
      },
      global: {
        stubs: {
          VRow: VRowStub,
          VCol: VColStub,
          VCard: VCardStub,
          VIcon: VIconStub,
        },
        mocks: {
          $t: (t: string) => t,
        },
      },
    })

    await wrapper.find('.nudge-toggle-card').trigger('click')

    expect(wrapper.emitted('update:modelValue')).toEqual([[[]]])
  })
})
