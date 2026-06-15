<template>
  <div
    class="hero-network-visual"
    :class="[`hero-network-visual--${densityProfile}`, { 'hero-network-visual--reduced-motion': reducedMotionActive }]"
    :data-theme="resolvedTheme"
    :style="rootStyle"
    aria-hidden="true"
  >
    <div ref="stageRef" class="hero-network-visual__stage">
      <div :id="particlesId" ref="particlesRef" class="hero-network-visual__particles" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { CSSProperties } from 'vue'

export type NetworkTheme = 'auto' | 'inherit' | 'dark' | 'light'
export type DeepPartial<T> = { [K in keyof T]?: T[K] extends Array<infer U> ? Array<U> : T[K] extends object ? DeepPartial<T[K]> : T[K] }
export type NetworkDensity = 'auto' | 'low' | 'medium' | 'high' | 'ultra'
export type NetworkLayout = 'balanced-random' | 'circle' | 'ellipse' | 'grid-random' | 'clustered' | 'constellation'
export type AnchorX = 'left' | 'center' | 'right' | number
export type AnchorY = 'top' | 'center' | 'bottom' | number
export type Fit = 'contain' | 'cover' | 'fill'

export interface HeroNetworkConfig {
  seed?: string | number | null
  theme: NetworkTheme
  density: NetworkDensity
  paused: boolean
  fps: number
  routers: {
    count: number
    layout: NetworkLayout
    size: [number, number]
    linkChance: number
    communicationIntensity: number
    minDistance: number
  }
  nodes: {
    total: number
    perRouter: 'auto' | number | number[]
    size: [number, number]
    orbit: boolean
    drift: boolean
    pulseChance: number
    switchRouterChance: number
    lifecycle: boolean
    minLifetime: number
    maxLifetime: number
    spawnJitter: number
    maxVisibleOnSmall: number
  }
  communication: {
    nodeToRouter: boolean
    routerToRouter: boolean
    nodeToNode: boolean
    packets: boolean
    beams: boolean
    arcs: boolean
    dottedPaths: boolean
    randomize: boolean
    subtlety: number
    packetCount: number
    packetSpeed: number
    beamChance: number
    linkSampleSize: number
  }
  rings: {
    enabled: boolean
    count: number
    opacity: number
    speed: number
    thickness: number
    scale: number
  }
  layout: {
    mode: NetworkLayout
    anchorX: AnchorX
    anchorY: AnchorY
    offsetX: number
    offsetY: number
    spread: number
    fit: Fit
    avoidOverlap: boolean
  }
  responsive: {
    mode: 'auto' | 'off'
    reduceRouters: boolean
    reduceNodes: boolean
    hidePacketsBelow: number
    hideRingsBelow: number
    hideNodeLinksBelow: number
    compactWidgetModeBelow: number
    minNodeCount: number
  }
  visual: {
    background: string
    grid: boolean
    routerGlyphs: boolean
    glow: number
    blur: number
    lineOpacity: number
    colorRouter: string
    colorRouterDeep: string
    colorNode: string
    colorNodeDeep: string
    colorPacket: string
    colorAccent: string
    colorGrid: string
  }
}

interface ParticlesInstance {
  pJS?: {
    canvas?: {
      el?: HTMLCanvasElement
    }
    fn?: {
      drawAnimFrame?: number
    }
  }
}

interface ParticlesWindow extends Window {
  particlesJS?: (tagId: string, params: Record<string, unknown>) => void
  pJSDom?: ParticlesInstance[] | null
}

const DEFAULT_CONFIG: HeroNetworkConfig = {
  seed: null,
  theme: 'auto',
  density: 'auto',
  paused: false,
  fps: 40,
  routers: {
    count: 4,
    layout: 'ellipse',
    size: [15, 26],
    linkChance: 0.45,
    communicationIntensity: 0.52,
    minDistance: 142,
  },
  nodes: {
    total: 26,
    perRouter: 'auto',
    size: [2.7, 5.8],
    orbit: true,
    drift: true,
    pulseChance: 0.14,
    switchRouterChance: 0.06,
    lifecycle: true,
    minLifetime: 32000,
    maxLifetime: 56000,
    spawnJitter: 0.98,
    maxVisibleOnSmall: 22,
  },
  communication: {
    nodeToRouter: true,
    routerToRouter: true,
    nodeToNode: false,
    packets: true,
    beams: true,
    arcs: true,
    dottedPaths: true,
    randomize: true,
    subtlety: 0.86,
    packetCount: 8,
    packetSpeed: 0.58,
    beamChance: 0.06,
    linkSampleSize: 24,
  },
  rings: {
    enabled: true,
    count: 3,
    opacity: 0.055,
    speed: 0.045,
    thickness: 1,
    scale: 1,
  },
  layout: {
    mode: 'balanced-random',
    anchorX: 'center',
    anchorY: 'center',
    offsetX: 0,
    offsetY: 0,
    spread: 0.82,
    fit: 'contain',
    avoidOverlap: true,
  },
  responsive: {
    mode: 'auto',
    reduceRouters: true,
    reduceNodes: true,
    hidePacketsBelow: 280,
    hideRingsBelow: 340,
    hideNodeLinksBelow: 300,
    compactWidgetModeBelow: 420,
    minNodeCount: 12,
  },
  visual: {
    background: 'transparent',
    grid: true,
    routerGlyphs: true,
    glow: 0.82,
    blur: 0.28,
    lineOpacity: 0.16,
    colorRouter: '#00f5ff',
    colorRouterDeep: '#053b73',
    colorNode: '#b15cff',
    colorNodeDeep: '#35136e',
    colorPacket: '#00ffc6',
    colorAccent: '#ff3df2',
    colorGrid: 'rgba(125, 211, 252, 0.12)',
  },
}

const props = withDefaults(defineProps<{
  config?: DeepPartial<HeroNetworkConfig>
  routers?: number
  nodes?: number
  theme?: NetworkTheme
  density?: NetworkDensity
  layout?: NetworkLayout
  anchorX?: AnchorX
  anchorY?: AnchorY
  seed?: string | number | null
  showRings?: boolean
  showRouterLinks?: boolean
  showNodeLinks?: boolean
  animatedPackets?: boolean
  paused?: boolean
}>(), {
  config: () => ({}),
  routers: undefined,
  nodes: undefined,
  theme: undefined,
  density: undefined,
  layout: undefined,
  anchorX: undefined,
  anchorY: undefined,
  seed: undefined,
  showRings: undefined,
  showRouterLinks: undefined,
  showNodeLinks: undefined,
  animatedPackets: undefined,
  paused: undefined,
})

const particlesId = `hero-particles-${Math.random().toString(36).slice(2)}`
const stageRef = ref<HTMLElement | null>(null)
const particlesRef = ref<HTMLElement | null>(null)
const prefersDark = ref(false)
const prefersReducedMotion = ref(false)
let mediaDarkQuery: MediaQueryList | null = null
let mediaReducedQuery: MediaQueryList | null = null
let removeMediaListeners: (() => void) | null = null
let initialized = false

const config = computed<HeroNetworkConfig>(() => {
  const merged = deepMerge(DEFAULT_CONFIG, props.config ?? {}) as HeroNetworkConfig

  if (typeof props.routers === 'number') merged.routers.count = Math.max(1, props.routers)
  if (typeof props.nodes === 'number') merged.nodes.total = Math.max(0, props.nodes)
  if (props.theme) merged.theme = props.theme
  if (props.density) merged.density = props.density
  if (props.layout) {
    merged.layout.mode = props.layout
    merged.routers.layout = props.layout
  }
  if (props.anchorX !== undefined) merged.layout.anchorX = props.anchorX
  if (props.anchorY !== undefined) merged.layout.anchorY = props.anchorY
  if (props.seed !== undefined) merged.seed = props.seed
  if (props.showRings !== undefined) merged.rings.enabled = props.showRings
  if (props.showRouterLinks !== undefined) merged.communication.routerToRouter = props.showRouterLinks
  if (props.showNodeLinks !== undefined) merged.communication.nodeToRouter = props.showNodeLinks
  if (props.animatedPackets !== undefined) merged.communication.packets = props.animatedPackets
  if (props.paused !== undefined) merged.paused = props.paused

  merged.communication.subtlety = clamp(merged.communication.subtlety, 0, 1)
  merged.visual.glow = clamp(merged.visual.glow, 0, 2)
  merged.layout.spread = clamp(merged.layout.spread, 0.18, 1.35)
  merged.nodes.total = Math.max(0, Math.min(96, Math.round(merged.nodes.total)))
  merged.communication.packetSpeed = Math.max(0, merged.communication.packetSpeed)

  return merged
})

const reducedMotionActive = computed(() => prefersReducedMotion.value || config.value.paused)
const resolvedTheme = computed(() => {
  if (config.value.theme === 'inherit') return 'inherit'
  if (config.value.theme === 'auto') return prefersDark.value ? 'dark' : 'light'
  return config.value.theme
})

const densityProfile = computed(() => {
  if (config.value.density !== 'auto') return config.value.density
  if (config.value.nodes.total <= 18) return 'low'
  if (config.value.nodes.total >= 42) return 'high'
  return 'medium'
})

const particleCount = computed(() => {
  const routerWeight = Math.max(0, config.value.routers.count) * 5
  const packetWeight = config.value.communication.packets ? Math.round(config.value.communication.packetCount * 0.7) : 0
  const base = config.value.nodes.total + routerWeight + packetWeight

  if (densityProfile.value === 'low') return clamp(Math.round(base * 0.7), 18, 42)
  if (densityProfile.value === 'high') return clamp(Math.round(base * 1.08), 42, 82)
  if (densityProfile.value === 'ultra') return clamp(Math.round(base * 1.25), 58, 104)
  return clamp(base, 28, 68)
})

const rootStyle = computed<CSSProperties>(() => ({
  '--hero-network-bg': config.value.visual.background,
  '--hero-network-router': config.value.visual.colorRouter,
  '--hero-network-node': config.value.visual.colorNode,
  '--hero-network-packet': config.value.visual.colorPacket,
  '--hero-network-accent': config.value.visual.colorAccent,
  '--hero-network-grid': config.value.visual.colorGrid,
  '--hero-network-glow': String(config.value.visual.glow),
  '--hero-network-blur': `${config.value.visual.blur}px`,
  '--hero-network-line-opacity': String(config.value.visual.lineOpacity),
}))

const particlesOptions = computed<Record<string, unknown>>(() => {
  const cfg = config.value
  const moving = !reducedMotionActive.value
  const lineOpacity = clamp(cfg.visual.lineOpacity + (cfg.communication.routerToRouter ? 0.08 : 0), 0.05, 0.34)
  const particleOpacity = resolvedTheme.value === 'light' ? 0.42 : 0.58
  const particleSize = cfg.routers.size[0] > 10 ? 3.2 : 2.6

  return {
    particles: {
      number: {
        value: particleCount.value,
        density: {
          enable: true,
          value_area: 860,
        },
      },
      color: {
        value: [cfg.visual.colorRouter, cfg.visual.colorNode, cfg.visual.colorPacket],
      },
      shape: {
        type: 'circle',
        stroke: {
          width: 0,
          color: '#000000',
        },
        polygon: {
          nb_sides: 5,
        },
      },
      opacity: {
        value: particleOpacity,
        random: true,
        anim: {
          enable: moving && cfg.communication.beams,
          speed: 0.8,
          opacity_min: 0.16,
          sync: false,
        },
      },
      size: {
        value: particleSize,
        random: true,
        anim: {
          enable: moving && cfg.rings.enabled,
          speed: 2.2,
          size_min: 0.3,
          sync: false,
        },
      },
      line_linked: {
        enable: cfg.communication.nodeToRouter || cfg.communication.routerToRouter,
        distance: cfg.routers.minDistance,
        color: cfg.visual.colorRouter,
        opacity: lineOpacity,
        width: cfg.rings.thickness,
      },
      move: {
        enable: moving,
        speed: moving ? clamp(cfg.communication.packetSpeed * 2.2, 0.55, 2.2) : 0,
        direction: 'none',
        random: true,
        straight: false,
        out_mode: 'out',
        bounce: false,
        attract: {
          enable: true,
          rotateX: 900,
          rotateY: 1600,
        },
      },
    },
    interactivity: {
      detect_on: 'canvas',
      events: {
        onhover: {
          enable: moving,
          mode: 'grab',
        },
        onclick: {
          enable: false,
          mode: 'push',
        },
        resize: true,
      },
      modes: {
        grab: {
          distance: 180,
          line_linked: {
            opacity: clamp(lineOpacity + 0.16, 0.18, 0.52),
          },
        },
        bubble: {
          distance: 220,
          size: 8,
          duration: 2,
          opacity: 0.7,
          speed: 3,
        },
        repulse: {
          distance: 140,
          duration: 0.4,
        },
        push: {
          particles_nb: 4,
        },
        remove: {
          particles_nb: 2,
        },
      },
    },
    retina_detect: true,
  }
})

async function startParticles() {
  if (import.meta.server || !particlesRef.value) return

  await import('particles.js')
  await nextTick()
  destroyParticles()

  const particlesWindow = window as ParticlesWindow
  particlesWindow.pJSDom ??= []
  particlesWindow.particlesJS?.(particlesId, cloneOptions(particlesOptions.value))
  initialized = true
}

function destroyParticles() {
  if (import.meta.server) return

  const particlesWindow = window as ParticlesWindow
  const instances = particlesWindow.pJSDom
  if (Array.isArray(instances)) {
    for (const instance of instances) {
      const canvas = instance.pJS?.canvas?.el
      if (canvas?.parentElement === particlesRef.value) {
        const frame = instance.pJS?.fn?.drawAnimFrame
        if (typeof frame === 'number') {
          cancelAnimationFrame(frame)
        }
        canvas.remove()
      }
    }
    particlesWindow.pJSDom = instances.filter((instance) => instance.pJS?.canvas?.el?.parentElement !== particlesRef.value)
  }

  particlesRef.value?.querySelectorAll('canvas').forEach((canvas) => canvas.remove())
  initialized = false
}

function setupMediaQueries() {
  if (import.meta.server) return

  mediaDarkQuery = window.matchMedia('(prefers-color-scheme: dark)')
  mediaReducedQuery = window.matchMedia('(prefers-reduced-motion: reduce)')

  const syncMedia = () => {
    prefersDark.value = Boolean(mediaDarkQuery?.matches)
    prefersReducedMotion.value = Boolean(mediaReducedQuery?.matches)
  }

  syncMedia()
  mediaDarkQuery.addEventListener('change', syncMedia)
  mediaReducedQuery.addEventListener('change', syncMedia)
  removeMediaListeners = () => {
    mediaDarkQuery?.removeEventListener('change', syncMedia)
    mediaReducedQuery?.removeEventListener('change', syncMedia)
  }
}

function deepMerge<T extends object>(base: T, patch: DeepPartial<T>): T {
  const output: Record<string, unknown> = { ...(base as Record<string, unknown>) }

  for (const [key, value] of Object.entries(patch as Record<string, unknown>)) {
    if (value === undefined) continue
    const current = output[key]
    if (isPlainObject(current) && isPlainObject(value)) {
      output[key] = deepMerge(current, value)
    } else if (Array.isArray(value)) {
      output[key] = [...value]
    } else {
      output[key] = value
    }
  }

  return output as T
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}

function cloneOptions(value: Record<string, unknown>) {
  return JSON.parse(JSON.stringify(value)) as Record<string, unknown>
}

onMounted(() => {
  setupMediaQueries()
  void startParticles()
})

watch(
  () => JSON.stringify(particlesOptions.value),
  () => {
    if (initialized) {
      void startParticles()
    }
  }
)

onBeforeUnmount(() => {
  destroyParticles()
  removeMediaListeners?.()
})
</script>

<style scoped lang="scss">
.hero-network-visual {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 320px;
  overflow: hidden;
  pointer-events: none;
  background: var(--hero-network-bg);
}

.hero-network-visual::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 78% 50%, color-mix(in oklab, var(--hero-network-router) calc(var(--hero-network-glow) * 18%), transparent) 0%, transparent 46%),
    radial-gradient(ellipse at 92% 58%, color-mix(in oklab, var(--hero-network-accent) calc(var(--hero-network-glow) * 14%), transparent) 0%, transparent 38%);
  filter: blur(var(--hero-network-blur));
  opacity: 0.82;
}

.hero-network-visual__stage {
  position: absolute;
  top: 50%;
  right: clamp(16px, 5vw, 96px);
  width: min(62vw, 920px);
  height: min(72svh, 720px);
  min-height: 420px;
  transform: translateY(-50%);
  -webkit-mask-image: linear-gradient(90deg, transparent 0%, #000 18%, #000 100%);
  mask-image: linear-gradient(90deg, transparent 0%, #000 18%, #000 100%);
}

.hero-network-visual__particles {
  position: absolute;
  inset: 0;
}

.hero-network-visual__particles :deep(.particles-js-canvas-el) {
  display: block;
  width: 100% !important;
  height: 100% !important;
}

.hero-network-visual--low .hero-network-visual__stage {
  width: min(54vw, 760px);
}

.hero-network-visual--high .hero-network-visual__stage,
.hero-network-visual--ultra .hero-network-visual__stage {
  width: min(68vw, 1040px);
}

.hero-network-visual[data-theme="light"] {
  opacity: 0.92;
}

.hero-network-visual--reduced-motion .hero-network-visual__particles {
  opacity: 0.76;
}

@media (max-width: 960px) {
  .hero-network-visual {
    min-height: 420px;
  }

  .hero-network-visual__stage {
    right: 50%;
    width: min(100vw, 680px);
    height: 520px;
    min-height: 360px;
    transform: translate(50%, -50%);
    -webkit-mask-image: linear-gradient(180deg, transparent 0%, #000 14%, #000 78%, transparent 100%);
    mask-image: linear-gradient(180deg, transparent 0%, #000 14%, #000 78%, transparent 100%);
  }
}
</style>
