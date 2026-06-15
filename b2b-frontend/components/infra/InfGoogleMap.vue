<template>
  <div :class="['inf-google-map-shell', { 'inf-google-map-shell--dark': darkMode }]">
    <v-alert v-if="!hasApiKey" type="warning" variant="tonal" density="comfortable">
      {{ t('nodes.map.missing_api_key') }}
    </v-alert>
    <v-alert v-else-if="loadError" type="warning" variant="tonal" density="comfortable">
      {{ t('nodes.map.load_error') }}
    </v-alert>
    <v-skeleton-loader
      v-if="hasApiKey && loading"
      type="image"
      class="inf-google-map inf-google-map--loading"
    />
    <div v-show="hasApiKey && !loading" ref="mapEl" class="inf-google-map" />
  </div>
</template>

<script setup lang="ts">
import { normalizeGeoPoint } from '~/utils/geo'

declare global {
  interface Window {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    google?: any
  }
}

type MapMarker = {
  id: string
  lat: number
  lng: number
  label: string
  status?: string
  location?: string
  hardware?: string
  provider?: string
  health?: string
  modelReadiness?: string
}

const props = withDefaults(defineProps<{
  markers: MapMarker[]
  userLocation?: {
    lat: number
    lng: number
    label: string
  } | null
  zoom?: number
  minHeight?: number
  selectedId?: string | null
  darkMode?: boolean
}>(), {
  zoom: 2,
  userLocation: null,
  minHeight: 360,
  selectedId: null,
  darkMode: false
})

const LIGHT_MAP_STYLES = [
  { featureType: 'poi', stylers: [{ visibility: 'off' }] },
  { featureType: 'transit', stylers: [{ visibility: 'off' }] },
  { featureType: 'administrative', elementType: 'geometry.stroke', stylers: [{ visibility: 'off' }] },
  { featureType: 'road', elementType: 'geometry', stylers: [{ visibility: 'off' }] },
  { featureType: 'landscape.natural', elementType: 'geometry.stroke', stylers: [{ visibility: 'off' }] }
]

const DARK_MAP_STYLES = [
  { elementType: 'geometry', stylers: [{ color: '#0d1b2a' }] },
  { elementType: 'labels.text.fill', stylers: [{ color: '#7fbbe8' }] },
  { elementType: 'labels.text.stroke', stylers: [{ color: '#0d1b2a' }] },
  { featureType: 'administrative', elementType: 'geometry.stroke', stylers: [{ color: '#2a4566' }] },
  { featureType: 'administrative.land_parcel', elementType: 'labels', stylers: [{ visibility: 'off' }] },
  { featureType: 'landscape.man_made', elementType: 'geometry', stylers: [{ color: '#081828' }] },
  { featureType: 'landscape.natural', elementType: 'geometry', stylers: [{ color: '#0a1e30' }] },
  { featureType: 'poi', stylers: [{ visibility: 'off' }] },
  { featureType: 'road', stylers: [{ visibility: 'off' }] },
  { featureType: 'transit', stylers: [{ visibility: 'off' }] },
  { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#060f1e' }] },
  { featureType: 'water', elementType: 'labels.text.fill', stylers: [{ color: '#2d5a7a' }] }
]

const emit = defineEmits<{
  select: [string]
}>()

const { t } = useI18n()
const runtimeConfig = useRuntimeConfig()

const hasApiKey = computed(() => Boolean(runtimeConfig.public.googleMapsApiKey))
const validMarkers = computed(() => props.markers
  .map((marker) => {
    const point = normalizeGeoPoint(marker.lat, marker.lng)
    return point ? { ...marker, ...point } : null
  })
  .filter((marker): marker is MapMarker => marker !== null))
const validUserLocation = computed(() => {
  if (!props.userLocation) {
    return null
  }
  const point = normalizeGeoPoint(props.userLocation.lat, props.userLocation.lng)
  return point ? { ...props.userLocation, ...point } : null
})
const mapEl = ref<HTMLElement | null>(null)
const loadError = ref(false)
const loading = ref(false)
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let map: any = null
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let markerPool: any[] = []
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let userMarker: any = null
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let infoWindow: any = null

const loaderState = useState<Promise<void> | null>('google-maps-loader', () => null)

const mapMinHeight = computed(() => `${props.minHeight}px`)

function loadGoogleMapsScript() {
  if (!import.meta.client || !hasApiKey.value) {
    return Promise.resolve()
  }
  if (window.google?.maps) {
    return Promise.resolve()
  }
  if (loaderState.value) {
    return loaderState.value
  }

  loaderState.value = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(runtimeConfig.public.googleMapsApiKey)}`
    script.async = true
    script.defer = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('google_maps_load_failed'))
    document.head.appendChild(script)
  })
  return loaderState.value
}

function fitToMarkers() {
  if (!map || !window.google?.maps) {
    return
  }
  const bounds = new window.google.maps.LatLngBounds()
  let boundsCount = 0
  validMarkers.value.forEach((marker) => {
    bounds.extend({ lat: marker.lat, lng: marker.lng })
    boundsCount += 1
  })
  if (validUserLocation.value) {
    bounds.extend({ lat: validUserLocation.value.lat, lng: validUserLocation.value.lng })
    boundsCount += 1
  }
  if (boundsCount === 0) {
    return
  }
  map.fitBounds(bounds)
  if (boundsCount === 1) {
    map.setZoom(Math.min(map.getZoom() ?? props.zoom, 9))
  }
}

function clearMarkers() {
  markerPool.forEach((marker) => marker.setMap(null))
  markerPool = []
  userMarker?.setMap(null)
  userMarker = null
  infoWindow?.close()
}

function markerColor(status?: string) {
  if (status === 'UP') return '#00A36C'
  if (status === 'DEGRADED') return '#F59E0B'
  if (status === 'DOWN' || status === 'OUT_OF_SERVICE') return '#DC2626'
  return '#2563EB'
}

function renderMarkers() {
  if (!map || !window.google?.maps) {
    return
  }
  clearMarkers()
  infoWindow = new window.google.maps.InfoWindow({
    pixelOffset: new window.google.maps.Size(0, -4)
  })
  markerPool = validMarkers.value.map((entry) => {
    const marker = new window.google.maps.Marker({
      map,
      position: { lat: entry.lat, lng: entry.lng },
      title: entry.label,
      animation: window.google.maps.Animation.DROP,
      icon: {
        path: window.google.maps.SymbolPath.CIRCLE,
        scale: props.selectedId === entry.id ? 10 : 7,
        fillColor: markerColor(entry.status),
        fillOpacity: 0.95,
        strokeColor: '#FFFFFF',
        strokeWeight: props.selectedId === entry.id ? 3 : 2
      },
      zIndex: props.selectedId === entry.id ? 30 : 10
    })
    marker.addListener('click', () => {
      emit('select', entry.id)
      openInfoWindow(marker, entry)
    })
    if (props.selectedId === entry.id) {
      openInfoWindow(marker, entry)
    }
    return marker
  })
  if (validUserLocation.value) {
    userMarker = new window.google.maps.Marker({
      map,
      position: { lat: validUserLocation.value.lat, lng: validUserLocation.value.lng },
      title: validUserLocation.value.label,
      icon: {
        path: window.google.maps.SymbolPath.CIRCLE,
        scale: 9,
        fillColor: '#0EA5E9',
        fillOpacity: 0.9,
        strokeColor: '#FFFFFF',
        strokeWeight: 3
      },
      zIndex: 20
    })
  }
  fitToMarkers()
}

onMounted(async () => {
  if (!import.meta.client || !mapEl.value || !hasApiKey.value) {
    return
  }
  try {
    loading.value = true
    await loadGoogleMapsScript()
    map = new window.google.maps.Map(mapEl.value, {
      center: { lat: 20, lng: 0 },
      zoom: props.zoom,
      disableDefaultUI: true,
      zoomControl: true,
      fullscreenControl: true,
      gestureHandling: 'cooperative',
      styles: props.darkMode ? DARK_MAP_STYLES : LIGHT_MAP_STYLES
    })
    renderMarkers()
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
})

watch(() => [props.markers, props.userLocation], () => {
  renderMarkers()
}, { deep: true })

watch(() => props.selectedId, () => {
  renderMarkers()
})

onBeforeUnmount(() => {
  clearMarkers()
})

function openInfoWindow(marker: unknown, entry: MapMarker) {
  if (!infoWindow || !window.google?.maps) {
    return
  }

  const content = document.createElement('div')
  content.className = 'inf-google-map-card'
  content.innerHTML = [
    `<strong>${escapeHtml(entry.label)}</strong>`,
    mapCardLine(entry.location),
    mapCardLine(entry.provider),
    mapCardLine(entry.hardware),
    mapCardLine(entry.health),
    mapCardLine(entry.modelReadiness)
  ].filter(Boolean).join('')
  infoWindow.setContent(content)
  infoWindow.open({ map, anchor: marker })
}

function mapCardLine(value: string | undefined) {
  return value ? `<span>${escapeHtml(value)}</span>` : ''
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}
</script>

<style scoped>
.inf-google-map-shell {
  width: 100%;
}

.inf-google-map {
  width: 100%;
  min-height: v-bind(mapMinHeight);
  border-radius: 8px;
  overflow: hidden;
}

.inf-google-map--loading {
  border-radius: 8px;
}

:deep(.inf-google-map-card) {
  display: grid;
  gap: 4px;
  min-width: 180px;
  color: #172033;
  font-family: Inter, Roboto, Arial, sans-serif;
}

:deep(.inf-google-map-card strong) {
  font-size: 0.9rem;
}

:deep(.inf-google-map-card span),
:deep(.inf-google-map-card small) {
  color: #4b5563;
}

.inf-google-map-shell--dark :deep(.inf-google-map-card) {
  background: #0d1b2a;
  border: 1px solid rgba(125, 211, 252, 0.15);
  color: #e1eef8;
  border-radius: 6px;
  padding: 8px 10px;
}

.inf-google-map-shell--dark :deep(.inf-google-map-card span),
.inf-google-map-shell--dark :deep(.inf-google-map-card small) {
  color: #7fbbe8;
}
</style>
