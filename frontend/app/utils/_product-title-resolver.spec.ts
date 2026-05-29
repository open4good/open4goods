import { describe, expect, it } from 'vitest'
import {
  resolveProductCardName,
  resolveProductLongName,
  resolveProductShortName,
} from './_product-title-resolver'
import type { ProductDto } from '~~/shared/api-client'

describe('resolveProductTitle', () => {
  it('uses pageTitle for page titles', () => {
    const product: ProductDto = {
      names: {
        pageTitle: 'TV samsung 32 led gu32t5379cd',
      },
      identity: {
        brand: 'Samsung',
        model: 'GU32T5379CD',
      },
    }

    expect(resolveProductLongName(product, 'fr-FR')).toBe(
      'TV SAMSUNG 32 led gu32t5379cd'
    )
  })

  it('uses displayName for short names', () => {
    const product: ProductDto = {
      names: {
        displayName: 'Samsung GU32T5379CD',
      },
      identity: {
        brand: 'Samsung',
        model: 'GU32T5379CD',
      },
    }

    expect(resolveProductShortName(product, 'fr-FR')).toBe(
      'Samsung GU32T5379CD'
    )
  })

  it('uses cardName for cards', () => {
    const product: ProductDto = {
      names: {
        cardName: 'Samsung GU32T5379CD',
        displayName: 'Samsung TV 32 pouces',
      },
      identity: {
        brand: 'Samsung',
        model: 'GU32T5379CD',
      },
    }

    expect(resolveProductCardName(product, 'fr-FR')).toBe('Samsung GU32T5379CD')
  })

  it('falls back to identity before brand and model', () => {
    const product: ProductDto = {
      identity: {
        brand: 'Brand',
        model: 'Oven 42',
        bestName: 'Brand Oven 42',
      },
      base: {
        bestName: 'Base Name',
      },
    }

    expect(resolveProductShortName(product, 'fr-FR')).toBe('Brand Oven 42')
  })

  it('does not expose unresolved raw templates', () => {
    const product: ProductDto = {
      names: {
        cardName: '[(${p.brand()})] [(${p.model()})]',
      },
      identity: {
        brand: 'Samsung',
        model: 'GU32T5379CD',
      },
    }

    expect(resolveProductCardName(product, 'fr-FR')).toBe('Samsung GU32T5379CD')
  })
})
