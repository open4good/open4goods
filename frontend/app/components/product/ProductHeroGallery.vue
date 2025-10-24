<template>
  <div class="product-gallery-wrapper" v-bind="$attrs">
    <ClientOnly>
      <template #default>
        <div v-if="galleryItems.length" class="product-gallery">
          <button
            v-if="heroMedia"
            type="button"
            class="product-gallery__stage"
            :class="{ 'product-gallery__stage--video': heroMedia.type === 'video' }"
            :aria-label="stageAriaLabel"
            data-testid="product-gallery-stage"
            @click="openGallery(activeMediaIndex)"
            @keydown.enter.prevent="openGallery(activeMediaIndex)"
            @keydown.space.prevent="openGallery(activeMediaIndex)"
          >
            <NuxtImg
              v-if="heroMedia.type === 'image'"
              :src="heroMedia.previewUrl"
              :alt="heroMedia.alt"
              class="product-gallery__stage-media"
              format="webp"
              :width="heroMedia.width"
              :height="heroMedia.height"
            />
            <NuxtImg
              v-else-if="heroMedia.posterUrl"
              :src="heroMedia.posterUrl"
              :alt="heroMedia.alt"
              class="product-gallery__stage-media"
              format="webp"
              :width="heroMedia.width"
              :height="heroMedia.height"
            />
            <div v-if="heroMedia.type === 'video'" class="product-gallery__stage-overlay">
              <v-icon icon="mdi-play-circle-outline" size="56" class="product-gallery__stage-icon" />
              <span class="product-gallery__sr-only">{{ $t('product.hero.videoBadge') }}</span>
            </div>
          </button>

          <ul class="product-gallery__thumbnails" role="list">
            <li
              v-for="(item, index) in galleryItems"
              :key="item.id"
              class="product-gallery__thumbnail"
            >
              <button
                type="button"
                class="product-gallery__thumbnail-button"
                :class="{ 'product-gallery__thumbnail-button--active': index === activeMediaIndex }"
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
                  class="product-gallery__thumbnail-image"
                  format="webp"
                  :width="item.thumbnailWidth"
                  :height="item.thumbnailHeight"
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

          <div v-if="pictureSwipeComponent" ref="pictureSwipeContainer" class="product-gallery__lightbox">
            <component
              :is="pictureSwipeComponent"
              ref="pictureSwipeRef"
              :items="pictureSwipeItems"
              :options="pictureSwipeOptions"
            />
          </div>
        </div>
        <div v-else-if="heroFallbackImage" class="product-gallery__fallback">
          <NuxtImg
            :src="heroFallbackImage"
            :alt="title"
            class="product-hero__fallback"
            format="webp"
            :width="600"
            :height="600"
          />
        </div>
      </template>
      <template #fallback>
        <NuxtImg
          v-if="heroFallbackImage"
          :src="heroFallbackImage"
          :alt="title"
          class="product-hero__fallback"
          format="webp"
          :width="600"
          :height="600"
        />
      </template>
    </ClientOnly>
  </div>
</template>

<script setup lang="ts">
import {
  computed,
  nextTick,
  onMounted,
  ref,
  shallowRef,
  watch,
  type ComponentPublicInstance,
  type DefineComponent,
  type PropType,
} from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProductDto } from '~~/shared/api-client'
import type { VuePictureSwipeItem, VuePictureSwipeOptions } from 'vue3-picture-swipe'

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

type PictureSwipeComponent = DefineComponent<{ items: VuePictureSwipeItem[]; options?: VuePictureSwipeOptions }>

type PictureSwipeComponentInstance = ComponentPublicInstance<{ pswp?: LightboxInstance }> & {
  open?: (index: number) => void
  $el?: HTMLElement
}

const pictureSwipeComponent = shallowRef<PictureSwipeComponent | null>(null)
const pictureSwipeRef = ref<PictureSwipeComponentInstance | null>(null)
const pictureSwipeContainer = ref<HTMLElement | null>(null)

const { t } = useI18n()
const nuxtImage = useImage()

type ImageModifiers = Parameters<typeof nuxtImage>[1]

const resolveImageUrl = (src: string | null | undefined, modifiers?: ImageModifiers) => {
  if (!src) {
    return ''
  }

  try {
    return nuxtImage(src, modifiers)
  } catch {
    return src
  }
}

const fallbackDimension = (value: number | null | undefined, fallback: number) =>
  typeof value === 'number' && value > 0 ? value : fallback

const DEFAULT_IMAGE_WIDTH = 1600
const DEFAULT_IMAGE_HEIGHT = 1200
const DEFAULT_THUMBNAIL_SIZE = 200
const DEFAULT_VIDEO_WIDTH = 1280
const DEFAULT_VIDEO_HEIGHT = 720

const coverImageRaw = computed(
  () =>
    props.product.resources?.coverImagePath ??
    props.product.resources?.externalCover ??
    props.product.base?.coverImagePath ??
    null,
)

interface ProductGalleryItem {
  id: string
  type: 'image' | 'video'
  originalUrl: string
  previewUrl: string
  thumbnailUrl: string
  thumbnailWidth: number
  thumbnailHeight: number
  width: number
  height: number
  alt: string
  caption: string
  videoUrl?: string
  posterUrl?: string
}

type LightboxItem = {
  el?: Element | null
  html?: string
  w?: number
  h?: number
  [key: string]: unknown
}

interface LightboxInstance {
  listen(event: 'gettingData', handler: (index: number, item: LightboxItem) => void): void
  listen(event: string, handler: (...args: unknown[]) => void): void
  getCurrentIndex?: () => number
  container?: HTMLElement
  init?: () => void
}

const galleryItems = computed<ProductGalleryItem[]>(() => {
  const images = props.product.resources?.images ?? []
  const videos = props.product.resources?.videos ?? []

  const items: ProductGalleryItem[] = []
  const fallbackPoster =
    coverImageRaw.value ??
    images[0]?.url ??
    images[0]?.originalUrl ??
    ''

  images.forEach((image) => {
    const original = image.originalUrl ?? image.url ?? ''
    if (!original) {
      return
    }

    const source = image.url ?? original
    const caption = image.datasourceName ?? props.title
    const width = fallbackDimension(image.width, DEFAULT_IMAGE_WIDTH)
    const height = fallbackDimension(image.height, DEFAULT_IMAGE_HEIGHT)
    const preview = resolveImageUrl(source, {
      width: 1200,
      height: 900,
      fit: 'cover',
      format: 'webp',
    }) || source
    const thumbnail = resolveImageUrl(source, {
      width: DEFAULT_THUMBNAIL_SIZE,
      height: DEFAULT_THUMBNAIL_SIZE,
      fit: 'cover',
      format: 'webp',
    }) || preview

    items.push({
      id: `image-${image.cacheKey ?? original}`,
      type: 'image',
      originalUrl: original,
      previewUrl: preview,
      thumbnailUrl: thumbnail,
      thumbnailWidth: DEFAULT_THUMBNAIL_SIZE,
      thumbnailHeight: DEFAULT_THUMBNAIL_SIZE,
      width,
      height,
      alt: image.fileName ?? caption,
      caption,
      posterUrl: preview,
    })
  })

  videos.forEach((video) => {
    const url = video.url ?? ''
    if (!url) {
      return
    }

    const caption = video.datasourceName ?? props.title
    const posterSource = fallbackPoster || coverImageRaw.value || ''
    const poster = posterSource
      ? resolveImageUrl(posterSource, {
          width: DEFAULT_VIDEO_WIDTH,
          height: DEFAULT_VIDEO_HEIGHT,
          fit: 'cover',
          format: 'webp',
        }) || posterSource
      : ''
    const thumbnail = posterSource
      ? resolveImageUrl(posterSource, {
          width: DEFAULT_THUMBNAIL_SIZE,
          height: DEFAULT_THUMBNAIL_SIZE,
          fit: 'cover',
          format: 'webp',
        }) || posterSource
      : ''

    items.push({
      id: `video-${video.cacheKey ?? url}`,
      type: 'video',
      originalUrl: poster || url,
      previewUrl: poster || thumbnail || url,
      thumbnailUrl: thumbnail || poster || url,
      thumbnailWidth: DEFAULT_THUMBNAIL_SIZE,
      thumbnailHeight: DEFAULT_THUMBNAIL_SIZE,
      width: DEFAULT_VIDEO_WIDTH,
      height: DEFAULT_VIDEO_HEIGHT,
      alt: video.fileName ?? caption,
      caption,
      videoUrl: url,
      posterUrl: poster || thumbnail || url,
    })
  })

  return items.filter((item) => Boolean(item.originalUrl))
})

const heroFallbackImage = computed(() => {
  const fallback = coverImageRaw.value ?? galleryItems.value[0]?.previewUrl ?? null
  if (!fallback) {
    return null
  }

  return resolveImageUrl(fallback, {
    width: 960,
    height: 720,
    fit: 'cover',
    format: 'webp',
  }) || fallback
})

const activeMediaIndex = ref(0)

watch(
  galleryItems,
  (items) => {
    if (!items.length) {
      activeMediaIndex.value = 0
      return
    }

    if (activeMediaIndex.value >= items.length) {
      activeMediaIndex.value = items.length - 1
    }
  },
  { immediate: true },
)

const heroMedia = computed(() => galleryItems.value[activeMediaIndex.value] ?? null)

const stageAriaLabel = computed(() => {
  const media = heroMedia.value
  if (!media) {
    return t('product.hero.openGalleryFallback')
  }

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

const escapeMap: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
}

const escapeHtml = (value: string) => value.replace(/[&<>"']/g, (char) => escapeMap[char] ?? char)
const escapeAttribute = (value: string | null | undefined) => escapeHtml(String(value ?? ''))

const pictureSwipeItems = computed<VuePictureSwipeItem[]>(() =>
  galleryItems.value.map((item) => {
    const caption = item.caption || props.title
    const sanitizedCaption = escapeHtml(caption)

    const datasetAttributes = [
      `data-type="${item.type}"`,
      `data-width="${item.width}"`,
      `data-height="${item.height}"`,
    ]

    if (item.videoUrl) {
      datasetAttributes.push(`data-video="${escapeAttribute(item.videoUrl)}"`)
    }

    if (item.posterUrl) {
      datasetAttributes.push(`data-poster="${escapeAttribute(item.posterUrl)}"`)
    }

    const htmlAfterThumbnail = `<span class="product-gallery__lightbox-caption" ${datasetAttributes.join(' ')}>${sanitizedCaption}</span>`

    return {
      src: item.type === 'video' ? item.posterUrl ?? item.originalUrl : item.originalUrl,
      thumbnail: item.thumbnailUrl,
      w: item.width,
      h: item.height,
      title: sanitizedCaption,
      alt: item.alt,
      type: item.type,
      htmlAfterThumbnail,
    }
  }),
)

const pictureSwipeOptions = computed<VuePictureSwipeOptions>(() => {
  const base: VuePictureSwipeOptions = {
    shareEl: false,
    fullscreenEl: true,
    zoomEl: true,
    counterEl: true,
    history: false,
    bgOpacity: 0.95,
    closeOnScroll: false,
  }

  if (import.meta.client) {
    base.getThumbBoundsFn = (index: number) => {
      const thumbnails = document.querySelectorAll<HTMLElement>('.product-gallery__thumbnail-image')
      const element = thumbnails[index]

      if (!element) {
        const scrollY = window.scrollY || document.documentElement.scrollTop
        return { x: window.innerWidth / 2, y: scrollY + window.innerHeight / 2, w: 0 }
      }

      const rect = element.getBoundingClientRect()
      const scrollY = window.scrollY || document.documentElement.scrollTop

      return {
        x: rect.left,
        y: rect.top + scrollY,
        w: rect.width,
      }
    }
  }

  return base
})

const lightboxBound = ref(false)

const bindLightboxListeners = () => {
  const instance = (pictureSwipeRef.value as ComponentPublicInstance<{ pswp?: LightboxInstance }> | null)?.pswp

  if (!instance || lightboxBound.value) {
    return
  }

  lightboxBound.value = true
  let activeVideo: HTMLVideoElement | null = null

  const stopVideo = () => {
    if (activeVideo) {
      activeVideo.pause()
      activeVideo.removeAttribute('src')
      activeVideo.load()
      activeVideo = null
    }
  }

  instance.listen('gettingData', (_index: number, item: LightboxItem) => {
    const captionElement = item?.el?.querySelector?.('.product-gallery__lightbox-caption') as HTMLElement | undefined
    const dataset = captionElement?.dataset

    if (dataset?.type === 'video' && dataset.video) {
      const poster = dataset.poster ? ` poster="${dataset.poster}"` : ''
      item.html = `<div class="product-gallery__lightbox-video"><video controls playsinline${poster} src="${dataset.video}"></video></div>`
      item.w = Number(dataset.width) || item.w || DEFAULT_VIDEO_WIDTH
      item.h = Number(dataset.height) || item.h || DEFAULT_VIDEO_HEIGHT
    }
  })

  instance.listen('afterChange', () => {
    activeMediaIndex.value = instance.getCurrentIndex?.() ?? activeMediaIndex.value
    stopVideo()

    nextTick(() => {
      const video = instance.container?.querySelector?.('.product-gallery__lightbox-video video')
      if (video instanceof HTMLVideoElement) {
        activeVideo = video
        video.play().catch(() => {})
      }
    })
  })

  const resetBinding = () => {
    stopVideo()
    lightboxBound.value = false
  }

  instance.listen('beforeChange', stopVideo)
  instance.listen('close', resetBinding)
  instance.listen('destroy', resetBinding)
}

const setActiveMedia = (index: number) => {
  if (index >= 0 && index < galleryItems.value.length) {
    activeMediaIndex.value = index
  }
}

const ensureLightbox = async () => {
  if (pictureSwipeComponent.value || !import.meta.client) {
    return
  }

  try {
    const [{ default: VuePictureSwipe }] = await Promise.all([import('vue3-picture-swipe')])
    pictureSwipeComponent.value = VuePictureSwipe
  } catch (error) {
    console.error('Failed to load gallery', error)
  }
}

const openGallery = async (index: number) => {
  if (!galleryItems.value.length) {
    return
  }

  await ensureLightbox()
  await nextTick()

  const safeIndex = Math.min(Math.max(index, 0), galleryItems.value.length - 1)
  activeMediaIndex.value = safeIndex

  const componentInstance = pictureSwipeRef.value

  if (componentInstance?.open) {
    componentInstance.open(safeIndex)
    window.setTimeout(bindLightboxListeners, 150)
    return
  }

  const rootElement = (componentInstance?.$el ?? pictureSwipeContainer.value) as HTMLElement | undefined
  const anchors = rootElement?.querySelectorAll<HTMLAnchorElement>('figure.gallery-thumbnail a')
  const target = anchors?.[safeIndex]

  if (target) {
    target.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    window.setTimeout(bindLightboxListeners, 150)
  }
}

onMounted(async () => {
  await ensureLightbox()
})
</script>

<style scoped>
.product-gallery-wrapper {
  border-radius: 24px;
  position: relative;
  min-height: 420px;
  background: rgba(15, 23, 42, 0.02);
}

.product-gallery {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  height: 100%;
}

.product-gallery__stage {
  position: relative;
  border: none;
  padding: 0;
  border-radius: 20px;
  overflow: hidden;
  width: 100%;
  background: rgba(15, 23, 42, 0.04);
  cursor: zoom-in;
  min-height: 360px;
  aspect-ratio: 4 / 3;
}

.product-gallery__stage-media,
.product-hero__fallback {
  width: 100%;
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
  background: linear-gradient(0deg, rgba(15, 23, 42, 0.65), rgba(15, 23, 42, 0.2));
  pointer-events: none;
}

.product-gallery__stage--video .product-gallery__stage-overlay {
  background: linear-gradient(0deg, rgba(15, 23, 42, 0.75), rgba(15, 23, 42, 0.25));
}

.product-gallery__stage-icon {
  color: rgba(var(--v-theme-hero-overlay-strong), 0.9);
}

.product-gallery__thumbnails {
  list-style: none;
  display: flex;
  gap: 0.75rem;
  padding: 0;
  margin: 0;
  overflow-x: auto;
  scrollbar-width: thin;
}

.product-gallery__thumbnail {
  flex: 0 0 auto;
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
  transition: box-shadow 0.2s ease, transform 0.2s ease;
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

.product-gallery__lightbox {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
}

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

.product-gallery__lightbox-video {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
}

.product-gallery__lightbox-video video {
  width: 100%;
  height: 100%;
  max-height: 100vh;
  object-fit: contain;
}

.product-gallery__fallback {
  border-radius: 20px;
  overflow: hidden;
  aspect-ratio: 4 / 3;
  background: rgba(15, 23, 42, 0.04);
}

@media (max-width: 1280px) {
  .product-gallery__stage {
    min-height: 320px;
  }
}

@media (max-width: 960px) {
  .product-gallery-wrapper {
    min-height: 280px;
  }

  .product-gallery__stage {
    min-height: 240px;
  }

  .product-gallery__thumbnail-image {
    width: 68px;
    height: 68px;
  }
}
</style>
