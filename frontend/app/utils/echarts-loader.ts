type ModuleLoader<T> = () => Promise<T>

const moduleLoaders = {
    BarChart: async () =>
        (await import(
            /* webpackChunkName: "vendor-echarts" */ 'echarts/charts'
        )).BarChart,
    LineChart: async () =>
        (await import(
            /* webpackChunkName: "vendor-echarts" */ 'echarts/charts'
        )).LineChart,
    RadarChart: async () =>
        (await import(
            /* webpackChunkName: "vendor-echarts" */ 'echarts/charts'
        )).RadarChart,
    GridComponent: async () =>
        (await import(
            /* webpackChunkName: "vendor-echarts" */ 'echarts/components'
        )).GridComponent,
    LegendComponent: async () =>
        (await import(
            /* webpackChunkName: "vendor-echarts" */ 'echarts/components'
        )).LegendComponent,
    TooltipComponent: async () =>
        (await import(
            /* webpackChunkName: "vendor-echarts" */ 'echarts/components'
        )).TooltipComponent,
    DataZoomComponent: async () =>
        (await import(
            /* webpackChunkName: "vendor-echarts" */ 'echarts/components'
        )).DataZoomComponent,
    MarkAreaComponent: async () =>
        (await import(
            /* webpackChunkName: "vendor-echarts" */ 'echarts/components'
        )).MarkAreaComponent,
    RadarComponent: async () =>
        (await import(
            /* webpackChunkName: "vendor-echarts" */ 'echarts/components'
        )).RadarComponent,
    CanvasRenderer: async () =>
        (await import(
            /* webpackChunkName: "vendor-echarts" */ 'echarts/renderers'
        )).CanvasRenderer,
} satisfies Record<string, ModuleLoader<unknown>>

type EChartsModuleKey = keyof typeof moduleLoaders
type LoadedModuleMap = {
    [K in EChartsModuleKey]: Awaited<ReturnType<(typeof moduleLoaders)[K]>>
}

let corePromise: Promise<typeof import('echarts/core')> | null = null
const modulePromises: Partial<
    Record<EChartsModuleKey, Promise<LoadedModuleMap[EChartsModuleKey]>>
> = {}
const loadedModules: Partial<LoadedModuleMap> = {}

const loadCore = () => {
    if (!corePromise) {
        corePromise = import(
            /* webpackChunkName: "vendor-echarts" */ 'echarts/core'
        )
    }

    return corePromise
}

const loadModule = <K extends EChartsModuleKey>(module: K) => {
    if (loadedModules[module]) {
        return Promise.resolve(loadedModules[module] as LoadedModuleMap[K])
    }

    if (!modulePromises[module]) {
        modulePromises[module] = moduleLoaders[module]().then(loadedModule => {
            loadedModules[module] = loadedModule as LoadedModuleMap[K]
            return loadedModule as LoadedModuleMap[K]
        })
    }

    return modulePromises[module] as Promise<LoadedModuleMap[K]>
}

export const ensureECharts = async (
    requiredModules: EChartsModuleKey[]
): Promise<
    | {
          core: typeof import('echarts/core')
          modules: Array<LoadedModuleMap[EChartsModuleKey]>
      }
    | null
> => {
    if (import.meta.server) {
        return null
    }

    const uniqueModules = Array.from(new Set(requiredModules))
    const [core, modules] = await Promise.all([
        loadCore(),
        Promise.all(uniqueModules.map(module => loadModule(module))),
    ])

    return { core, modules }
}
