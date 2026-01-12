import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import ProductImpactRadarChart from './ProductImpactRadarChart.vue'

interface RadarChartInstance {
  option: {
    radar: {
      indicator: Array<{ name: string; max: number }>
    }
  } | null
}

// Mock echarts since we don't need to render the actual canvas for logic testing
vi.mock('vue-echarts', () => ({
  default: {
    name: 'VueECharts',
    render: () => null,
    props: ['option'],
  },
}))

// Mock useElementSize to allow rendering
vi.mock('@vueuse/core', () => ({
  useElementSize: () => ({ width: { value: 500 }, height: { value: 400 } }),
}))

describe('ProductImpactRadarChart', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          impact: {
            radarAria: 'Radar chart for {product}',
          },
        },
      },
    },
  })

  const axes = [
    { id: 'energy', name: 'Energy', attributeValue: 'kWh' }, // Large values
    { id: 'score', name: 'Score', attributeValue: '/10' }, // Small values
  ]

  const series = [
    {
      label: 'Product A',
      values: [100, 8] as Array<number | null>,
      lineColor: 'red',
      areaColor: 'red',
      symbolColor: 'red',
    },
    {
      label: 'Product B',
      values: [50, 4] as Array<number | null>,
      lineColor: 'blue',
      areaColor: 'blue',
      symbolColor: 'blue',
    },
  ]

  it('calculates max value independently for each axis', () => {
    const wrapper = mount(ProductImpactRadarChart, {
      props: {
        axes,
        series,
        productName: 'Test Product',
      },
      global: {
        plugins: [i18n],
      },
    })

    // Access the component instance to get the computed option
    const vm = wrapper.vm as unknown as RadarChartInstance
    const option = vm.option

    expect(option).not.toBeNull()
    if (!option) return

    const indicators = option.radar.indicator
    expect(indicators).toHaveLength(2)

    // Check Energy axis (max should be based on 100)
    // 100 * 1.1 = 110
    expect(indicators[0].name).toBe('Energy')
    expect(indicators[0].max).toBeCloseTo(110)

    // Check Score axis (max should be based on 8)
    // 8 * 1.1 = 8.8
    expect(indicators[1].name).toBe('Score')
    expect(indicators[1].max).toBeCloseTo(8.8)
  })

  it('handles null values correctly', () => {
    const seriesWithNull = [
      {
        label: 'Product C',
        values: [null, 5] as Array<number | null>,
        lineColor: 'green',
        areaColor: 'green',
        symbolColor: 'green',
      },
    ]

    const wrapper = mount(ProductImpactRadarChart, {
      props: {
        axes,
        series: seriesWithNull,
        productName: 'Test Product',
      },
      global: {
        plugins: [i18n],
      },
    })

    const vm = wrapper.vm as unknown as RadarChartInstance
    const option = vm.option

    expect(option).not.toBeNull()
    if (!option) return

    const indicators = option.radar.indicator
    // First axis has only null, default max is 5
    // Actually, logic is: valuesForAxis -> empty -> maxObserved = 5 -> paddedMax = 5.5
    expect(indicators[0].max).toBe(5.5)

    // Second axis has 5 -> 5.5
    expect(indicators[1].max).toBe(5.5)
  })
})
