import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { defineComponent } from 'vue'
import AgentPromptShell from './AgentPromptShell.vue'

vi.mock('@hcaptcha/vue3-hcaptcha', () => ({
  default: defineComponent({ name: 'VueHcaptcha', template: '<div />' }),
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

mockNuxtImport('useRuntimeConfig', () => () => ({
  public: { hcaptchaSiteKey: '' },
}))
mockNuxtImport('useI18n', () => () => ({ t: (key: string) => key }))

const VTextareaStub = defineComponent({
  name: 'VTextareaStub',
  inheritAttrs: false,
  props: { modelValue: { type: String, default: '' } },
  emits: ['update:modelValue'],
  template:
    '<textarea v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)"></textarea>',
})

const VBtnStub = defineComponent({
  name: 'VBtnStub',
  inheritAttrs: false,
  props: { disabled: { type: Boolean, default: false } },
  emits: ['click'],
  template:
    '<button v-bind="$attrs" :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
})

const baseStubs = {
  ClientOnly: { template: '<div><slot /></div>' },
  VCard: {
    template:
      '<div><slot /><slot name="title" /><slot name="subtitle" /></div>',
  },
  VCardTitle: { template: '<div><slot /></div>' },
  VCardSubtitle: { template: '<div><slot /></div>' },
  VCardText: { template: '<div><slot /></div>' },
  VAlert: { template: '<div><slot /></div>' },
  VSelect: { template: '<select><slot /></select>' },
  VTextarea: VTextareaStub,
  VCheckbox: { template: '<input type="checkbox" />' },
  VBtn: VBtnStub,
  VIcon: { template: '<span />' },
  VDivider: { template: '<div />' },
  VCardActions: { template: '<div><slot /></div>' },
  VSpacer: { template: '<span />' },
  VChip: { template: '<span><slot /></span>' },
}

const globalConfig = {
  stubs: baseStubs,
  mocks: {
    $t: (key: string) => key,
  },
}

describe('AgentPromptShell', () => {
  it('prevents submission when not authorized', () => {
    const wrapper = mount(AgentPromptShell, {
      props: {
        title: 'Test agent',
        promptTemplates: [{ id: 'p1', title: 'Prompt', content: 'content' }],
        allowTemplateEditing: true,
        isAuthorized: false,
      },
      global: globalConfig,
    })

    const submit = wrapper.get('[data-test="agent-submit"]')
    expect(submit.attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="agent-locked"]').exists()).toBe(true)
  })

  it('emits submit payload when form is valid', async () => {
    const wrapper = mount(AgentPromptShell, {
      props: {
        title: 'Test agent',
        promptTemplates: [{ id: 'p1', title: 'Prompt', content: 'content' }],
        allowTemplateEditing: true,
      },
      global: globalConfig,
    })

    const promptField = wrapper.get('[data-test="agent-prompt"]')
    await promptField.setValue('Hello world')
    await wrapper.get('[data-test="agent-submit"]').trigger('click')

    const submission = wrapper.emitted('submit')?.[0]?.[0]
    expect(submission).toBeDefined()
    expect(submission.prompt).toBe('Hello world')
    expect(submission.promptVariantId).toBe('p1')
  })
})
