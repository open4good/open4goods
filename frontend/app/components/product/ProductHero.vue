<template>
  <section
    class="product-hero"
    itemscope
    itemtype="https://schema.org/Product"
  >
    <meta itemprop="sku" :content="String(product.base?.gtin ?? '')" />
    <meta itemprop="brand" :content="product.identity?.brand ?? ''" />

    <div class="product-hero__gallery">
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
            <span v-if="heroMedia.caption" class="product-gallery__caption">{{ heroMedia.caption }}</span>
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

    <div class="product-hero__details">
      <p v-if="product.identity?.brand" class="product-hero__eyebrow">
        {{ product.identity.brand }}
      </p>
      <h1 class="product-hero__title" itemprop="name">
        {{ title }}
      </h1>
      <p v-if="product.identity?.model" class="product-hero__subtitle">
        {{ product.identity.model }}
      </p>

      <div class="product-hero__meta">
        <ImpactScore
          v-if="impactScore !== null"
          :score="impactScore"
          :max="5"
          size="large"
          :show-value="true"
          class="product-hero__impact"
        />
        <div v-if="product.base?.gtinInfo" class="product-hero__origin">
          <NuxtImg
            v-if="product.base.gtinInfo.countryFlagUrl"
            :src="product.base.gtinInfo.countryFlagUrl"
            :alt="product.base.gtinInfo.countryName ?? ''"
            width="32"
            height="24"
            class="product-hero__flag"
          />
          <span>
            {{ product.base.gtinInfo.countryName }}
          </span>
        </div>
      </div>

      <div class="product-hero__facts">
        <div class="product-hero__fact">
          <span class="product-hero__fact-label">{{ $t('product.hero.gtin') }}</span>
          <span class="product-hero__fact-value" itemprop="gtin13">
            {{ product.gtin }}
          </span>
        </div>
        <div class="product-hero__fact">
          <span class="product-hero__fact-label">{{ $t('product.hero.offersCount') }}</span>
          <span class="product-hero__fact-value">
            {{ offersCountLabel }}
          </span>
        </div>
      </div>
    </div>

    <aside class="product-hero__pricing">
      <div class="product-hero__pricing-card" itemprop="offers" itemscope itemtype="https://schema.org/AggregateOffer">
        <meta itemprop="offerCount" :content="String(product.offers?.offersCount ?? 0)" />
        <meta itemprop="priceCurrency" :content="product.offers?.bestPrice?.currency ?? 'EUR'" />
        <h2 class="product-hero__pricing-title">
          {{ $t('product.hero.bestPriceTitle') }}
        </h2>
        <div class="product-hero__price">
          <span class="product-hero__price-value" itemprop="lowPrice">
            {{ bestPriceLabel }}
          </span>
          <span class="product-hero__price-currency">
            {{ product.offers?.bestPrice?.currency ?? '€' }}
          </span>
        </div>
        <p v-if="product.offers?.bestPrice?.datasourceName" class="product-hero__price-source">
          {{ $t('product.hero.priceFrom', { source: product.offers.bestPrice.datasourceName }) }}
        </p>
        <div v-if="product.offers?.bestPrice?.compensation" class="product-hero__price-meta">
          <v-chip
            size="small"
            color="success"
            variant="tonal"
            class="product-hero__price-chip"
          >
            {{ $t('product.hero.compensation', { amount: compensationLabel }) }}
          </v-chip>
        </div>
        <div class="product-hero__price-actions">
          <v-btn
            color="primary"
            :href="product.offers?.bestPrice?.url ?? '#prix'"
            :target="product.offers?.bestPrice?.url ? '_blank' : undefined"
            rel="nofollow noopener"
            variant="flat"
          >
            {{ $t('product.hero.viewOffers') }}
          </v-btn>
        </div>
      </div>
    </aside>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, shallowRef, watch, type Component, type ComponentPublicInstance, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import type { ProductDto } from '~~/shared/api-client'

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
})

const pictureSwipeComponent = shallowRef<Component | null>(null)
const pictureSwipeRef = ref<ComponentPublicInstance<{ pswp?: unknown }> | null>(null)
const pictureSwipeContainer = ref<HTMLElement | null>(null)

const { n, t } = useI18n()
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

const title = computed(() => props.product.names?.h1Title ?? props.product.identity?.bestName ?? props.product.base?.bestName ?? '')

const bestPrice = computed(() => props.product.offers?.bestPrice ?? null)

const bestPriceLabel = computed(() => {
  if (!bestPrice.value?.price) {
    return '—'
  }

  return n(bestPrice.value.price, {
    style: 'currency',
    currency: bestPrice.value.currency ?? 'EUR',
    maximumFractionDigits: 0,
  })
})

const compensationLabel = computed(() => {
  if (!bestPrice.value?.compensation) {
    return null
  }

  return n(bestPrice.value.compensation, {
    style: 'currency',
    currency: bestPrice.value.currency ?? 'EUR',
    maximumFractionDigits: 2,
  })
})

const offersCountLabel = computed(() => {
  const count = props.product.offers?.offersCount ?? 0
  return n(count)
})

const impactScore = computed(() => {
  const ecoscore = props.product.base?.ecoscoreValue
  if (typeof ecoscore === 'number') {
    return ecoscore
  }

  const relative = props.product.scores?.ecoscore?.relativeValue
  return typeof relative === 'number' ? relative : null
})

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
    const caption = image.datasourceName ?? title.value
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

    const caption = video.datasourceName ?? title.value
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

  const label = media.caption || media.alt || title.value
  return media.type === 'video'
    ? t('product.hero.openGalleryVideo', { label })
    : t('product.hero.openGalleryImage', { label })
})

const thumbnailAriaLabel = (item: ProductGalleryItem, index: number) => {
  const label = item.caption || item.alt || title.value
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

const pictureSwipeItems = computed(() =>
  galleryItems.value.map((item) => {
    const caption = item.caption || title.value
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

const pictureSwipeOptions = computed<Record<string, unknown>>(() => {
  const base: Record<string, unknown> = {
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

const openGallery = (index: number) => {
  if (!import.meta.client || !galleryItems.value.length) {
    return
  }

  const safeIndex = Math.min(Math.max(index, 0), galleryItems.value.length - 1)
  activeMediaIndex.value = safeIndex

  const componentInstance = pictureSwipeRef.value as ComponentPublicInstance & { $el?: HTMLElement }
  const rootElement = (componentInstance?.$el ?? pictureSwipeContainer.value) as HTMLElement | undefined
  const anchors = rootElement?.querySelectorAll<HTMLAnchorElement>('figure.gallery-thumbnail a')
  const target = anchors?.[safeIndex]

  if (target) {
    target.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    window.setTimeout(bindLightboxListeners, 150)
  }
}

onMounted(async () => {
  if (!import.meta.client) {
    return
  }

  try {
    const [{ default: VuePictureSwipe }] = await Promise.all([import('vue3-picture-swipe')])
    pictureSwipeComponent.value = VuePictureSwipe
  } catch (error) {
    console.error('Failed to load gallery', error)
  }
})
</script>

<style scoped>
.product-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr) minmax(0, 0.8fr);
  gap: 2.5rem;
  padding: 2.5rem;
  border-radius: 32px;
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-ice-050), 0.9), rgba(var(--v-theme-surface-glass), 0.95));
  box-shadow: 0 32px 70px rgba(15, 23, 42, 0.08);
}

.product-hero__gallery {
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

.product-gallery__caption {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 0.75rem 1rem;
  background: linear-gradient(0deg, rgba(15, 23, 42, 0.75), rgba(15, 23, 42, 0));
  color: #fff;
  font-weight: 600;
  font-size: 0.95rem;
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
}

.product-hero__details {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.product-hero__eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-weight: 600;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.8);
}

.product-hero__title {
  font-size: clamp(2rem, 2.8vw, 3rem);
  font-weight: 700;
  line-height: 1.1;
  margin: 0;
}

.product-hero__subtitle {
  font-size: 1.1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
}

.product-hero__impact {
  display: inline-flex;
}

.product-hero__origin {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__flag {
  border-radius: 4px;
  width: 32px;
  height: 24px;
  object-fit: cover;
}

.product-hero__facts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 1rem;
  margin-top: 0.5rem;
}

.product-hero__fact {
  background: rgba(var(--v-theme-surface-glass-strong), 0.6);
  border-radius: 14px;
  padding: 0.875rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.product-hero__fact-label {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.product-hero__fact-value {
  font-weight: 700;
  font-size: 1rem;
}

.product-hero__pricing {
  align-self: stretch;
}

.product-hero__pricing-card {
  border-radius: 24px;
  background: rgba(var(--v-theme-surface-glass-strong), 0.9);
  padding: 1.75rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.1);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-hero__pricing-title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
}

.product-hero__price {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.product-hero__price-value {
  font-size: clamp(2rem, 3.4vw, 2.6rem);
  font-weight: 700;
}

.product-hero__price-currency {
  font-size: 1.1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.8);
}

.product-hero__price-source {
  font-size: 0.95rem;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.product-hero__price-actions {
  margin-top: 0.5rem;
}

@media (max-width: 1280px) {
  .product-hero {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: auto auto;
  }

  .product-hero__pricing {
    grid-column: 1 / -1;
  }

  .product-gallery__stage {
    min-height: 320px;
  }
}

@media (max-width: 960px) {
  .product-hero {
    grid-template-columns: 1fr;
    padding: 1.5rem;
    gap: 1.5rem;
  }

  .product-hero__gallery {
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
