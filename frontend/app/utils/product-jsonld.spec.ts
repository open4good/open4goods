import { describe, it, expect } from 'vitest'
import { buildProductJsonLdGraph } from './product-jsonld'
import type { ProductJsonLdInput } from './product-jsonld'

describe('product-jsonld', () => {
  const mockProduct = {
    gtin: 1234567890123,
    slug: 'test-product',
    names: { singular: 'Test Product' },
    offers: {
      offersCount: 1,
      offersByCondition: {
        NEW: [
          {
            url: '/offer/1',
            price: 100,
            currency: 'EUR',
            condition: 'NEW',
            datasourceName: 'Shop',
          },
        ],
      },
    },
    aiReview: {
      review: {
        summary: 'Great product',
        shortTitle: 'Review',
      },
      createdMs: 1625097600000,
    },
    scores: {
      ecoscore: {
        absolute: { value: 15 },
      },
    },
  } as unknown as Record<string, unknown>

  const mockInput = {
    product: mockProduct,
    productTitle: 'Test Product Title',
    canonicalUrl: 'https://nudger.fr/product/123',
    locale: 'fr-FR',
    breadcrumbs: [
      { title: 'Home', link: '/' },
      { title: 'Category', link: '/category' },
    ],
    site: {
      url: 'https://nudger.fr',
      name: 'Nudger',
    },
    impactScoreOn20: 15,
    review: {
      '@type': 'Review',
      reviewRating: {
        '@type': 'Rating',
        ratingValue: 4.5,
        bestRating: 5,
        worstRating: 0, // Old value, to be fixed by the logic
      },
    },
  }

  interface TestProductNode {
    '@type': string
    offers: {
      offers: { url: string }[]
    }
    review: {
      reviewRating: {
        worstRating: number
      }
    }
    additionalProperty?: Array<{ name: string }>
  }

  it('generates valid JSON-LD graph with absolute URLs', () => {
    const graph = buildProductJsonLdGraph(
      mockInput as unknown as ProductJsonLdInput
    )
    expect(graph).toBeDefined()
    const productNode = (
      graph?.['@graph'] as unknown as TestProductNode[]
    )?.find(n => n['@type'] === 'Product')

    expect(productNode).toBeDefined()
    expect(productNode!.offers.offers[0].url).toBe('https://nudger.fr/offer/1')
  })

  it('enforces worstRating = 1', () => {
    const graph = buildProductJsonLdGraph(
      mockInput as unknown as ProductJsonLdInput
    )
    const productNode = (
      graph?.['@graph'] as unknown as TestProductNode[]
    )?.find(n => n['@type'] === 'Product')

    expect(productNode!.review.reviewRating.worstRating).toBe(1)
  })

  it('removes incomplete additionalProperties', () => {
    const graph = buildProductJsonLdGraph({
      ...mockInput,
      impactScoreOn20: undefined,
    } as unknown as ProductJsonLdInput)
    const productNode = (
      graph?.['@graph'] as unknown as TestProductNode[]
    )?.find(n => n['@type'] === 'Product')

    const props = productNode?.additionalProperty || []
    const scoreProp = props.find(p => p.name === 'Nudger Impact Score')
    expect(scoreProp).toBeUndefined()
  })
})
