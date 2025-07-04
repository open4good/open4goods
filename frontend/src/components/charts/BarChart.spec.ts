import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import BarChart from './BarChart.vue'

describe('BarChart', () => {
  it('renders canvas', () => {
    const wrapper = mount(BarChart, { props: { data: { labels: ['A'], values: [1] } } })
    expect(wrapper.find('canvas').exists()).toBe(true)
  })
})
