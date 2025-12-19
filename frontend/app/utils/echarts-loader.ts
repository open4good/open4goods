type EChartsModules = {
  core: typeof import('echarts/core')
  charts: typeof import('echarts/charts')
  components: typeof import('echarts/components')
  renderers: typeof import('echarts/renderers')
}

let registrationPromise: Promise<void> | null = null
let loadedModules: EChartsModules | null = null
const pendingRegistrations: Array<(modules: EChartsModules) => void> = []

export const ensureECharts = (register?: (modules: EChartsModules) => void) => {
  if (import.meta.server) {
    return Promise.resolve()
  }

  if (register) {
    if (loadedModules) {
      register(loadedModules)
    } else {
      pendingRegistrations.push(register)
    }
  }

  if (!registrationPromise) {
    registrationPromise = (async () => {
      const [core, charts, components, renderers] = await Promise.all([
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

      loadedModules = { core, charts, components, renderers }

      pendingRegistrations.splice(0).forEach(registration => {
        registration(loadedModules as EChartsModules)
      })
    })()
  }

  return registrationPromise
}
