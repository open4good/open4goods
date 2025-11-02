import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import type { BarSeriesOption, EChartsOption, MarkLineComponentOption } from 'echarts'
import ProductImpactSubscoreChart from './ProductImpactSubscoreChart.vue'

vi.mock('vue-echarts', () => ({
  default: defineComponent({
    name: 'VueEChartsStub',
    props: {
      option: { type: Object, required: false },
    },
    setup(props) {
      return () => h('div', { class: 'echart-stub', 'data-option': JSON.stringify(props.option ?? {}) })
    },
  }),
}))

describe('ProductImpactSubscoreChart', () => {
  const clientOnlyStub = defineComponent({
    name: 'ClientOnlyStub',
    setup(_, { slots }) {
      return () => slots.default?.()
    },
  })

  it('does not render the chart when no distribution is provided', () => {
    const wrapper = mount(ProductImpactSubscoreChart, {
      props: {
        distribution: [],
        label: 'CO2 emissions',
        relativeValue: null,
        productName: 'Demo product',
      },
      global: {
        stubs: {
          ClientOnly: clientOnlyStub,
        },
      },
    })

    expect(wrapper.find('.impact-subscore-chart__echart').exists()).toBe(false)
  })

  it('renders an indicator aligned with the closest bucket and colors it based on the percent value', async () => {
    const wrapper = mount(ProductImpactSubscoreChart, {
      props: {
        distribution: [
          { label: '1', value: 5 },
          { label: '2', value: 10 },
          { label: '3', value: 2 },
        ],
        label: 'CO2 emissions',
        relativeValue: 1.3,
        productName: 'Demo product',
        percent: 50,
      },
      global: {
        stubs: {
          ClientOnly: clientOnlyStub,
        },
      },
    })

    await nextTick()

    const echartStub = wrapper.findComponent({ name: 'VueEChartsStub' })
    expect(echartStub.exists()).toBe(true)

    const option = echartStub.props('option') as EChartsOption
    const grid = Array.isArray(option.grid) ? option.grid[0] : option.grid
    expect(grid?.top).toBe(52)

    const yAxis = Array.isArray(option.yAxis) ? option.yAxis[0] : option.yAxis
    expect(yAxis?.max).toBeCloseTo(11.2)

    const seriesOption = (Array.isArray(option.series) ? option.series[0] : option.series) as BarSeriesOption
    const markLine = seriesOption.markLine as MarkLineComponentOption
    expect(markLine?.symbol).toEqual(['none', 'path://M12 24L24 8H16V0H8V8H0L12 24Z'])

    const markLineData = Array.isArray(markLine?.data) ? markLine?.data[0] : undefined
    expect(markLine?.lineStyle?.color).toBe('hsl(60.00, 75%, 45%)')
    expect(markLineData && 'xAxis' in markLineData ? markLineData.xAxis : undefined).toBe('1')
    expect(markLineData && 'yAxis' in markLineData ? markLineData.yAxis : undefined).toBeCloseTo(11.2)
  })

  it('falls back to a default accent color when percent is not provided', async () => {
    const wrapper = mount(ProductImpactSubscoreChart, {
      props: {
        distribution: [
          { label: '10', value: 4 },
          { label: '20', value: 6 },
        ],
        label: 'CO2 emissions',
        relativeValue: 18,
        productName: 'Demo product',
        percent: null,
      },
      global: {
        stubs: {
          ClientOnly: clientOnlyStub,
        },
      },
    })

    await nextTick()

    const option = wrapper.findComponent({ name: 'VueEChartsStub' }).props('option') as EChartsOption
    const seriesOption = (Array.isArray(option.series) ? option.series[0] : option.series) as BarSeriesOption
    const markLine = seriesOption.markLine as MarkLineComponentOption
    expect(markLine?.lineStyle?.color).toBe('#2563eb')
  })
})
