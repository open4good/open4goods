import type { ComponentOptions } from 'vue'
import deviceMixin from 'nuxt-device'

interface DeviceInfo {
  isMobile: boolean
  isTablet: boolean
  isDesktop: boolean
  isMobileOrTablet: boolean
  isDesktopOrTablet: boolean
  isIos: boolean
  isAndroid: boolean
}

const DEFAULT_DEVICE: DeviceInfo = {
  isMobile: false,
  isTablet: false,
  isDesktop: true,
  isMobileOrTablet: false,
  isDesktopOrTablet: true,
  isIos: false,
  isAndroid: false,
}

export function useDevice() {
  const nuxtApp = useNuxtApp()
  const display = useDisplay()
  const deviceState = useState<DeviceInfo>('nudger-device-state', () => ({
    ...DEFAULT_DEVICE,
  }))
  const mixinComputed =
    (deviceMixin as ComponentOptions | undefined)?.computed ?? {}

  const computeFromMixin = (width: number) => {
    const ctx: Record<string, unknown> = { windowWidth: width }
    const resolve = (
      key: keyof typeof mixinComputed,
      fallback: () => boolean
    ) => {
      const handler = mixinComputed[key]
      if (typeof handler === 'function') {
        try {
          return handler.call(ctx)
        } catch {
          return fallback()
        }
      }

      return fallback()
    }

    const isMobile = resolve('isMobile', () => width <= 720)
    ctx.isMobile = isMobile
    const isTablet = resolve('isTablet', () => width > 720 && width <= 1112)
    ctx.isTablet = isTablet
    const isDesktop = resolve('isDesktop', () => width > 1112)
    ctx.isDesktop = isDesktop

    return {
      isMobile,
      isTablet,
      isDesktop,
      isMobileOrTablet: resolve('isMobileOrTablet', () => isMobile || isTablet),
      isDesktopOrTablet: resolve(
        'isDesktopOrTablet',
        () => isDesktop || isTablet
      ),
    }
  }

  const syncFromWindow = () => {
    if (!import.meta.client) {
      return
    }

    const width = window.innerWidth
    const mixinValues = computeFromMixin(width)
    Object.assign(deviceState.value, mixinValues)
    const userAgent = navigator.userAgent
    deviceState.value.isIos = /iPad|iPhone|iPod/.test(userAgent)
    deviceState.value.isAndroid = /android/i.test(userAgent)
  }

  const syncFromModule = () => {
    const moduleDevice = nuxtApp.$device as Partial<DeviceInfo> | undefined
    if (moduleDevice) {
      Object.assign(deviceState.value, moduleDevice)
    }
  }

  if (import.meta.client) {
    syncFromModule()
    syncFromWindow()
    watch(
      () => display.width.value,
      () => {
        syncFromModule()
        syncFromWindow()
      },
      { immediate: false }
    )
  }

  return deviceState.value
}
