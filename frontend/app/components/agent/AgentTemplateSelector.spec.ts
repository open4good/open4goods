import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import AgentTemplateSelector from './AgentTemplateSelector.vue'

describe('AgentTemplateSelector', () => {
  const baseTemplate = {
    id: 'test',
    name: 'Test Agent',
    description: 'Desc',
    icon: 'mdi-robot',
    promptTemplates: [],
    tags: [],
    allowedRoles: [],
    publicPromptHistory: true,
    allowTemplateEditing: false,
  }

  const stubs = {
    VContainer: { template: '<div><slot /></div>' },
    VRow: { template: '<div><slot /></div>' },
    VCol: { template: '<div><slot /></div>' },
    VCard: {
      template:
        '<div data-test="agent-template-card" @click="$emit(\'click\')"><slot /></div>',
    },
    VCardItem: { template: '<div><slot /><slot name="prepend" /></div>' },
    VIcon: { template: '<span />' },
    VCardTitle: { template: '<div><slot /></div>' },
    VCardSubtitle: { template: '<div><slot /></div>' },
    VCardText: { template: '<div><slot /></div>' },
    VCardActions: { template: '<div><slot /></div>' },
    VChip: { template: '<span><slot /></span>' },
    VBtn: { template: '<button><slot /></button>' },
    VSpacer: { template: '<span />' },
    VDivider: { template: '<hr />' },
  }

  it('emits blocked when template is not authorized', async () => {
    const wrapper = mount(AgentTemplateSelector, {
      props: {
        templates: [{ ...baseTemplate, isAuthorized: false }],
      },
      global: {
        stubs,
        mocks: {
          $t: (key: string) => key,
        },
      },
    })

    await wrapper.get('[data-test="agent-template-card"]').trigger('click')

    expect(wrapper.emitted('blocked')).toBeTruthy()
    expect(wrapper.emitted('select')).toBeUndefined()
  })

  it('emits select when template is allowed', async () => {
    const wrapper = mount(AgentTemplateSelector, {
      props: {
        templates: [{ ...baseTemplate, isAuthorized: true }],
      },
      global: {
        stubs,
        mocks: {
          $t: (key: string) => key,
        },
      },
    })

    await wrapper.get('[data-test="agent-template-card"]').trigger('click')

    expect(wrapper.emitted('select')).toBeTruthy()
    expect(wrapper.emitted('blocked')).toBeUndefined()
  })
})
