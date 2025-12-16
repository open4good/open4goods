import { defineNuxtPlugin } from '#app'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import {
  AriaComponent,
  DatasetComponent,
  GridComponent,
  TitleComponent,
  TooltipComponent,
} from 'echarts/components'

export default defineNuxtPlugin(() => {
  use([
    CanvasRenderer,
    BarChart,
    GridComponent,
    TooltipComponent,
    DatasetComponent,
    AriaComponent,
    TitleComponent,
  ])
})
