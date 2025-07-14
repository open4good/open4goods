<script setup lang="ts">
import { DEFAULT_IMAGE, getImageUrl } from '~/utils/images/image'

interface Props {
  src: string
  alt: string
  width?: number | string
  height?: number | string
  fallback?: string
  placeholderText?: string
  cover?: boolean
  loading?: 'lazy' | 'eager'
}

const props = withDefaults(defineProps<Props>(), {
  width: 200,
  height: 200,
  fallback: undefined,
  placeholderText: 'Image non disponible',
  cover: true,
  loading: 'lazy',
})

const imageError = ref(false)
const currentSrc = ref(getImageUrl(props.src, props.fallback || DEFAULT_IMAGE))

const handleError = (payload: string | Event) => {
  imageError.value = true

  // If it's an Event, try to get the target element
  if (payload instanceof Event) {
    const target = payload.target as HTMLImageElement
    if (target) {
      target.src = props.fallback || DEFAULT_IMAGE
    }
  }
  // If it's a string, we just set the error state
  // The fallback will be handled by the component logic
}

const imageStyle = computed(() => ({
  width: typeof props.width === 'number' ? `${props.width}px` : props.width,
  height: typeof props.height === 'number' ? `${props.height}px` : props.height,
  objectFit: props.cover ? 'cover' : 'contain',
}))
</script>

<template>
  <div class="robust-image-container">
    <!-- Show placeholder if image failed to load -->
    <ImagePlaceholder
      v-if="imageError"
      :width="width"
      :height="height"
      :text="placeholderText"
    />

    <!-- Show image if not in error state -->
    <NuxtImg
      v-else
      :src="currentSrc"
      :alt="alt"
      :style="imageStyle"
      class="robust-image"
      :loading="loading"
      @error="handleError"
    />
  </div>
</template>

<style scoped>
.robust-image-container {
  display: flex;
  align-items: center;
  justify-content: center;
}

.robust-image {
  border-radius: 4px;
  flex-shrink: 0;
}
</style>
