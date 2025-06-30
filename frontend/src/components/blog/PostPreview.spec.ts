import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import PostPreview from './PostPreview.vue'

const post = { title: 'T', summary: 'S', author: 'A', category: ['tag'] }

describe('PostPreview', () => {
  it('renders title and summary', () => {
    const wrapper = mount(PostPreview, { props: { post } })
    expect(wrapper.text()).toContain('T')
    expect(wrapper.text()).toContain('S')
  })
})
