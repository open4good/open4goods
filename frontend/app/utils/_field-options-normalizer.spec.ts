import { describe, expect, it } from 'vitest'
import type { FieldMetadataDto, ProductFieldOptionsResponse } from '~~/shared/api-client'
import {
  deduplicateFieldMetadataList,
  normalizeFieldOptionsResponse,
} from './_field-options-normalizer'

describe('deduplicateFieldMetadataList', () => {
  it('deduplicates duplicated mappings and keeps the best metadata quality', () => {
    const fields: FieldMetadataDto[] = [
      {
        mapping: 'power.hdr',
        title: 'Consommation électrique (HDR)',
        valueType: 'numeric',
      },
      {
        mapping: 'power.hdr',
        title: 'Consommation électrique (HDR)',
        description: 'HDR consumption',
        valueType: 'numeric',
      },
    ]

    const deduplicated = deduplicateFieldMetadataList(fields)

    expect(deduplicated).toHaveLength(1)
    expect(deduplicated[0]?.description).toBe('HDR consumption')
  })

  it('falls back to title-based deduplication when mappings are missing', () => {
    const fields: FieldMetadataDto[] = [
      {
        title: 'Disponibilité pièces détachées',
        valueType: 'numeric',
      },
      {
        title: '  disponibilité   pièces détachées  ',
        description: 'duplicate with spacing',
        valueType: 'numeric',
      },
    ]

    const deduplicated = deduplicateFieldMetadataList(fields)

    expect(deduplicated).toHaveLength(1)
    expect(deduplicated[0]?.description).toBe('duplicate with spacing')
  })
})

describe('normalizeFieldOptionsResponse', () => {
  it('normalizes each section independently', () => {
    const options: ProductFieldOptionsResponse = {
      global: [
        { mapping: 'price.minPrice.price', title: 'Prix', valueType: 'numeric' },
        { mapping: 'price.minPrice.price', title: 'Prix', valueType: 'numeric' },
      ],
      impact: [
        {
          mapping: 'impact.ecoscore',
          title: 'Eco score',
          valueType: 'numeric',
        },
      ],
      technical: [
        {
          mapping: 'power.hdr',
          title: 'Consommation électrique (HDR)',
          valueType: 'numeric',
        },
        {
          mapping: 'power.hdr',
          title: 'Consommation électrique (HDR)',
          valueType: 'numeric',
        },
      ],
    }

    const normalized = normalizeFieldOptionsResponse(options)

    expect(normalized?.global).toHaveLength(1)
    expect(normalized?.impact).toHaveLength(1)
    expect(normalized?.technical).toHaveLength(1)
  })
})
