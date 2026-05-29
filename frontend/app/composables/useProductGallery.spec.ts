import { describe, expect, it } from 'vitest'
import type { ProductDto } from '~~/shared/api-client'
import { useProductGallery } from './useProductGallery'

describe('useProductGallery', () => {
  it('normalizes already transformed ipx URLs before rendering', () => {
    const product = {
      resources: {
        images: [
          {
            url: '/_ipx/f_webp&s_1200x900/images/product.png',
            originalUrl: '/images/product.png',
          },
        ],
      },
    } as ProductDto

    const { galleryItems } = useProductGallery(product, 'Product')

    expect(galleryItems.value[0].previewUrl).toBe('/images/product.png')
    expect(galleryItems.value[0].thumbnailUrl).toBe('/images/product.png')
    expect(galleryItems.value[0].previewUrl).not.toContain('/_ipx/w_1200/_ipx/')
    expect(galleryItems.value[0].previewUrl).not.toContain('/_ipx/_ipx/')
  })

  it('normalizes legacy missing-image icon paths to the public fallback image', () => {
    const product = {
      resources: {
        images: [
          {
            url: '/icons/no-image.png',
            originalUrl: '/icons/no-image.png',
          },
        ],
      },
    } as ProductDto

    const { galleryItems } = useProductGallery(product, 'Product')

    expect(galleryItems.value[0].originalUrl).toBe('/images/no-image.png')
  })
})
