import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import B2bPlaygroundShell from '~/components/B2bPlaygroundShell.vue'

describe('B2bPlaygroundShell', () => {
  it('mounts without errors', () => {
    const wrapper = mount(B2bPlaygroundShell)
    expect(wrapper.exists()).toBe(true)
  })

  it('renders request and response slot content', () => {
    const wrapper = mount(B2bPlaygroundShell, {
      slots: {
        request: '<div class="test-request">request</div>',
        response: '<div class="test-response">response</div>',
      }
    })
    expect(wrapper.html()).toContain('test-request')
    expect(wrapper.html()).toContain('test-response')
  })

  it('renders examples slot when provided', () => {
    const wrapper = mount(B2bPlaygroundShell, {
      slots: {
        request: '<div>r</div>',
        response: '<div>r</div>',
        examples: '<div class="test-examples">examples</div>',
      }
    })
    expect(wrapper.html()).toContain('test-examples')
  })

  it('does not render examples section when slot is absent', () => {
    const wrapper = mount(B2bPlaygroundShell, {
      slots: {
        request: '<div>r</div>',
        response: '<div>r</div>',
      }
    })
    expect(wrapper.html()).not.toContain('test-examples')
  })
})
