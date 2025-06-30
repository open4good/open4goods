import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import Card from './Card.vue'

describe('Card', () => {
  it('renders content', () => {
    const wrapper = mount(Card, { slots: { default: 'Hello' } })
    expect(wrapper.text()).toBe('Hello')
  })

  it('applies variant classes', () => {
    const wrapper = mount(Card, { props: { variant: 'primary' }, slots: { default: 'hi' } })
    expect(wrapper.classes()).toContain('border-blue-500')
  })
})
