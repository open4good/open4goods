import { describe, expect, it } from 'vitest'
import type { ProductDto } from '~~/shared/api-client'
import { buildProductImageAlt } from './product-image-alt'

const product = (identity: Record<string, string>) =>
  ({ identity, base: {} }) as unknown as ProductDto

describe('buildProductImageAlt', () => {
  it('combines type, brand, model and view', () => {
    expect(
      buildProductImageAlt(product({ brand: 'Bosch', model: 'WGG244A0FR' }), {
        productType: 'Lave-linge',
        view: 'vue de face',
      })
    ).toBe('Lave-linge Bosch WGG244A0FR, vue de face')
  })

  it('falls back to brand and model, then to the provided fallback', () => {
    expect(buildProductImageAlt(product({ brand: 'Sony', model: 'XM5' }))).toBe(
      'Sony XM5'
    )
    expect(buildProductImageAlt(product({}), { fallback: 'Produit' })).toBe(
      'Produit'
    )
  })
})
