<template>
  <div class="inf-network-globe" :class="`inf-network-globe--${visualMode}`" :style="{ minHeight: mapMinHeight }">
    <div class="inf-network-globe__toolbar">
      <v-btn-toggle v-model="visualMode" density="compact" color="primary" variant="tonal" divided mandatory>
        <v-btn value="night" size="x-small" icon="mdi-weather-night" :aria-label="t('nodes.map.mode_night')" />
        <v-btn value="light" size="x-small" icon="mdi-white-balance-sunny" :aria-label="t('nodes.map.mode_light')" />
        <v-btn value="terrain" size="x-small" icon="mdi-image-filter-hdr" :aria-label="t('nodes.map.mode_terrain')" />
      </v-btn-toggle>
    </div>
    <svg viewBox="0 0 1000 560" preserveAspectRatio="xMidYMid slice" class="inf-network-globe__svg">
      <defs>
        <radialGradient id="globe-fill" cx="50%" cy="45%" r="60%">
          <stop offset="0%" stop-color="var(--globe-core)" stop-opacity="0.98" />
          <stop offset="68%" stop-color="var(--globe-mid)" stop-opacity="1" />
          <stop offset="100%" stop-color="var(--globe-edge)" stop-opacity="1" />
        </radialGradient>
        <radialGradient id="globe-light" cx="36%" cy="28%" r="70%">
          <stop offset="0%" stop-color="rgba(255,255,255,.32)" />
          <stop offset="54%" stop-color="rgba(255,255,255,.06)" />
          <stop offset="100%" stop-color="rgba(255,255,255,0)" />
        </radialGradient>
        <clipPath id="globe-clip">
          <circle cx="500" cy="280" r="220" />
        </clipPath>
      </defs>
      <circle cx="500" cy="280" r="220" fill="url(#globe-fill)" class="inf-network-globe__sphere" />
      <g clip-path="url(#globe-clip)">
        <g class="inf-network-globe__continents" :transform="continentTransform">
          <path
            v-for="continent in continentPaths"
            :key="continent.id"
            :d="continent.d"
          />
        </g>
      </g>
      <circle cx="500" cy="280" r="220" fill="url(#globe-light)" class="inf-network-globe__light" />
      <g class="inf-network-globe__grid">
        <ellipse v-for="lat in [-60,-30,30,60]" :key="`lat-${lat}`" cx="500" cy="280" rx="220" :ry="latRadius(lat)" />
        <ellipse v-for="lon in [0,30,60,90,120,150]" :key="`lon-${lon}`" cx="500" cy="280" :rx="Math.max(10, 220 - lon)" ry="220" />
      </g>
      <path
        v-for="link in linkArcs"
        :key="link.id"
        :d="link.d"
        class="inf-network-globe__arc"
        :style="{ '--delay': `${link.delay}ms`, '--dur': `${link.duration}ms` }"
      />
      <g
        v-for="point in projectedPoints"
        :key="point.id"
        class="inf-network-globe__node-wrap"
        :class="{ 'inf-network-globe__node-wrap--selected': selectedId === point.id }"
        :style="{ opacity: point.opacity }"
        @click="point.visible && emit('select', point.id)"
      >
        <circle :cx="point.x" :cy="point.y" :r="selectedId === point.id ? 5 : 3.2" class="inf-network-globe__node" />
        <circle :cx="point.x" :cy="point.y" :r="selectedId === point.id ? 14 : 9" class="inf-network-globe__halo" />
      </g>
      <circle v-if="userProjected" :cx="userProjected.x" :cy="userProjected.y" r="6" class="inf-network-globe__user" />
    </svg>
    <div class="inf-network-globe__legend">{{ t('nodes.map.cinematic_hint') }}</div>
  </div>
</template>

<script setup lang="ts">
type MapMarker = { id: string, lat: number, lng: number, label: string }
type VisualMode = 'night' | 'light' | 'terrain'
const props = withDefaults(defineProps<{ markers: MapMarker[], userLocation?: { lat: number, lng: number, label: string } | null, minHeight?: number, selectedId?: string | null }>(), { userLocation: null, minHeight: 360, selectedId: null })
const emit = defineEmits<{ select: [string] }>()
const { t } = useI18n()
const { prefersReducedMotion } = useReducedMotion()
const mapMinHeight = computed(() => `${props.minHeight}px`)
const visualMode = ref<VisualMode>('night')
const rotation = ref(0)
let rotationTimer: ReturnType<typeof setInterval> | undefined

const projectedPoints = computed(() => props.markers.map((m) => {
  const projected = projectPoint(m.lat, m.lng)
  return { id: m.id, ...projected }
}))

const userProjected = computed(() => {
  if (!props.userLocation) return null
  return projectPoint(props.userLocation.lat, props.userLocation.lng)
})

const visibleProjectedPoints = computed(() => projectedPoints.value.filter(point => point.visible))

const linkArcs = computed(() => visibleProjectedPoints.value.slice(1).map((point, index) => {
  const origin = visibleProjectedPoints.value[0] ?? { x: 500, y: 280 }
  const cx = (origin.x + point.x) / 2
  const cy = Math.min(origin.y, point.y) - 40 - (index % 4) * 14
  return {
    id: `${origin.x}-${point.x}-${index}`,
    d: `M ${origin.x} ${origin.y} Q ${cx} ${cy} ${point.x} ${point.y}`,
    delay: index * 180,
    duration: 1200 + (index % 5) * 220
  }
}))

const continentTransform = computed(() => `translate(${Math.sin(rotation.value * Math.PI / 180) * 34} 0)`)

const continentPaths = [
  { id: 'americas', d: 'M342 144c-38 18-72 49-80 88-7 35 12 62 37 78 21 14 20 38 34 62 12 20 39 25 52 4 11-18-1-42 10-62 10-18 38-20 47-42 9-21-16-34-28-49-17-18-18-54-72-79Z' },
  { id: 'europe-africa', d: 'M492 144c-34 8-63 28-70 56-6 25 13 39 42 34 22-4 35 5 28 25-12 33-15 76 5 110 23 39 67 36 81-4 10-29-12-53-17-82-4-27 23-34 48-42 31-11 29-42 3-61-27-20-77-46-120-36Z' },
  { id: 'asia', d: 'M586 154c42-19 108-3 141 26 31 27 32 66 0 82-25 13-57 0-80 18-22 17-9 50-36 63-27 14-56-8-58-39-2-24 15-43 4-65-12-24-48-15-51-42-2-20 35-31 80-43Z' },
  { id: 'oceania', d: 'M674 344c22-9 61-5 78 11 16 16 6 36-21 38-26 2-69-25-57-49Z' }
]

function projectPoint(lat: number, lng: number) {
  const latRad = lat * Math.PI / 180
  const lngRad = (lng + rotation.value) * Math.PI / 180
  const depth = Math.cos(latRad) * Math.cos(lngRad)
  const visible = depth >= -0.12
  return {
    x: 500 + 220 * Math.cos(latRad) * Math.sin(lngRad),
    y: 280 - 170 * Math.sin(latRad),
    visible,
    opacity: visible ? 0.38 + Math.max(0, depth) * 0.62 : 0
  }
}

function latRadius(lat: number) {
  return Math.max(24, 220 * Math.cos(lat * Math.PI / 180))
}

onMounted(() => {
  if (prefersReducedMotion.value) {
    return
  }
  rotationTimer = setInterval(() => {
    rotation.value = (rotation.value + 0.16) % 360
  }, 80)
})

onBeforeUnmount(() => {
  if (rotationTimer) {
    clearInterval(rotationTimer)
  }
})
</script>

<style scoped>
.inf-network-globe {
  --globe-core: #17406d;
  --globe-mid: #0a1d3d;
  --globe-edge: #030917;
  --globe-land: rgba(108, 190, 130, 0.46);
  --globe-land-line: rgba(204, 255, 215, 0.22);
  position: relative;
  width: 100%;
  background: radial-gradient(circle at 50% 30%, #132f63 0, #050b18 60%, #02050d 100%);
  border-radius: 8px;
  overflow: hidden;
}

.inf-network-globe--light {
  --globe-core: #d7f2ff;
  --globe-mid: #7fc7e8;
  --globe-edge: #2f6d9f;
  --globe-land: rgba(55, 138, 88, 0.5);
  --globe-land-line: rgba(14, 74, 45, 0.22);
  background: radial-gradient(circle at 50% 30%, #dff7ff 0, #b7ddf6 54%, #6b91b8 100%);
}

.inf-network-globe--terrain {
  --globe-core: #2f6d76;
  --globe-mid: #164c5e;
  --globe-edge: #092134;
  --globe-land: rgba(174, 197, 102, 0.48);
  --globe-land-line: rgba(240, 255, 190, 0.18);
  background: radial-gradient(circle at 50% 30%, #2f6d76 0, #092134 68%, #020812 100%);
}

.inf-network-globe__toolbar {
  position: absolute;
  z-index: 2;
  top: 12px;
  right: 12px;
}

.inf-network-globe__svg { width: 100%; height: 100%; }
.inf-network-globe__sphere { filter: drop-shadow(0 28px 70px rgba(0,0,0,.38)); }
.inf-network-globe__light { mix-blend-mode: screen; }
.inf-network-globe__grid ellipse { fill: none; stroke: rgba(154,203,255,.18); stroke-width: 1; }
.inf-network-globe__continents { transition: transform 220ms linear; }
.inf-network-globe__continents path { fill: var(--globe-land); stroke: var(--globe-land-line); stroke-width: 1.2; }
.inf-network-globe__node { fill: #80f2ff; }
.inf-network-globe__halo { fill: none; stroke: rgba(128,242,255,.5); animation: pulse 2.1s ease infinite; }
.inf-network-globe__node-wrap { cursor: pointer; transition: opacity 160ms ease; }
.inf-network-globe__node-wrap--selected .inf-network-globe__node { fill: #fff; stroke: #00e5ff; stroke-width: 2; }
.inf-network-globe__node-wrap--selected .inf-network-globe__halo { stroke: rgba(255,255,255,.82); }
.inf-network-globe__user { fill: #ffe08a; stroke: #fff; stroke-width: 2; }
.inf-network-globe__arc { fill: none; stroke: rgba(118,192,255,.6); stroke-width: 1.6; stroke-dasharray: 6 8; animation: flow var(--dur) linear infinite; animation-delay: var(--delay); }
.inf-network-globe__legend { position: absolute; left: 12px; bottom: 12px; color: rgba(255,255,255,.82); font-size: .85rem; }
@keyframes flow { from { stroke-dashoffset: 40; } to { stroke-dashoffset: 0; } }
@keyframes pulse { 0%,100% { opacity: .2; } 50% { opacity: .8; } }

@media (prefers-reduced-motion: reduce) {
  .inf-network-globe__halo,
  .inf-network-globe__arc {
    animation: none;
  }
}
</style>
