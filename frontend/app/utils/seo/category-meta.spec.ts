import { describe, expect, it } from 'vitest'
import { resolveCategorySeoMeta } from './category-meta'

describe('resolveCategorySeoMeta', () => {
  it('prioritises sub-category backend SEO and Open Graph fields', () => {
    const meta = resolveCategorySeoMeta({
      siteName: 'Nudger',
      category: {
        verticalMetaTitle: 'Category SEO title',
        verticalMetaDescription: 'Category SEO description',
        verticalMetaOpenGraphTitle: 'Category OG title',
        verticalMetaOpenGraphDescription: 'Category OG description',
        verticalHomeTitle: 'Category home title',
      },
      subCategory: {
        metaTitle: 'Sub SEO title',
        metaDescription: 'Sub **SEO** description',
        metaOpenGraphTitle: 'Sub OG title',
        metaOpenGraphDescription: 'Sub [OG](https://example.test) description',
        h1Title: 'Sub H1',
      },
    })

    expect(meta).toEqual({
      title: 'Sub SEO title',
      description: 'Sub SEO description',
      ogTitle: 'Sub OG title',
      ogDescription: 'Sub OG description',
    })
  })

  it('falls back to category and site fields when specific metadata is absent', () => {
    const meta = resolveCategorySeoMeta({
      siteName: 'Nudger',
      category: {
        verticalHomeTitle: 'Category home title',
        verticalHomeDescription: 'Category _home_ description',
      },
    })

    expect(meta).toEqual({
      title: 'Category home title',
      description: 'Category home description',
      ogTitle: 'Category home title',
      ogDescription: 'Category home description',
    })
  })
})
