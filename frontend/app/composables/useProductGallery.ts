import { computed, type Ref } from 'vue'
import { useImage } from '#imports'
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

type ImageModifiers = Parameters<ReturnType<typeof useImage>>[1]

export const useProductGallery = (
  product: Ref<ProductDto> | ProductDto,
  title?: string
) => {
  const nuxtImage = useImage()

  // Handle both ref and raw object
  const productData = computed(() => {
    return 'value' in product ? product.value : product
  })

  // Helper to resolve modifiers
  const resolveImageUrl = (
    src: string | null | undefined,
    modifiers?: ImageModifiers
  ) => {
    if (!src) return ''
    try {
      return nuxtImage(src, modifiers)
    } catch {
      return src
    }
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

    images.forEach(image => {
      const original = image.originalUrl ?? image.url ?? ''
      if (!original) return

      const source = image.url ?? original
      const caption = image.datasourceName ?? fallbackTitle
      const width = fallbackDimension(image.width, DEFAULT_IMAGE_WIDTH)
      const height = fallbackDimension(image.height, DEFAULT_IMAGE_HEIGHT)

      const preview =
        resolveImageUrl(source, {
          width: 1200,
          height: 900,
          fit: 'cover',
          format: 'webp',
        }) || source

      const thumbnail =
        resolveImageUrl(source, {
          width: DEFAULT_THUMBNAIL_SIZE,
          height: DEFAULT_THUMBNAIL_SIZE,
          fit: 'cover',
          format: 'webp',
        }) || preview

      imageItems.push({
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
        group: image.group,
        posterUrl: preview,
      })
    })

    videos.forEach(video => {
      const url = video.url ?? ''
      if (!url) return

      const caption = video.datasourceName ?? fallbackTitle
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

      videoItems.push({
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
        group: video.group,
        videoUrl: url,
        posterUrl: poster || thumbnail || url,
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

    return (
      resolveImageUrl(fallback, {
        width: 960,
        height: 720,
        fit: 'cover',
        format: 'webp',
      }) || fallback
    )
  })

  return {
    galleryItems,
    heroFallbackImage,
  }
}
