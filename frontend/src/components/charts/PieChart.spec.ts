import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import PieChart from './PieChart.vue'

describe('PieChart', () => {
  it('renders canvas', () => {
    const wrapper = mount(PieChart, { props: { data: { labels: ['A'], values: [1] } } })
    expect(wrapper.find('canvas').exists()).toBe(true)
  })
})
