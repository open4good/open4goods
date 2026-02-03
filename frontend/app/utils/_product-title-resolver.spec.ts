import { describe, expect, it } from 'vitest'
import { resolveProductShortName } from './_product-title-resolver'
import type { ProductDto } from '~~/shared/api-client'

describe('resolveProductTitle', () => {
  it('should use longest offer name when no category is associated', () => {
    const product: ProductDto = {
      base: {
        vertical: undefined, // No category
      },
      names: {
        offerNames: new Set([
          'Short Name',
          'A very long offer name that should be picked',
        ]),
        longestOfferName: 'A very long offer name that should be picked',
      },
      identity: {
        brand: 'Brand',
        model: 'Model',
      },
    }

    // Currently this might return "Brand - Model" or empty string if other names are missing
    // We want it to return the longest offer name
    const result = resolveProductShortName(product)

    expect(result).toBe('A very long offer name that should be picked')
  })

  it('should compute longest offer name if explicit field is missing but offerNames set is present', () => {
    const product: ProductDto = {
      base: {
        vertical: undefined,
      },
      names: {
        offerNames: new Set(['Short', 'Longer Name']),
        // longestOfferName missing
      },
      identity: {
        brand: 'Brand',
        model: 'Model',
      },
    }

    const result = resolveProductShortName(product)

    expect(result).toBe('Longer Name')
  })
})
