let registrationPromise: Promise<void> | null = null

export const ensureECharts = () => {
  if (import.meta.server) {
    return Promise.resolve()
  }

  if (!registrationPromise) {
    registrationPromise = (async () => {
      const [core, charts, components, renderer] = await Promise.all([
        import(
          /* webpackChunkName: "echarts-chunk" */ 'echarts/core'
        ),
        import(
          /* webpackChunkName: "echarts-chunk" */ 'echarts/charts'
        ),
        import(
          /* webpackChunkName: "echarts-chunk" */ 'echarts/components'
        ),
        import(
          /* webpackChunkName: "echarts-chunk" */ 'echarts/renderers'
        ),
      ])

      const { use } = core
      const { LineChart, BarChart, RadarChart } = charts
      const {
        GridComponent,
        TooltipComponent,
        DataZoomComponent,
        TitleComponent,
        LegendComponent,
        PolarComponent,
      } = components
      const { CanvasRenderer } = renderer

      use([
        LineChart,
        BarChart,
        RadarChart,
        GridComponent,
        TooltipComponent,
        DataZoomComponent,
        TitleComponent,
        LegendComponent,
        PolarComponent,
        CanvasRenderer,
      ])
    })()
  }

  return registrationPromise
}
