import { describe, expect, it } from 'vitest'
import {
  deduplicateSortItemsByTitle,
  type SortFieldItem,
} from './_sort-options-normalizer'

describe('deduplicateSortItemsByTitle', () => {
  it('deduplicates sort options sharing the same visible title', () => {
    const items: SortFieldItem[] = [
      {
        value: 'attributes.indexedAttributes.HDR_A.numericValue',
        title: 'Consommation électrique (HDR)',
      },
      {
        value: 'attributes.indexedAttributes.HDR_B.numericValue',
        title: '  consommation   electrique (hdr) ',
      },
      {
        value: 'price.minPrice.price',
        title: 'Prix',
      },
    ]

    const deduplicated = deduplicateSortItemsByTitle(items)

    expect(deduplicated).toEqual([
      {
        value: 'attributes.indexedAttributes.HDR_A.numericValue',
        title: 'Consommation électrique (HDR)',
      },
      {
        value: 'price.minPrice.price',
        title: 'Prix',
      },
    ])
  })
})
