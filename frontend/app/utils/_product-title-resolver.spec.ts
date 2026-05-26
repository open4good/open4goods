import { describe, expect, it } from 'vitest'
import {
  resolveProductCardName,
  resolveProductLongName,
  resolveProductShortName,
} from './_product-title-resolver'
import type { ProductDto } from '~~/shared/api-client'

describe('resolveProductTitle', () => {
  it('prefers the H1 title over category-like long names for page titles', () => {
    const product: ProductDto = {
      names: {
        h1Title: 'TV samsung 32 led gu32t5379cd',
        longName: 'Téléviseurs et écrans TV',
      },
      identity: {
        brand: 'Samsung',
        model: 'GU32T5379CD',
      },
    }

    const result = resolveProductLongName(product, 'fr-FR')

    expect(result).toBe('TV SAMSUNG 32 led gu32t5379cd')
  })

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

  it('prefers card title over category-like short names for cards', () => {
    const product: ProductDto = {
      names: {
        cardTitle: 'Samsung GU32T5379CD',
        shortName: 'Téléviseurs',
      },
      identity: {
        brand: 'Samsung',
        model: 'GU32T5379CD',
      },
    }

    const result = resolveProductCardName(product, 'fr-FR')

    expect(result).toBe('Samsung GU32T5379CD')
  })
})
