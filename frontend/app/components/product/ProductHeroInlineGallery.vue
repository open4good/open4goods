<template>
  <Teleport to="body" :disabled="!isFullscreen">
    <div
      class="product-inline-gallery"
      :class="{ 'product-inline-gallery--fullscreen': isFullscreen }"
      v-bind="$attrs"
    >
      <!-- Toolbar -->
      <div class="product-inline-gallery__toolbar">
        <div class="product-inline-gallery__controls">
          <v-btn
            icon="mdi-minus"
            variant="text"
            density="comfortable"
            :disabled="scale <= 1"
            @click="zoomOut"
          />
          <span class="product-inline-gallery__zoom-level">
            {{ Math.round(scale * 100) }}%
          </span>
          <v-btn
            icon="mdi-plus"
            variant="text"
            density="comfortable"
            :disabled="scale >= maxScale"
            @click="zoomIn"
          />
          <v-divider vertical class="mx-2" />
          <v-btn
            icon="mdi-rotate-right"
            variant="text"
            density="comfortable"
            @click="rotate"
          />
          <v-btn
            icon="mdi-restore"
            variant="text"
            density="comfortable"
            :disabled="isReset"
            @click="resetTransform"
          />
        </div>

        <div class="product-inline-gallery__actions">
          <v-btn
            :icon="isFullscreen ? 'mdi-fullscreen-exit' : 'mdi-fullscreen'"
            variant="text"
            density="comfortable"
            @click="toggleFullscreen"
          />
          <v-btn
            icon="mdi-close"
            variant="text"
            density="comfortable"
            @click="$emit('close')"
          />
        </div>
      </div>

      <!-- Main Stage -->
      <div
        ref="stageRef"
        class="product-inline-gallery__stage"
        @wheel.prevent="handleWheel"
        @mousedown="startDrag"
        @mousemove="onDrag"
        @mouseup="stopDrag"
        @mouseleave="stopDrag"
        @touchstart="startDrag"
        @touchmove="onDrag"
        @touchend="stopDrag"
      >
        <div class="product-inline-gallery__image-wrapper" :style="imageStyle">
          <template v-if="currentMedia.type === 'video'">
            <video
              :src="currentMedia.videoUrl"
              :poster="currentMedia.posterUrl"
              controls
              playsinline
              class="product-inline-gallery__media"
            />
          </template>
          <template v-else>
            <NuxtImg
              :src="currentMedia.originalUrl"
              :alt="currentMedia.alt"
              class="product-inline-gallery__media"
              @dragstart.prevent
            />
          </template>
        </div>
      </div>

      <!-- Navigation Arrows -->
      <button
        v-if="hasMultipleItems"
        class="product-inline-gallery__nav product-inline-gallery__nav--prev"
        @click="prev"
      >
        <v-icon icon="mdi-chevron-left" size="32" />
      </button>
      <button
        v-if="hasMultipleItems"
        class="product-inline-gallery__nav product-inline-gallery__nav--next"
        @click="next"
      >
        <v-icon icon="mdi-chevron-right" size="32" />
      </button>

      <!-- Thumbs Strip (Optional/Simplified) -->
      <div class="product-inline-gallery__footer">
        <div class="product-inline-gallery__counter">
          {{ currentIndex + 1 }} / {{ items.length }}
        </div>
        <div
          v-if="currentMedia.caption"
          class="product-inline-gallery__caption"
        >
          {{ currentMedia.caption }}
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useEventListener } from '@vueuse/core'

interface GalleryItem {
  id: string
  type: 'image' | 'video'
  originalUrl: string
  previewUrl?: string
  videoUrl?: string
  posterUrl?: string
  alt?: string
  caption?: string
}

const props = defineProps<{
  items: GalleryItem[]
  startIndex: number
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'update:index', index: number): void
}>()

const currentIndex = ref(props.startIndex)
const scale = ref(1)
const rotation = ref(0)
const translateX = ref(0)
const translateY = ref(0)
const isDragging = ref(false)
const items = computed(() => props.items)
const currentMedia = computed(() => items.value[currentIndex.value])
const hasMultipleItems = computed(() => items.value.length > 1)
const isFullscreen = ref(false)

const startX = ref(0)
const startY = ref(0)

const maxScale = 5
const step = 0.5

const isReset = computed(
  () =>
    scale.value === 1 &&
    rotation.value === 0 &&
    translateX.value === 0 &&
    translateY.value === 0
)

const imageStyle = computed(() => ({
  transform: `translate(${translateX.value}px, ${translateY.value}px) rotate(${rotation.value}deg) scale(${scale.value})`,
  transition: isDragging.value ? 'none' : 'transform 0.2s ease-out',
  cursor:
    scale.value > 1 ? (isDragging.value ? 'grabbing' : 'grab') : 'default',
}))

// Zoom Logic
const zoomIn = () => {
  scale.value = Math.min(scale.value + step, maxScale)
}

const zoomOut = () => {
  scale.value = Math.max(scale.value - step, 1)
  if (scale.value === 1) resetPosition()
}

const handleWheel = (e: WheelEvent) => {
  if (e.ctrlKey || e.metaKey || scale.value > 1) {
    const delta = e.deltaY > 0 ? -0.2 : 0.2
    const newScale = Math.min(Math.max(scale.value + delta, 1), maxScale)
    scale.value = newScale
    if (newScale === 1) resetPosition()
  }
}

// Rotate Logic
const rotate = () => {
  rotation.value = (rotation.value + 90) % 360
}

const resetTransform = () => {
  scale.value = 1
  rotation.value = 0
  resetPosition()
}

const resetPosition = () => {
  translateX.value = 0
  translateY.value = 0
}

// Drag Logic
const startDrag = (e: MouseEvent | TouchEvent) => {
  if (scale.value <= 1) return
  isDragging.value = true

  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY

  startX.value = clientX - translateX.value
  startY.value = clientY - translateY.value
}

const onDrag = (e: MouseEvent | TouchEvent) => {
  if (!isDragging.value) return
  e.preventDefault()

  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY

  translateX.value = clientX - startX.value
  translateY.value = clientY - startY.value
}

const stopDrag = () => {
  isDragging.value = false
}

// Navigation
const prev = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
    resetTransform()
  } else {
    // Optional: Loop
    currentIndex.value = items.value.length - 1
    resetTransform()
  }
}

const next = () => {
  if (currentIndex.value < items.value.length - 1) {
    currentIndex.value++
    resetTransform()
  } else {
    // Optional: Loop
    currentIndex.value = 0
    resetTransform()
  }
}

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
}

// Keyboard support
useEventListener(window, 'keydown', e => {
  if (e.key === 'Escape') {
    if (isFullscreen.value) toggleFullscreen()
    else emit('close')
  }
  if (e.key === 'ArrowLeft') prev()
  if (e.key === 'ArrowRight') next()
})

watch(currentIndex, val => {
  emit('update:index', val)
})

watch(
  () => props.startIndex,
  val => {
    currentIndex.value = val
    resetTransform()
  }
)
</script>

<style scoped>
.product-inline-gallery {
  position: absolute;
  inset: 0;
  background: white;
  z-index: 10;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: inherit;
}

.product-inline-gallery--fullscreen {
  position: fixed;
  z-index: 10000; /* High enough to cover sticky headers */
  border-radius: 0;
}

.product-inline-gallery__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 1rem;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  z-index: 20;
}

.product-inline-gallery__controls {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.product-inline-gallery__zoom-level {
  font-size: 0.85rem;
  font-weight: 500;
  min-width: 3ch;
  text-align: center;
  color: #666;
}

.product-inline-gallery__stage {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
}

.product-inline-gallery__image-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  transform-origin: center center;
  will-change: transform;
}

.product-inline-gallery__media {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  user-select: none;
}

.product-inline-gallery__nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 1px solid rgba(0, 0, 0, 0.1);
  color: #333;
  transition: all 0.2s;
  z-index: 15;
}

.product-inline-gallery__nav:hover {
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.product-inline-gallery__nav--prev {
  left: 1rem;
}

.product-inline-gallery__nav--next {
  right: 1rem;
}

.product-inline-gallery__footer {
  padding: 0.75rem 1rem;
  background: rgba(255, 255, 255, 0.9);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.9rem;
  color: #666;
  z-index: 20;
}

.product-inline-gallery__counter {
  font-weight: 500;
  color: #333;
}
</style>
