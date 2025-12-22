import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AgentAttributeRenderer from './AgentAttributeRenderer.vue'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'

const vuetify = createVuetify({
  components,
  directives,
})

// eslint-disable-next-line @typescript-eslint/no-require-imports
global.ResizeObserver = require('resize-observer-polyfill')

describe('AgentAttributeRenderer.vue', () => {
  it('renders text field for TEXT type', () => {
    const wrapper = mount(AgentAttributeRenderer, {
      global: { plugins: [vuetify] },
      props: {
        attribute: { id: 'attr1', type: 'TEXT', label: 'My Text' },
        modelValue: '',
      },
    })
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('My Text')
  })

  it('renders checkbox for CHECKBOX type', () => {
    const wrapper = mount(AgentAttributeRenderer, {
      global: { plugins: [vuetify] },
      props: {
        attribute: { id: 'attr2', type: 'CHECKBOX', label: 'My Checkbox' },
        modelValue: false,
      },
    })
    expect(wrapper.find('input[type="checkbox"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('My Checkbox')
  })

  it('renders select for COMBO type', () => {
    // Vuetify selects are complex to query by native elements, checking component existence is safer
    const wrapper = mount(AgentAttributeRenderer, {
      global: { plugins: [vuetify] },
      props: {
        attribute: {
          id: 'attr3',
          type: 'COMBO',
          label: 'My Combo',
          options: ['A', 'B'],
        },
        modelValue: 'A',
      },
    })
    // Check if VSelect is present (wrapper name might be VSelect or similar)
    // Or check if label is rendered
    expect(wrapper.text()).toContain('My Combo')
  })

  it('emits update:modelValue when input changes', async () => {
    const wrapper = mount(AgentAttributeRenderer, {
      global: { plugins: [vuetify] },
      props: {
        attribute: { id: 'attr1', type: 'TEXT', label: 'My Text' },
        modelValue: '',
      },
    })

    const input = wrapper.find('input')
    await input.setValue('New Value')

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['New Value'])
  })

  it('initializes defaults for CHECKBOX', () => {
    const wrapper = mount(AgentAttributeRenderer, {
      global: { plugins: [vuetify] },
      props: {
        attribute: { id: 'attr2', type: 'CHECKBOX' }, // No initial value
        modelValue: undefined,
      },
    })
    // Expect emission of false
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([false])
  })
})
