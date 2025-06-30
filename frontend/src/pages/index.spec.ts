import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import IndexPage from './index.vue'
describe('Index page', () => {
  it('renders welcome text', () => {
    const wrapper = mount(IndexPage)
    expect(wrapper.text()).toContain('Nudger')
  })
})
