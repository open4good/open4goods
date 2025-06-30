import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import Author from './Author.vue'

describe('Author', () => {
  it('renders name', () => {
    const wrapper = mount(Author, { props: { name: 'Alice' } })
    expect(wrapper.text()).toContain('Alice')
  })

  it('computes avatar url', () => {
    const wrapper = mount(Author, { props: { name: 'Alice Smith' } })
    expect(wrapper.find('img').attributes('src')).toContain('alice-smith')
  })
})
