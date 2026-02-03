import { describe, expect, it, vi } from 'vitest'
import type { ProductDto, VerticalConfigFullDto } from '~~/shared/api-client'
import { createCompareService } from './CompareService'
import type { CompareProductEntry } from './CompareService'

describe('CompareService', () => {
  const buildProduct = (overrides: Partial<ProductDto> = {}): ProductDto =>
    ({
      gtin: 1234567890123,
      base: {
        vertical: 'VERTICAL',
        gtinInfo: {
          countryName: 'France',
          countryFlagUrl: 'https://example.com/fr.svg',
        },
      },
      identity: {
        brand: 'Brand',
        model: 'Model X',
        bestName: 'Brand Model X',
      },
      resources: {
        coverImagePath: 'https://example.com/cover.jpg',
      },
      aiReview: {
        review: {
          description: '<p>Great <strong>product</strong></p>',
          pros: ['<b>Efficient</b>'],
          cons: ['<i>Pricey</i>'],
        },
      },
      scores: overrides.scores,
      ...overrides,
    }) as ProductDto

  const minimalEntry = (verticalId: string | null): CompareProductEntry => ({
    gtin: '1',
    product: buildProduct(),
    verticalId,
    title: 'Stub',
    brand: null,
    model: null,
    coverImage: null,
    impactScore: null,
    review: { description: null, pros: [], cons: [] },
    country: null,
  })

  it('loads and normalises product entries', async () => {
    const product = buildProduct()
    const fetchProduct = vi.fn().mockResolvedValue(product)
    const service = createCompareService({ fetchProduct })

    const [entry] = await service.loadProducts(['1234567890123'])
    expect(fetchProduct).toHaveBeenCalledWith('1234567890123')
    expect(entry.gtin).toBe('1234567890123')
    expect(entry.title).toBe('Brand Model X')
    expect(entry.review.description).toBe('Great product')
    expect(entry.review.pros[0]).toContain('<b>Efficient</b>')
    expect(entry.country?.name).toBe('France')
    expect(entry.impactScore).toBeNull()
  })

  it('detects mixed verticals', async () => {
    const service = createCompareService({ fetchProduct: vi.fn() })
    const mixed = service.hasMixedVerticals([
      minimalEntry('A'),
      minimalEntry('B'),
    ])
    expect(mixed).toBe(true)
    const uniform = service.hasMixedVerticals([
      minimalEntry('A'),
      minimalEntry('A'),
    ])
    expect(uniform).toBe(false)
  })

  it('returns null when vertical id missing', async () => {
    const fetchVertical = vi.fn()
    const service = createCompareService({ fetchVertical })
    await expect(service.loadVertical(null)).resolves.toBeNull()
    expect(fetchVertical).not.toHaveBeenCalled()
  })

  it('loads vertical configuration when id provided', async () => {
    const vertical = { id: 'VERTICAL' } as VerticalConfigFullDto
    const fetchVertical = vi.fn().mockResolvedValue(vertical)
    const service = createCompareService({ fetchVertical })

    await expect(service.loadVertical('VERTICAL')).resolves.toBe(vertical)
    expect(fetchVertical).toHaveBeenCalledWith('VERTICAL')
  })

  it('handles partial failures during product load', async () => {
    const fetchProduct = vi.fn().mockImplementation(async gtin => {
      if (gtin === 'failed') {
        throw new Error('Not found')
      }
      return buildProduct({ gtin: Number(gtin) })
    })
    const service = createCompareService({ fetchProduct })

    const entries = await service.loadProducts(['123', 'failed', '456'])
    expect(entries).toHaveLength(2)
    expect(entries.map(e => e.gtin)).toEqual(['123', '456'])
    expect(fetchProduct).toHaveBeenCalledTimes(3)
  })
})
