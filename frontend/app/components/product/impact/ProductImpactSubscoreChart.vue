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

const productBucket = computed(() => {
  if (props.productAbsoluteValue == null || !hasData.value) {
    return null
  }

  const targetValue = props.productAbsoluteValue

  let closest: { bucket: DistributionBucket; distance: number } | null = null

  for (const bucket of filteredDistribution.value) {
    const labelValue = Number(bucket.label)
    if (!Number.isFinite(labelValue)) {
      continue
    }

    const distance = Math.abs(labelValue - targetValue)
    if (!closest || distance < closest.distance) {
      closest = { bucket, distance }
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
    yAxis: { type: 'value' },
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
