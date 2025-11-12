<template>
  <div v-if="hasData" ref="chartContainerRef" class="impact-subscore-chart">
    <ClientOnly>
      <VueECharts
        v-if="chartOption"
        ref="chartRef"
        :option="chartOption"
        :autoresize="true"
        class="impact-subscore-chart__echart"
        @finished="handleChartFinished"
      />
      <template #fallback>
        <div class="impact-subscore-chart__placeholder" />
      </template>
    </ClientOnly>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import VueECharts from 'vue-echarts'
import type { EChartsOption } from 'echarts'
import type { EChartsType } from 'echarts/core'
import { useResizeObserver } from '@vueuse/core'
import type { DistributionBucket } from './impact-types'
import { ensureImpactECharts } from './echarts-setup'

const props = defineProps<{
  distribution: DistributionBucket[]
  label: string
  relativeValue: number | null
  productName: string
  productBrand: string
  productModel: string
  productImage: string
  productAbsoluteValue: number | null
}>()

ensureImpactECharts()

const FALLBACK_PRODUCT_IMAGE = '/nudger-icon-512x512.png'

const chartRef = ref<InstanceType<typeof VueECharts> | null>(null)
const chartContainerRef = ref<HTMLElement | null>(null)

const filteredDistribution = computed(() =>
  props.distribution.filter((bucket) => bucket.label.toUpperCase() !== 'ES-UNKNOWN'),
)

type NumericRange = {
  min: number
  max: number
}

const sanitizeNumericLabel = (label: string) =>
  label
    .replace(/\u202f/g, ' ')
    .replace(/(\d)[\s\u00a0]+(\d)/g, '$1$2')
    .replace(/,/g, '.')

const extractNumericRange = (label: string): NumericRange | null => {
  const sanitized = sanitizeNumericLabel(label)
  const matches = sanitized.match(/-?\d+(?:\.\d+)?/g)

  if (!matches?.length) {
    return null
  }

  const values = matches
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value))
    .sort((a, b) => a - b)

  if (!values.length) {
    return null
  }

  let min = values[0]
  let max = values[values.length - 1]

  const hasLessToken = /[<≤]/.test(label)
  const hasGreaterToken = /[>≥]/.test(label)
  const hasPlusToken = /\+\s*$/.test(label)

  if (values.length === 1) {
    if (hasLessToken && !hasGreaterToken) {
      min = Number.NEGATIVE_INFINITY
      max = values[0]
    } else if (hasGreaterToken || hasPlusToken) {
      min = values[0]
      max = Number.POSITIVE_INFINITY
    } else {
      min = values[0]
      max = values[0]
    }
  }

  return { min, max }
}

const calculateDistanceFromRange = (target: number, range: NumericRange) => {
  if (target < range.min) {
    return range.min === Number.NEGATIVE_INFINITY ? 0 : range.min - target
  }

  if (target > range.max) {
    return range.max === Number.POSITIVE_INFINITY ? 0 : target - range.max
  }

  return 0
}

const hasData = computed(() => filteredDistribution.value.length > 0)

const productImageSource = computed(() => {
  const source = props.productImage?.trim()
  return source?.length ? source : FALLBACK_PRODUCT_IMAGE
})

const productLabel = computed(() => {
  const brand = props.productBrand?.trim()
  const model = props.productModel?.trim()
  const segments = [brand, model].filter((segment) => segment?.length)
  if (segments.length) {
    return segments.join(' ')
  }

  return props.productName
})

const productAnchorValue = computed(() => {
  if (typeof props.productAbsoluteValue === 'number' && Number.isFinite(props.productAbsoluteValue)) {
    return props.productAbsoluteValue
  }

  if (typeof props.relativeValue === 'number' && Number.isFinite(props.relativeValue)) {
    return props.relativeValue
  }

  return null
})

const productBucket = computed(() => {
  if (!hasData.value) {
    return null
  }

  const targetValue = productAnchorValue.value
  if (targetValue == null) {
    return null
  }

  let closest: { bucket: DistributionBucket; distance: number } | null = null

  for (const bucket of filteredDistribution.value) {
    const range = extractNumericRange(bucket.label)
    if (!range) {
      continue
    }

    const distance = calculateDistanceFromRange(targetValue, range)
    if (!closest || distance < closest.distance) {
      closest = { bucket, distance }

      if (distance === 0) {
        break
      }
    }
  }

  return closest?.bucket ?? null
})

const chartOption = computed<EChartsOption | null>(() => {
  if (!hasData.value) {
    return null
  }

  return {
    grid: { top: 20, left: 40, right: 20, bottom: 40 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: filteredDistribution.value.map((bucket) => bucket.label),
      axisLabel: { rotate: 35 },
    },
    yAxis: { type: 'value', min: 0 },
    series: [
      {
        type: 'bar',
        name: props.label,
        data: filteredDistribution.value.map((bucket) => bucket.value),
        itemStyle: {
          color: 'rgba(33, 150, 243, 0.75)',
        },
      },
    ],
  }
})

const PRODUCT_GRAPHIC_ID = 'product-impact-marker'

const removeProductGraphic = (chart: EChartsType) => {
  chart.setOption({ graphic: [{ id: PRODUCT_GRAPHIC_ID, $action: 'remove' }] }, false)
}

const updateProductGraphic = () => {
  const chart = chartRef.value?.chart as EChartsType | undefined
  if (!chart) {
    return
  }

  if (!productBucket.value) {
    removeProductGraphic(chart)
    return
  }

  const coordinates = chart.convertToPixel({ xAxisIndex: 0, yAxisIndex: 0 }, [
    productBucket.value.label,
    0,
  ])

  if (!Array.isArray(coordinates)) {
    return
  }

  const [x, y] = coordinates
  if (!Number.isFinite(x) || !Number.isFinite(y)) {
    return
  }

  const arrowWidth = 24
  const arrowHeight = 36
  const imageSize = 56
  const gap = 12
  const labelOffset = arrowHeight + imageSize + gap * 2

  chart.setOption(
    {
      graphic: [
        {
          id: PRODUCT_GRAPHIC_ID,
          type: 'group',
          position: [x, y],
          z: 20,
          children: [
            {
              type: 'path',
              position: [-arrowWidth / 2, -arrowHeight],
              shape: {
                pathData: `M0 0 L${arrowWidth} 0 L${arrowWidth / 2} ${arrowHeight} Z`,
              },
              style: {
                fill: '#ef4444',
                shadowColor: 'rgba(239, 68, 68, 0.35)',
                shadowBlur: 12,
              },
              silent: true,
            },
            {
              type: 'image',
              position: [-imageSize / 2, -(arrowHeight + imageSize + gap)],
              style: {
                image: productImageSource.value,
                width: imageSize,
                height: imageSize,
              },
              silent: true,
            },
            {
              type: 'text',
              position: [0, -labelOffset],
              style: {
                text: productLabel.value,
                align: 'center',
                verticalAlign: 'middle',
                fontSize: 14,
                fontWeight: 700,
                fill: '#991b1b',
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                padding: [6, 14],
                borderRadius: 18,
                shadowBlur: 6,
                shadowColor: 'rgba(15, 23, 42, 0.08)',
              },
              silent: true,
            },
          ],
        },
      ],
    },
    false,
  )
}

const scheduleGraphicUpdate = () => {
  if (!import.meta.client) {
    return
  }

  nextTick(() => {
    requestAnimationFrame(() => {
      updateProductGraphic()
    })
  })
}

watch([chartOption, productBucket, productImageSource, productLabel], () => {
  scheduleGraphicUpdate()
})

let stopResizeObserver: (() => void) | null = null

onMounted(() => {
  if (chartContainerRef.value) {
    const { stop } = useResizeObserver(chartContainerRef, () => {
      scheduleGraphicUpdate()
    })
    stopResizeObserver = stop
  }
})

onBeforeUnmount(() => {
  stopResizeObserver?.()
  const chart = chartRef.value?.chart as EChartsType | undefined
  if (chart) {
    removeProductGraphic(chart)
  }
})

const handleChartFinished = () => {
  scheduleGraphicUpdate()
}
</script>

<style scoped>
.impact-subscore-chart {
  margin-top: auto;
}

.impact-subscore-chart__echart {
  height: 220px;
}

.impact-subscore-chart__placeholder {
  height: 220px;
  border-radius: 18px;
  background: rgba(148, 163, 184, 0.15);
}
</style>
