import { computed, type Ref } from 'vue'
import type { ProductDto } from '~~/shared/api-client'

export interface ProductGalleryItem {
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
  group?: string | null
  videoUrl?: string
  posterUrl?: string
}

const DEFAULT_IMAGE_WIDTH = 1600
const DEFAULT_IMAGE_HEIGHT = 1200
const DEFAULT_THUMBNAIL_SIZE = 200
const DEFAULT_VIDEO_WIDTH = 1280
const DEFAULT_VIDEO_HEIGHT = 720
const LOCAL_FALLBACK_IMAGE_SRC = '/images/no-image.png'

export const useProductGallery = (
  product: Ref<ProductDto> | ProductDto,
  title?: string
) => {
  // Handle both ref and raw object
  const productData = computed(() => {
    return 'value' in product ? product.value : product
  })

  const normalizeImageSource = (src: string | null | undefined): string => {
    if (!src) return ''

    const trimmed = src.trim()
    if (!trimmed.length) return ''

    if (trimmed.includes('/_ipx/')) {
      const nestedPublicAssetMatch = trimmed.match(/\/(images\/[^?#]+)$/)
      if (nestedPublicAssetMatch?.[1]) {
        return `/${nestedPublicAssetMatch[1]}`
      }

      return trimmed
    }

    if (trimmed === '/icons/no-image.png') {
      return LOCAL_FALLBACK_IMAGE_SRC
    }

    return trimmed
  }

  const fallbackDimension = (
    value: number | null | undefined,
    fallback: number
  ) => (typeof value === 'number' && value > 0 ? value : fallback)

  const coverImageRaw = computed(
    () =>
      productData.value.resources?.coverImagePath ??
      productData.value.resources?.externalCover ??
      productData.value.base?.coverImagePath ??
      null
  )

  const galleryItems = computed<ProductGalleryItem[]>(() => {
    const images = productData.value.resources?.images ?? []
    const videos = productData.value.resources?.videos ?? []
    const fallbackTitle = title ?? productData.value.identity?.name ?? ''

    const fallbackPoster =
      coverImageRaw.value ?? images[0]?.url ?? images[0]?.originalUrl ?? ''

    const imageItems: ProductGalleryItem[] = []
    const videoItems: ProductGalleryItem[] = []

    const hasCoverInImages = images.some(img => {
      const u = normalizeImageSource(img.originalUrl ?? img.url)
      return u && u === coverImageRaw.value
    })

    if (coverImageRaw.value && !hasCoverInImages) {
      const source = normalizeImageSource(coverImageRaw.value)

      imageItems.push({
        id: `image-cover-fallback-${source}`,
        type: 'image',
        originalUrl: source,
        previewUrl: source,
        thumbnailUrl: source,
        thumbnailWidth: DEFAULT_THUMBNAIL_SIZE,
        thumbnailHeight: DEFAULT_THUMBNAIL_SIZE,
        width: DEFAULT_IMAGE_WIDTH,
        height: DEFAULT_IMAGE_HEIGHT,
        alt: fallbackTitle,
        caption: fallbackTitle,
        group: 'cover',
        posterUrl: source,
      })
    }

    images.forEach(image => {
      const original = normalizeImageSource(image.originalUrl ?? image.url)
      if (!original) return

      const source = normalizeImageSource(image.url) || original
      const caption = image.datasourceName ?? fallbackTitle
      const width = fallbackDimension(image.width, DEFAULT_IMAGE_WIDTH)
      const height = fallbackDimension(image.height, DEFAULT_IMAGE_HEIGHT)

      imageItems.push({
        id: `image-${image.cacheKey ?? original}`,
        type: 'image',
        originalUrl: original,
        previewUrl: source,
        thumbnailUrl: source,
        thumbnailWidth: DEFAULT_THUMBNAIL_SIZE,
        thumbnailHeight: DEFAULT_THUMBNAIL_SIZE,
        width,
        height,
        alt: image.fileName ?? caption,
        caption,
        group: image.group,
        posterUrl: source,
      })
    })

    videos.forEach(video => {
      const url = video.url ?? ''
      if (!url) return

      const caption = video.datasourceName ?? fallbackTitle
      const poster = normalizeImageSource(fallbackPoster || coverImageRaw.value)

      videoItems.push({
        id: `video-${video.cacheKey ?? url}`,
        type: 'video',
        originalUrl: poster || url,
        previewUrl: poster || url,
        thumbnailUrl: poster || url,
        thumbnailWidth: DEFAULT_THUMBNAIL_SIZE,
        thumbnailHeight: DEFAULT_THUMBNAIL_SIZE,
        width: DEFAULT_VIDEO_WIDTH,
        height: DEFAULT_VIDEO_HEIGHT,
        alt: video.fileName ?? caption,
        caption,
        group: video.group,
        videoUrl: url,
        posterUrl: poster || url,
      })
    })

    const orderedItems = [...videoItems, ...imageItems]
      .map((item, index) => ({ item, index }))
      // .filter(({ item }) => Boolean(item.originalUrl)) // originalUrl always set above
      .sort((a, b) => {
        const normaliseGroup = (value: unknown) =>
          value == null ? '' : String(value).toLowerCase()

        const groupA = normaliseGroup(a.item.group)
        const groupB = normaliseGroup(b.item.group)

        if (groupA === groupB) {
          return a.index - b.index
        }

        return groupA.localeCompare(groupB)
      })
      .map(({ item }) => item)

    return orderedItems
  })

  const heroFallbackImage = computed(() => {
    const fallback =
      coverImageRaw.value ?? galleryItems.value[0]?.previewUrl ?? null
    if (!fallback) return null

    return normalizeImageSource(fallback)
  })

  return {
    galleryItems,
    heroFallbackImage,
  }
}
