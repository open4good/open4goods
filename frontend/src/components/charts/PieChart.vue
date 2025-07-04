<template>
  <canvas ref="canvas" />
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import Chart from 'chart.js/auto'

export interface ChartData { labels: string[]; values: number[] }

const props = defineProps<{ data: ChartData }>()

const canvas = ref<HTMLCanvasElement | null>(null)
let chart: Chart | undefined

const render = () => {
  if (!canvas.value) return
  if (chart) chart.destroy()
  chart = new Chart(canvas.value, {
    type: 'pie',
    data: { labels: props.data.labels, datasets: [{ data: props.data.values }] }
  })
}

onMounted(render)
watch(() => props.data, render, { deep: true })
</script>
