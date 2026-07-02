import { describe, it, expect } from 'vitest'
import { buildProductJsonLdGraph } from './product-jsonld'
import type { ProductJsonLdInput } from './product-jsonld'

describe('product-jsonld', () => {
  const mockProduct = {
    gtin: 1234567890123,
    slug: 'test-product',
    names: { seoName: 'Test Product' },
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
    width?: { '@type': string; value: number; unitCode: string }
    height?: { '@type': string; value: number; unitCode: string }
    depth?: { '@type': string; value: number; unitCode: string }
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

  it('sets used and refurbished itemCondition values', () => {
    const graph = buildProductJsonLdGraph({
      ...(mockInput as unknown as ProductJsonLdInput),
      product: {
        ...mockProduct,
        offers: {
          offersCount: 2,
          offersByCondition: {
            USED: [
              {
                url: '/offer/used',
                price: 80,
                currency: 'EUR',
                condition: 'USED',
              },
            ],
            REFURBISHED: [
              {
                url: '/offer/refurbished',
                price: 90,
                currency: 'EUR',
                condition: 'REFURBISHED',
              },
            ],
          },
        },
      } as unknown as ProductJsonLdInput['product'],
    })
    const productNode = (
      graph?.['@graph'] as unknown as TestProductNode[]
    )?.find(n => n['@type'] === 'Product')

    expect(
      productNode?.offers.offers.map(offer => offer.itemCondition)
    ).toEqual([
      'https://schema.org/UsedCondition',
      'https://schema.org/RefurbishedCondition',
    ])
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

  it('uses localized property labels when provided', () => {
    const graph = buildProductJsonLdGraph({
      ...(mockInput as unknown as ProductJsonLdInput),
      product: {
        ...mockProduct,
        attributes: {
          referentialAttributes: {
            WARRANTY: '2',
          },
        },
      } as unknown as ProductJsonLdInput['product'],
      labels: {
        impactScore: 'Impact score',
        repairabilityIndex: 'Repairability index',
        sparePartsAvailability: 'Spare parts availability',
        softwareUpdates: 'Software updates',
        warranty: 'Warranty',
        screenSize: 'Screen size',
        displayTechnology: 'Display technology',
        resolution: 'Resolution',
        frequency: 'Frequency',
        hdmiPorts: 'HDMI ports',
        usbPorts: 'USB ports',
        wifi: 'Wi-Fi',
        wifiStandards: 'Wi-Fi standards',
        bluetooth: 'Bluetooth',
        bluetoothVersion: 'Bluetooth version',
        operatingSystem: 'OS / Platform',
        releaseYear: 'Release year',
        color: 'Color',
        energySdr: 'Energy label (SDR)',
        energyHdr: 'Energy label (HDR)',
      },
    })
    const productNode = (
      graph?.['@graph'] as unknown as TestProductNode[]
    )?.find(n => n['@type'] === 'Product')

    expect(
      productNode?.additionalProperty?.some(
        property => property.name === 'Warranty' && property.value === '2'
      )
    ).toBe(true)
  })

  it('normalizes compact metric dimensions before emitting schema units', () => {
    const graph = buildProductJsonLdGraph({
      ...(mockInput as unknown as ProductJsonLdInput),
      product: {
        ...mockProduct,
        attributes: {
          indexedAttributes: {
            WIDTH: { numericValue: 0.751 },
            HEIGHT: { numericValue: 1.62 },
            DEPTH: { numericValue: 0.0865 },
          },
        },
      } as unknown as ProductJsonLdInput['product'],
    })
    const productNode = (
      graph?.['@graph'] as unknown as TestProductNode[]
    )?.find(n => n['@type'] === 'Product')

    expect(productNode?.width).toEqual({
      '@type': 'QuantitativeValue',
      value: 75.1,
      unitCode: 'MMT',
    })
    expect(productNode?.height).toEqual({
      '@type': 'QuantitativeValue',
      value: 162,
      unitCode: 'MMT',
    })
    expect(productNode?.depth).toEqual({
      '@type': 'QuantitativeValue',
      value: 8.65,
      unitCode: 'MMT',
    })
  })

  it('emits shippingDetails when shippingCost is present on an offer', () => {
    const graph = buildProductJsonLdGraph({
      ...(mockInput as unknown as ProductJsonLdInput),
      product: {
        ...mockProduct,
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
                shippingCost: 0,
                shippingTimeDays: 3,
              },
            ],
          },
        },
      } as unknown as ProductJsonLdInput['product'],
    })
    const productNode = (
      graph?.['@graph'] as unknown as Array<{
        '@type': string
        offers: {
          offers: Array<{
            shippingDetails?: {
              '@type': string
              shippingRate: { value: number }
              deliveryTime?: { transitTime: { minValue: number } }
            }
          }>
        }
      }>
    )?.find(n => n['@type'] === 'Product')

    const offer = productNode!.offers.offers[0]
    expect(offer.shippingDetails?.['@type']).toBe('OfferShippingDetails')
    expect(offer.shippingDetails?.shippingRate?.value).toBe(0)
    expect(offer.shippingDetails?.deliveryTime?.transitTime?.minValue).toBe(3)
  })

  it('omits shippingDetails when shippingCost is absent', () => {
    const graph = buildProductJsonLdGraph(
      mockInput as unknown as ProductJsonLdInput
    )
    const productNode = (
      graph?.['@graph'] as unknown as Array<{
        '@type': string
        offers: { offers: Array<{ shippingDetails?: unknown }> }
      }>
    )?.find(n => n['@type'] === 'Product')

    expect(productNode!.offers.offers[0].shippingDetails).toBeUndefined()
  })

  it('emits countryOfOrigin when gtinInfo.countryName is present', () => {
    const graph = buildProductJsonLdGraph({
      ...(mockInput as unknown as ProductJsonLdInput),
      product: {
        ...mockProduct,
        base: { gtinInfo: { countryCode: 'CN', countryName: 'Chine' } },
      } as unknown as ProductJsonLdInput['product'],
    })
    const productNode = (
      graph?.['@graph'] as unknown as Array<{
        '@type': string
        countryOfOrigin?: { '@type': string; name: string }
      }>
    )?.find(n => n['@type'] === 'Product')

    expect(productNode?.countryOfOrigin).toEqual({
      '@type': 'Country',
      name: 'Chine',
    })
  })

  it('omits countryOfOrigin when gtinInfo.countryName is absent', () => {
    const graph = buildProductJsonLdGraph(
      mockInput as unknown as ProductJsonLdInput
    )
    const productNode = (
      graph?.['@graph'] as unknown as Array<{
        '@type': string
        countryOfOrigin?: unknown
      }>
    )?.find(n => n['@type'] === 'Product')

    expect(productNode?.countryOfOrigin).toBeUndefined()
  })

  it('filters out breadcrumbs with empty titles', () => {
    const graph = buildProductJsonLdGraph({
      ...(mockInput as unknown as ProductJsonLdInput),
      breadcrumbs: [
        { title: 'Home', link: '/' },
        { title: '', link: '/empty-title' },
        { title: 'Category', link: '/category' },
      ],
    })
    const breadcrumbNode = (
      graph?.['@graph'] as unknown as Array<{
        '@type': string
        itemListElement: Array<{ name: string }>
      }>
    )?.find(n => n['@type'] === 'BreadcrumbList')

    expect(breadcrumbNode!.itemListElement).toHaveLength(2)
    expect(breadcrumbNode!.itemListElement.map(e => e.name)).toEqual([
      'Home',
      'Category',
    ])
  })

  it('uses the resolved category label instead of the product SEO name', () => {
    const graph = buildProductJsonLdGraph({
      ...(mockInput as unknown as ProductJsonLdInput),
      category: 'Televisions',
    })
    const productNode = (
      graph?.['@graph'] as unknown as Array<{
        '@type': string
        category?: string
      }>
    )?.find(n => n['@type'] === 'Product')

    expect(productNode?.category).toBe('Televisions')
  })

  it('derives OutOfStock availability from quantityInStock when present', () => {
    const graph = buildProductJsonLdGraph({
      ...(mockInput as unknown as ProductJsonLdInput),
      product: {
        ...mockProduct,
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
                quantityInStock: 0,
              },
            ],
          },
        },
      } as unknown as ProductJsonLdInput['product'],
    })
    const productNode = (
      graph?.['@graph'] as unknown as Array<{
        '@type': string
        offers: { offers: Array<{ availability: string }> }
      }>
    )?.find(n => n['@type'] === 'Product')

    expect(productNode!.offers.offers[0].availability).toBe(
      'https://schema.org/OutOfStock'
    )
  })

  it('defaults to InStock when quantityInStock is not provided', () => {
    const graph = buildProductJsonLdGraph(
      mockInput as unknown as ProductJsonLdInput
    )
    const productNode = (
      graph?.['@graph'] as unknown as Array<{
        '@type': string
        offers: { offers: Array<{ availability: string }> }
      }>
    )?.find(n => n['@type'] === 'Product')

    expect(productNode!.offers.offers[0].availability).toBe(
      'https://schema.org/InStock'
    )
  })

  it('derives priceValidUntil from the offer timeStamp when present', () => {
    const timeStamp = Date.parse('2026-01-01T00:00:00Z')
    const graph = buildProductJsonLdGraph({
      ...(mockInput as unknown as ProductJsonLdInput),
      product: {
        ...mockProduct,
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
                timeStamp,
              },
            ],
          },
        },
      } as unknown as ProductJsonLdInput['product'],
    })
    const productNode = (
      graph?.['@graph'] as unknown as TestProductNode[]
    )?.find(n => n['@type'] === 'Product')

    expect(productNode!.offers.offers[0].priceValidUntil).toBe('2026-01-11')
  })

  it('emits a Review node with a normalized rating when impactScore is present', () => {
    const graph = buildProductJsonLdGraph(
      mockInput as unknown as ProductJsonLdInput
    )
    const productNode = (
      graph?.['@graph'] as unknown as Array<{
        '@type': string
        review?: {
          '@type': string
          reviewRating: {
            '@type': string
            ratingValue: number
            bestRating: number
            worstRating: number
          }
          author: { '@type': string; name: string; url: string }
          reviewBody?: string
        }
      }>
    )?.find(n => n['@type'] === 'Product')

    // mockInput.impactScore is 18.5 (on a /20 scale) -> 18.5 / 4 = 4.6
    expect(productNode?.review).toMatchObject({
      '@type': 'Review',
      reviewRating: {
        '@type': 'Rating',
        ratingValue: 4.6,
        bestRating: 5,
        worstRating: 0,
      },
      author: { '@type': 'Organization', name: 'Nudger', url: 'https://nudger.fr' },
      reviewBody: 'Best product ever',
    })
  })

  it('omits the Review node when impactScore is missing', () => {
    const graph = buildProductJsonLdGraph({
      ...(mockInput as unknown as ProductJsonLdInput),
      impactScore: null,
    })
    const productNode = (
      graph?.['@graph'] as unknown as Array<{
        '@type': string
        review?: unknown
      }>
    )?.find(n => n['@type'] === 'Product')

    expect(productNode?.review).toBeUndefined()
  })

  it('never emits two breadcrumb ListItems sharing the same item URL', () => {
    const graph = buildProductJsonLdGraph({
      ...(mockInput as unknown as ProductJsonLdInput),
      breadcrumbs: [
        { title: 'Home', link: '/' },
        { title: 'Category', link: '/category' },
        { title: 'Brand', link: '/category' },
      ],
    })
    const breadcrumbNode = (
      graph?.['@graph'] as unknown as Array<{
        '@type': string
        itemListElement: Array<{ position: number; name: string; item: string }>
      }>
    )?.find(n => n['@type'] === 'BreadcrumbList')

    const urls = breadcrumbNode!.itemListElement.map(item => item.item)
    expect(new Set(urls).size).toBe(urls.length)
    expect(breadcrumbNode!.itemListElement).toHaveLength(2)
    expect(breadcrumbNode!.itemListElement.map(e => e.name)).toEqual([
      'Home',
      'Category',
    ])
    expect(breadcrumbNode!.itemListElement.map(e => e.position)).toEqual([
      1, 2,
    ])
  })

  it('emits VideoObject only when required video fields exist', () => {
    const graph = buildProductJsonLdGraph({
      ...(mockInput as unknown as ProductJsonLdInput),
      product: {
        ...mockProduct,
        resources: {
          videos: [
            {
              name: 'Product demo',
              thumbnailUrl: '/images/video-thumb.webp',
              uploadDate: '2026-05-29T12:00:00Z',
              contentUrl: '/videos/demo.mp4',
              duration: 'PT1M',
            },
            {
              name: 'Missing thumbnail',
              uploadDate: '2026-05-29T12:00:00Z',
              contentUrl: '/videos/missing.mp4',
            },
          ],
        },
      } as unknown as ProductJsonLdInput['product'],
    })

    const videoNodes = (
      graph?.['@graph'] as unknown as Array<{
        '@type': string
        name?: string
        thumbnailUrl?: string[]
        contentUrl?: string
      }>
    ).filter(n => n['@type'] === 'VideoObject')

    expect(videoNodes).toHaveLength(1)
    expect(videoNodes[0]).toMatchObject({
      name: 'Product demo',
      thumbnailUrl: ['https://nudger.fr/images/video-thumb.webp'],
      contentUrl: 'https://nudger.fr/videos/demo.mp4',
    })
  })
})
