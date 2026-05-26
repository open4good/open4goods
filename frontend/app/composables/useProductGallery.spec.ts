import { describe, expect, it, vi } from 'vitest'
import type { ProductDto } from '~~/shared/api-client'
import { useProductGallery } from './useProductGallery'

vi.mock('#imports', () => ({
  useImage: () => (src: string, modifiers?: Record<string, unknown>) => {
    if (!modifiers?.width || src.startsWith('/_ipx/')) {
      return src
    }

    return `/_ipx/w_${modifiers.width}${src}`
  },
}))

describe('useProductGallery', () => {
  it('does not pass already transformed ipx URLs through the image transformer again', () => {
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

    expect(galleryItems.value[0].previewUrl).toContain('/images/product.png')
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
