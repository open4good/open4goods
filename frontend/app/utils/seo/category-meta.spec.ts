import { describe, expect, it } from 'vitest'
import {
  clampMetaDescription,
  resolveCategoryEcoscoreSeoMeta,
  resolveCategorySeoMeta,
} from './category-meta'

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

describe('meta description length', () => {
  it('clamps long category descriptions to 155 characters', () => {
    const meta = resolveCategorySeoMeta({
      siteName: 'Nudger',
      category: { verticalHomeDescription: 'mot '.repeat(90) },
    })

    expect(meta.description.length).toBeLessThanOrEqual(155)
    expect(meta.description.endsWith('…')).toBe(true)
  })

  it('keeps short descriptions untouched', () => {
    expect(clampMetaDescription('Courte description.')).toBe(
      'Courte description.'
    )
  })

  it('gives the ecoscore page its own title and clamped description', () => {
    const category = resolveCategorySeoMeta({
      siteName: 'Nudger',
      category: {
        verticalMetaTitle: 'Comparatif téléviseurs 2026 | Nudger',
        verticalMetaDescription: 'Description de la catégorie.',
      },
    })
    const ecoscore = resolveCategoryEcoscoreSeoMeta({
      title: 'Impact Score Téléviseurs : critères et méthode',
      description: 'x '.repeat(200),
    })

    expect(ecoscore.title).not.toBe(category.title)
    expect(ecoscore.description).not.toBe(category.description)
    expect(ecoscore.description.length).toBeLessThanOrEqual(155)
    expect(ecoscore.ogTitle).toBe(ecoscore.title)
  })
})
