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
    punchline: 'Best product ever',
    impactScore: 18.5,
  }

  interface TestProductNode {
    '@type': string
    description?: string
    offers: {
      offers: {
        url: string
        priceValidUntil: string
        itemCondition: string
      }[]
      offerCount: number
    }
    additionalProperty?: Array<{ name: string; value?: string | number }>
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

  it('sets priceValidUntil to 10 days in the future', () => {
    const graph = buildProductJsonLdGraph(
      mockInput as unknown as ProductJsonLdInput
    )
    const productNode = (
      graph?.['@graph'] as unknown as TestProductNode[]
    )?.find(n => n['@type'] === 'Product')

    const offer = productNode!.offers.offers[0]

    const expectedDate = new Date()
    expectedDate.setDate(expectedDate.getDate() + 10)
    const expectedDateString = expectedDate.toISOString().split('T')[0]

    expect(offer.priceValidUntil).toBe(expectedDateString)
  })

  it('sets correct itemCondition for NEW products', () => {
    const graph = buildProductJsonLdGraph(
      mockInput as unknown as ProductJsonLdInput
    )
    const productNode = (
      graph?.['@graph'] as unknown as TestProductNode[]
    )?.find(n => n['@type'] === 'Product')

    const offer = productNode!.offers.offers[0]
    expect(offer.itemCondition).toBe('https://schema.org/NewCondition')
  })

  it('correctly sets offerCount', () => {
    const graph = buildProductJsonLdGraph(
      mockInput as unknown as ProductJsonLdInput
    )
    const productNode = (
      graph?.['@graph'] as unknown as TestProductNode[]
    )?.find(n => n['@type'] === 'Product')

    // mockProduct has offersCount: 1 and offers array length 1
    expect(productNode!.offers.offerCount).toBe(1)
  })

  it('filters out breadcrumb items without links', () => {
    const inputWithMissingLink = {
      ...mockInput,
      breadcrumbs: [
        { title: 'Home', link: '/' },
        { title: 'No Link Item', link: undefined },
        { title: 'Category', link: '/category' },
      ],
    } as unknown as ProductJsonLdInput

    const graph = buildProductJsonLdGraph(inputWithMissingLink)
    const breadcrumbNode = (
      graph?.['@graph'] as unknown as Array<{
        '@type': string
        itemListElement: Array<{ position: number; name: string }>
      }>
    )?.find(n => n['@type'] === 'BreadcrumbList')

    expect(breadcrumbNode).toBeDefined()
    expect(breadcrumbNode!.itemListElement).toHaveLength(2)
    expect(breadcrumbNode!.itemListElement[0].name).toBe('Home')
    expect(breadcrumbNode!.itemListElement[0].position).toBe(1)
    expect(breadcrumbNode!.itemListElement[1].name).toBe('Category')
    expect(breadcrumbNode!.itemListElement[1].position).toBe(2)
  })

  it('appends punchline to description', () => {
    const graph = buildProductJsonLdGraph(
      mockInput as unknown as ProductJsonLdInput
    )
    const productNode = (
      graph?.['@graph'] as unknown as TestProductNode[]
    )?.find(n => n['@type'] === 'Product')

    expect(productNode?.description).toContain('Best product ever')
  })

  it('includes impactScore in additionalProperty', () => {
    const graph = buildProductJsonLdGraph(
      mockInput as unknown as ProductJsonLdInput
    )
    const productNode = (
      graph?.['@graph'] as unknown as TestProductNode[]
    )?.find(n => n['@type'] === 'Product')

    const impactScoreProp = productNode?.additionalProperty?.find(
      p => p.name === 'Nudger Impact Score'
    )
    expect(impactScoreProp).toBeDefined()
    expect(impactScoreProp?.value).toBe(18.5)
  })
})
