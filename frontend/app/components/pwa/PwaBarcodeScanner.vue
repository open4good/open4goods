<template>
  <div class="pwa-barcode-scanner" data-test="pwa-barcode-scanner">
    <div class="pwa-barcode-scanner__viewport" :data-mode="modeLabel">
      <video
        v-if="isUsingNativeScanner"
        ref="videoRef"
        class="pwa-barcode-scanner__video"
        autoplay
        playsinline
        muted
      />
      <component
        :is="fallbackComponent"
        v-else-if="fallbackComponent"
        class="pwa-barcode-scanner__fallback"
        data-test="pwa-barcode-fallback"
        :paused="!active"
        @decode="handleFallbackDecode"
      />
      <div
        v-else
        class="pwa-barcode-scanner__placeholder"
        data-test="pwa-barcode-placeholder"
      >
        <v-progress-circular
          indeterminate
          size="32"
          color="primary"
          class="mr-2"
        />
        <p class="pwa-barcode-scanner__status">{{ loadingLabel }}</p>
      </div>
    </div>
    <p
      v-if="statusMessage"
      class="pwa-barcode-scanner__status"
      data-test="pwa-barcode-status"
    >
      {{ statusMessage }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import type { Component } from 'vue'

interface Props {
  active: boolean
  loadingLabel: string
  errorLabel: string
}

const props = defineProps<Props>()
const emit = defineEmits<{ (event: 'decode' | 'error', value: string): void }>()

const videoRef = ref<HTMLVideoElement | null>(null)
const fallbackComponent = shallowRef<Component | null>(null)
const isUsingNativeScanner = ref(false)
const statusMessage = ref('')
const device = useDevice()

let mediaStream: MediaStream | null = null
let detector: BarcodeDetector | null = null
let detectionFrame: number | null = null
let fallbackLoader: Promise<void> | null = null

const modeLabel = computed(() =>
  isUsingNativeScanner.value ? 'native' : 'fallback'
)

const stopNativeScanner = () => {
  if (detectionFrame !== null) {
    cancelAnimationFrame(detectionFrame)
    detectionFrame = null
  }

  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }

  detector = null
  isUsingNativeScanner.value = false
}

const emitDecode = (value: string | null) => {
  const normalized = (value ?? '').trim()

  if (!normalized) {
    return
  }

  emit('decode', normalized)
}

const runNativeDetection = async () => {
  if (!detector || !videoRef.value) {
    return
  }

  try {
    const results = await detector.detect(videoRef.value)

    if (results.length > 0) {
      emitDecode(results[0]?.rawValue ?? '')
      stopNativeScanner()
      return
    }
  } catch (error) {
    console.warn('Barcode detection failed', error)
  }

  detectionFrame = requestAnimationFrame(runNativeDetection)
}

const startNativeScanner = async () => {
  if (!import.meta.client) {
    return false
  }

  const hasBarcodeDetector = 'BarcodeDetector' in window
  const canAccessCamera =
    typeof navigator !== 'undefined' && !!navigator.mediaDevices?.getUserMedia

  if (!hasBarcodeDetector || !canAccessCamera || props.active === false) {
    return false
  }

  statusMessage.value = ''

  try {
    const detectorCtor = (
      window as typeof window & { BarcodeDetector: typeof BarcodeDetector }
    ).BarcodeDetector
    detector = new detectorCtor({
      formats: ['ean_13', 'ean_8', 'code_128', 'upc_a'],
    })
    mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: false,
      video: {
        facingMode: device.isMobileOrTablet
          ? { ideal: 'environment' }
          : 'environment',
      },
    })

    if (!videoRef.value) {
      return false
    }

    videoRef.value.srcObject = mediaStream
    await videoRef.value.play()
    isUsingNativeScanner.value = true
    runNativeDetection()
    return true
  } catch (error) {
    console.error('Native barcode scanner failed to start', error)
    statusMessage.value = props.errorLabel
    stopNativeScanner()
    return false
  }
}

const loadFallbackScanner = async () => {
  if (fallbackComponent.value || fallbackLoader) {
    return fallbackLoader
  }

  fallbackLoader = import('vue-barcode-reader')
    .then(module => {
      fallbackComponent.value = module.StreamBarcodeReader
      statusMessage.value = ''
    })
    .catch(error => {
      console.error('Fallback scanner failed to load', error)
      statusMessage.value = props.errorLabel
      emit('error', props.errorLabel)
    })
    .finally(() => {
      fallbackLoader = null
    })

  return fallbackLoader
}

const initialiseScanner = async () => {
  if (!props.active || !import.meta.client) {
    return
  }

  const nativeStarted = await startNativeScanner()

  if (!nativeStarted) {
    await loadFallbackScanner()
  }
}

const handleFallbackDecode = (value: string | null) => {
  emitDecode(value)
}

watch(
  () => props.active,
  async isActive => {
    if (isActive) {
      await initialiseScanner()
    } else {
      stopNativeScanner()
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  stopNativeScanner()
})
</script>

<style scoped lang="sass">
.pwa-barcode-scanner__viewport
  position: relative
  overflow: hidden
  border-radius: 20px
  background: rgba(var(--v-theme-surface-glass), 0.95)
  min-height: 280px
  display: flex
  align-items: center
  justify-content: center

.pwa-barcode-scanner__video,
.pwa-barcode-scanner__fallback
  width: 100%
  height: 100%
  object-fit: cover

.pwa-barcode-scanner__placeholder
  display: flex
  align-items: center
  justify-content: center
  gap: 8px
  flex-direction: column

.pwa-barcode-scanner__status
  text-align: center
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)
  margin-top: 12px

.pwa-barcode-scanner__video
  background: #000
</style>
