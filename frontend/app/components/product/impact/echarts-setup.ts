import { use } from 'echarts/core'
import { BarChart, RadarChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { GridComponent, TooltipComponent, LegendComponent, PolarComponent } from 'echarts/components'

let registered = false

export const ensureImpactECharts = () => {
  if (registered) {
    return
  }

  use([BarChart, RadarChart, GridComponent, TooltipComponent, LegendComponent, PolarComponent, CanvasRenderer])
  registered = true
}
