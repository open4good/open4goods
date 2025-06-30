import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import PostContent from './PostContent.vue'

describe('PostContent', () => {
  it('renders HTML body', () => {
    const wrapper = mount(PostContent, { props: { body: '<p>Hello</p>' } })
    expect(wrapper.html()).toContain('<p>Hello</p>')
  })
})
