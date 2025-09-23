import { mount } from '@vue/test-utils'
import { expect, test, describe } from 'vitest'
import { defineComponent } from 'vue'

// Simple test component
const SimpleComponent = defineComponent({
  template:
    '<div><button @click="handleClick">Click me</button><p v-if="clicked">Clicked!</p></div>',
  data() {
    return {
      clicked: false,
    }
  },
  methods: {
    handleClick() {
      this.clicked = true
    },
  },
})

describe('Simple Component Test', () => {
  test('should render button and handle click', async () => {
    const wrapper = mount(SimpleComponent)

    // Check if button exists
    const button = wrapper.find('button')
    expect(button.exists()).toBe(true)
    expect(button.text()).toBe('Click me')

    // Initially, "Clicked!" should not be visible
    expect(wrapper.find('p').exists()).toBe(false)

    // Click button
    await button.trigger('click')

    // After click, "Clicked!" should be visible
    expect(wrapper.find('p').exists()).toBe(true)
    expect(wrapper.find('p').text()).toBe('Clicked!')
  })
})
