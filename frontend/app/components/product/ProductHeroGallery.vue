<template>
  <div class="product-gallery-wrapper" v-bind="$attrs">
    <div v-if="galleryItems.length" class="product-gallery">
      <div
        v-if="heroMedia"
        class="product-gallery__stage"
        :class="{ 'product-gallery__stage--video': heroMediaIsVideo }"
        data-testid="product-gallery-stage"
      >
        <button
          v-if="!heroMediaIsVideo"
          type="button"
          class="product-gallery__stage-trigger"
          :aria-label="stageAriaLabel"
          @click="openGallery(activeMediaIndex)"
          @keydown.enter.prevent="openGallery(activeMediaIndex)"
          @keydown.space.prevent="openGallery(activeMediaIndex)"
        >
          <NuxtImg
            v-if="heroMedia.type === 'image'"
            :src="heroMedia.previewUrl"
            :alt="heroMedia.alt"
            format="webp"
            :width="heroMedia.width"
            :height="heroMedia.height"
            class="product-gallery__stage-media"
            :preload="activeMediaIndex === 0"
            :loading="activeMediaIndex === 0 ? 'eager' : 'lazy'"
            :fetchpriority="activeMediaIndex === 0 ? 'high' : 'auto'"
            @error="handleImageError"
          />
          <NuxtImg
            v-else-if="heroMedia.posterUrl"
            :src="heroMedia.posterUrl"
            :alt="heroMedia.alt"
            format="webp"
            :width="heroMedia.width"
            :height="heroMedia.height"
            class="product-gallery__stage-media"
            @error="handleImageError"
          />
          <div
            v-if="heroMedia.type === 'video'"
            class="product-gallery__stage-overlay"
          >
            <v-icon
              icon="mdi-play-circle-outline"
              size="56"
              class="product-gallery__stage-icon"
            />
            <span class="product-gallery__sr-only">{{
              $t('product.hero.videoBadge')
            }}</span>
          </div>
        </button>
        <div v-else class="product-gallery__stage-video">
          <video
            ref="heroVideoElement"
            class="product-gallery__video"
            controls
            playsinline
            preload="metadata"
            :poster="heroMedia.posterUrl || heroMedia.previewUrl"
          >
            <source :src="heroMedia.videoUrl" />
          </video>
          <div class="product-gallery__video-badge">
            <v-icon icon="mdi-play-circle-outline" size="24" />
            <span>{{ $t('product.hero.videoBadge') }}</span>
          </div>
          <div
            class="product-gallery__stage-overlay product-gallery__stage-overlay--inline"
            aria-hidden="true"
          >
            <span class="product-gallery__sr-only">{{
              $t('product.hero.videoBadge')
            }}</span>
          </div>
          <button
            type="button"
            class="product-gallery__video-gallery-btn"
            :aria-label="stageAriaLabel"
            @click="openGalleryFromVideo(activeMediaIndex)"
            @keydown.enter.prevent="openGalleryFromVideo(activeMediaIndex)"
            @keydown.space.prevent="openGalleryFromVideo(activeMediaIndex)"
          >
            <v-icon icon="mdi-arrow-expand" size="18" />
            <span>{{ galleryButtonLabel }}</span>
          </button>
        </div>
      </div>

      <div
        class="product-gallery__thumbnails"
        :class="{
          'product-gallery__thumbnails--nav': showThumbnailNavigation,
        }"
        role="group"
        :aria-label="thumbnailGroupLabel"
      >
        <button
          v-if="showThumbnailNavigation"
          type="button"
          class="product-gallery__thumbnails-arrow product-gallery__thumbnails-arrow--prev"
          :aria-label="previousThumbnailsLabel"
          :disabled="!canScrollThumbnailsLeft"
          @click="scrollThumbnails('left')"
        >
          <v-icon icon="mdi-chevron-left" size="22" />
        </button>

        <div
          ref="thumbnailViewport"
          class="product-gallery__thumbnails-viewport"
          @scroll="handleThumbnailScroll"
        >
          <ul
            ref="thumbnailList"
            class="product-gallery__thumbnails-list"
            role="list"
          >
            <li
              v-for="(item, index) in galleryItems"
              :key="item.id"
              class="product-gallery__thumbnail"
            >
              <button
                type="button"
                class="product-gallery__thumbnail-button"
                :class="{
                  'product-gallery__thumbnail-button--active':
                    index === activeMediaIndex,
                }"
                data-testid="product-gallery-thumbnail"
                :aria-label="thumbnailAriaLabel(item, index)"
                :aria-pressed="index === activeMediaIndex"
                @click="setActiveMedia(index)"
                @dblclick="openGallery(index)"
                @keydown.enter.prevent="openGallery(index)"
                @keydown.space.prevent="openGallery(index)"
              >
                <NuxtImg
                  :src="item.thumbnailUrl"
                  :alt="item.alt"
                  format="webp"
                  :width="item.thumbnailWidth"
                  :height="item.thumbnailHeight"
                  class="product-gallery__thumbnail-image"
                  @error="handleImageError"
                />
                <span
                  v-if="item.type === 'video'"
                  class="product-gallery__thumbnail-badge"
                  aria-hidden="true"
                >
                  <v-icon icon="mdi-video-outline" size="18" />
                </span>
              </button>
            </li>
          </ul>
        </div>

        <button
          v-if="showThumbnailNavigation"
          type="button"
          class="product-gallery__thumbnails-arrow product-gallery__thumbnails-arrow--next"
          :aria-label="nextThumbnailsLabel"
          :disabled="!canScrollThumbnailsRight"
          @click="scrollThumbnails('right')"
        >
          <v-icon icon="mdi-chevron-right" size="22" />
        </button>
      </div>
    </div>
    <div v-else-if="heroFallbackImage" class="product-gallery__fallback">
      <NuxtImg
        :src="heroFallbackImage"
        :alt="title"
        format="webp"
        :width="960"
        :height="720"
        class="product-hero__fallback"
        @error="handleImageError"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
  type PropType,
} from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProductDto } from '~~/shared/api-client'
import {
  useProductGallery,
  type ProductGalleryItem,
} from '~/composables/useProductGallery'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
  title: {
    type: String,
    required: true,
  },
})

const emit = defineEmits<{
  (e: 'open-inline-gallery', index: number): void
}>()

const heroVideoElement = ref<HTMLVideoElement | null>(null)
const thumbnailViewport = ref<HTMLDivElement | null>(null)
const thumbnailList = ref<HTMLUListElement | null>(null)

const showThumbnailNavigation = ref(false)
const canScrollThumbnailsLeft = ref(false)
const canScrollThumbnailsRight = ref(false)

const { t, te } = useI18n()
const FALLBACK_IMAGE_SRC = '/images/no-image.png'

const { galleryItems, heroFallbackImage } = useProductGallery(
  computed(() => props.product),
  props.title
)

const thumbnailGroupLabel = computed(() =>
  te('product.hero.thumbnails.groupLabel')
    ? t('product.hero.thumbnails.groupLabel')
    : 'Product media thumbnails'
)

const previousThumbnailsLabel = computed(() =>
  te('product.hero.thumbnails.previous')
    ? t('product.hero.thumbnails.previous')
    : 'Scroll thumbnails backward'
)

const nextThumbnailsLabel = computed(() =>
  te('product.hero.thumbnails.next')
    ? t('product.hero.thumbnails.next')
    : 'Scroll thumbnails forward'
)

// Thumbnail helpers
type ThumbnailScrollDirection = 'left' | 'right'
let thumbnailResizeObserver: ResizeObserver | null = null

const updateThumbnailOverflow = () => {
  const viewport = thumbnailViewport.value
  if (!viewport) {
    showThumbnailNavigation.value = false
    canScrollThumbnailsLeft.value = false
    canScrollThumbnailsRight.value = false
    return
  }

  const totalWidth = viewport.scrollWidth
  const visibleWidth = viewport.clientWidth
  const maxScrollLeft = Math.max(totalWidth - visibleWidth, 0)

  showThumbnailNavigation.value = totalWidth - visibleWidth > 1
  canScrollThumbnailsLeft.value = viewport.scrollLeft > 0
  canScrollThumbnailsRight.value = viewport.scrollLeft < maxScrollLeft - 1
}

const handleThumbnailScroll = () => {
  updateThumbnailOverflow()
}

const scrollThumbnails = (direction: ThumbnailScrollDirection) => {
  const viewport = thumbnailViewport.value
  if (!viewport) return

  const scrollAmount = Math.max(viewport.clientWidth * 0.8, 120)
  const delta = direction === 'left' ? -scrollAmount : scrollAmount

  viewport.scrollBy({ left: delta, behavior: 'smooth' })
}

const scrollActiveThumbnailIntoView = () => {
  const viewport = thumbnailViewport.value
  const list = thumbnailList.value
  if (!viewport || !list) return

  const activeItem = list.children[activeMediaIndex.value] as
    | HTMLElement
    | undefined
  if (!activeItem) return

  const itemStart = activeItem.offsetLeft
  const itemEnd = itemStart + activeItem.offsetWidth
  const viewportStart = viewport.scrollLeft
  const viewportEnd = viewportStart + viewport.clientWidth

  if (itemStart < viewportStart) {
    viewport.scrollTo({ left: itemStart, behavior: 'smooth' })
    return
  }

  if (itemEnd > viewportEnd) {
    viewport.scrollTo({
      left: itemEnd - viewport.clientWidth,
      behavior: 'smooth',
    })
  }
}

const observeThumbnailElements = () => {
  if (!import.meta.client) return

  if (!thumbnailResizeObserver) {
    thumbnailResizeObserver = new ResizeObserver(() => {
      updateThumbnailOverflow()
      scrollActiveThumbnailIntoView()
    })
  }

  thumbnailResizeObserver.disconnect()
  const viewport = thumbnailViewport.value
  const list = thumbnailList.value

  if (viewport) thumbnailResizeObserver.observe(viewport)
  if (list) thumbnailResizeObserver.observe(list)

  updateThumbnailOverflow()
}

const handleImageError = (event: Event) => {
  if (!import.meta.client) return
  const target = event.target as HTMLImageElement | null
  if (!target || target.dataset.fallbackApplied === 'true') return
  target.dataset.fallbackApplied = 'true'
  target.src = FALLBACK_IMAGE_SRC
  target.srcset = ''
}

const activeMediaIndex = ref(0)

watch(
  galleryItems,
  items => {
    if (!items.length) {
      activeMediaIndex.value = 0
      void nextTick(() => {
        observeThumbnailElements()
        updateThumbnailOverflow()
      })
      return
    }

    if (activeMediaIndex.value >= items.length) {
      activeMediaIndex.value = items.length - 1
    }

    void nextTick(() => {
      observeThumbnailElements()
      scrollActiveThumbnailIntoView()
    })
  },
  { immediate: true }
)

const heroMedia = computed(
  () => galleryItems.value[activeMediaIndex.value] ?? null
)

const heroMediaIsVideo = computed(
  () => heroMedia.value?.type === 'video' && Boolean(heroMedia.value.videoUrl)
)

const stageAriaLabel = computed(() => {
  const media = heroMedia.value
  if (!media) return t('product.hero.openGalleryFallback')

  const label = media.caption || media.alt || props.title
  return media.type === 'video'
    ? t('product.hero.openGalleryVideo', { label })
    : t('product.hero.openGalleryImage', { label })
})

const thumbnailAriaLabel = (item: ProductGalleryItem, index: number) => {
  const label = item.caption || item.alt || props.title
  const typeKey = item.type === 'video' ? 'video' : 'image'

  return t('product.hero.thumbnailLabel', {
    type: t(`product.hero.mediaType.${typeKey}`),
    index: index + 1,
    total: galleryItems.value.length,
    label,
  })
}

const setActiveMedia = (index: number) => {
  if (index >= 0 && index < galleryItems.value.length) {
    activeMediaIndex.value = index
    void nextTick(() => {
      scrollActiveThumbnailIntoView()
      updateThumbnailOverflow()
    })
  }
}

// Logic for opening gallery
const openGallery = (index: number) => {
  if (!galleryItems.value.length) return
  emit('open-inline-gallery', index)
}

const pauseHeroVideo = () => {
  const element = heroVideoElement.value
  if (element) {
    element.pause()
    element.currentTime = 0
  }
}

const galleryButtonLabel = computed(() =>
  te('product.hero.openGalleryCta')
    ? t('product.hero.openGalleryCta')
    : 'Open gallery'
)

const openGalleryFromVideo = (index: number) => {
  pauseHeroVideo()
  openGallery(index)
}

onMounted(async () => {
  await nextTick()
  observeThumbnailElements()
  scrollActiveThumbnailIntoView()
})

watch([thumbnailViewport, thumbnailList], () => {
  observeThumbnailElements()
})

watch(activeMediaIndex, () => {
  void nextTick(() => {
    scrollActiveThumbnailIntoView()
    updateThumbnailOverflow()
  })
})

onBeforeUnmount(() => {
  thumbnailResizeObserver?.disconnect()
  thumbnailResizeObserver = null
  pauseHeroVideo()
})

// Expose active methods just in case parent needs them
defineExpose({
  setActiveMedia,
})
</script>

<style scoped>
.product-gallery-wrapper {
  border-radius: 24px;
  position: relative;
  background: rgba(15, 23, 42, 0.02);
  display: flex;
  width: 100%;
  min-width: 0;
  overflow: hidden;
}

.product-gallery {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  height: 100%;
  flex: 1 1 auto;
  width: 100%;
  min-width: 0;
}

.product-gallery__stage {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  width: 100%;
  max-width: 100%;
  background: rgba(15, 23, 42, 0.04);
  flex: 1 1 auto;
  aspect-ratio: 4 / 3;
  min-height: 0;
  max-height: min(50vh, 380px);
}

.product-gallery__stage-trigger {
  border: none;
  background: transparent;
  padding: 0;
  width: 100%;
  height: 100%;
  cursor: zoom-in;
  display: block;
  position: relative;
}

.product-gallery__stage-video {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: inherit;
  overflow: hidden;
  background: #000;
}

.product-gallery__video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  background: #000;
}

.product-gallery__video-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.7);
  color: #fff;
  font-size: 0.9rem;
  font-weight: 600;
}

.product-gallery__video-gallery-btn {
  position: absolute;
  right: 16px;
  bottom: 16px;
  border: none;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.85);
  color: #fff;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.35);
  transition:
    background-color 0.2s ease,
    transform 0.2s ease;
}

.product-gallery__video-gallery-btn:hover,
.product-gallery__video-gallery-btn:focus-visible {
  background: rgba(15, 23, 42, 0.95);
  transform: translateY(-1px);
}

.product-gallery__video-gallery-btn:focus-visible {
  outline: 2px solid rgba(var(--v-theme-accent-primary-highlight), 0.6);
  outline-offset: 3px;
}

.product-gallery__stage-media,
.product-hero__fallback {
  width: 100%;
  max-width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.product-gallery__stage-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(
    0deg,
    rgba(15, 23, 42, 0.65),
    rgba(15, 23, 42, 0.2)
  );
  pointer-events: none;
}

.product-gallery__stage--video .product-gallery__stage-overlay {
  background: linear-gradient(
    0deg,
    rgba(15, 23, 42, 0.75),
    rgba(15, 23, 42, 0.25)
  );
}

.product-gallery__stage-overlay--inline {
  background: linear-gradient(
    0deg,
    rgba(15, 23, 42, 0.35),
    rgba(15, 23, 42, 0.05)
  );
}

.product-gallery__stage-icon {
  color: rgba(var(--v-theme-hero-overlay-strong), 0.9);
}

.product-gallery__thumbnails {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  width: 100%;
  justify-content: center;
  margin-top: auto;
  min-width: 0;
}

.product-gallery__thumbnails--nav {
  padding-inline: 0.25rem;
  justify-content: space-between;
}

.product-gallery__thumbnails-viewport {
  flex: 1 1 auto;
  overflow: hidden;
  scroll-behavior: smooth;
  min-width: 0;
}

.product-gallery__thumbnails-list {
  list-style: none;
  display: flex;
  gap: 0.6rem;
  padding: 0;
  margin: 0;
}

.product-gallery__thumbnail {
  flex: 0 0 auto;
}

.product-gallery__thumbnails-arrow {
  border: none;
  background: rgba(var(--v-theme-surface-default), 0.92);
  color: rgb(var(--v-theme-text-neutral-strong));
  width: 48px;
  height: 48px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease;
}

.product-gallery__thumbnails-arrow:hover:not(:disabled) {
  background: rgba(var(--v-theme-surface-default), 1);
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.16);
}

.product-gallery__thumbnails-arrow:focus-visible {
  outline: 2px solid rgba(var(--v-theme-accent-primary-highlight), 0.6);
  outline-offset: 3px;
}

.product-gallery__thumbnails-arrow:disabled {
  opacity: 0.4;
  box-shadow: none;
  cursor: default;
}

.product-gallery__thumbnail-button {
  position: relative;
  display: inline-flex;
  border: none;
  padding: 0;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  background: transparent;
  transition:
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.product-gallery__thumbnail-button:hover {
  transform: translateY(-2px);
}

.product-gallery__thumbnail-button:focus-visible {
  outline: 2px solid rgba(var(--v-theme-accent-primary-highlight), 0.6);
  outline-offset: 2px;
}

.product-gallery__thumbnail-button--active {
  box-shadow: 0 0 0 2px rgba(var(--v-theme-accent-primary-highlight), 0.6);
}

.product-gallery__thumbnail-image {
  width: 86px;
  height: 86px;
  object-fit: cover;
  display: block;
}

.product-gallery__thumbnail-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 0.25rem;
  background: rgba(15, 23, 42, 0.75);
  color: #fff;
}

/* Lightbox styles are removed as it is now inline */
.product-gallery__sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}

.product-gallery__fallback {
  border-radius: 20px;
  overflow: hidden;
  aspect-ratio: 4 / 3;
  background: rgba(15, 23, 42, 0.04);
}

@media (max-width: 768px) {
  .product-gallery__stage {
    aspect-ratio: 1 / 1;
    max-height: 65vh;
  }

  .product-gallery__stage-media,
  .product-gallery__video,
  .product-hero__fallback {
    object-fit: contain;
    background: rgba(15, 23, 42, 0.02);
  }
}

@media (max-width: 1280px) {
  .product-gallery-wrapper {
    min-height: clamp(214px, 27vw, 240px);
  }

  .product-gallery__stage {
    min-height: clamp(150px, 20vw, 214px);
    max-height: clamp(174px, 23vw, 227px);
  }
}

@media (max-width: 960px) {
  .product-gallery-wrapper {
    min-height: 180px;
  }

  .product-gallery__stage {
    min-height: 140px;
    max-height: 180px;
  }

  .product-gallery__thumbnails {
    gap: 0.5rem;
  }

  .product-gallery__thumbnails-arrow {
    width: 44px;
    height: 44px;
  }

  .product-gallery__thumbnail-image {
    width: 64px;
    height: 64px;
  }
}
</style>
