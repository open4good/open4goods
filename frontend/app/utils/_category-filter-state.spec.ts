import { describe, expect, it } from 'vitest'

import {
  buildCategoryHash,
  deserializeCategoryHashState,
  serializeCategoryHashState,
  type CategoryHashState,
} from './_category-filter-state'
import { ECOSCORE_VALUE_FIELD } from '~/constants/scores'

describe('category filter hash serialisation', () => {
  it('serialises and deserialises state payloads', () => {
    const state: CategoryHashState = {
      search: 'smartphone',
      pageNumber: 2,
      view: 'cards',
      activeSubsets: ['eco', 'budget'],
      impactExpanded: true,
      filters: {
        filters: [
          {
            field: ECOSCORE_VALUE_FIELD,
            operator: 'range',
            min: 80,
            max: 100,
          },
        ],
      },
    }

    const serialised = serializeCategoryHashState(state)
    expect(serialised).toBeTypeOf('string')
    expect(serialised.length).toBeGreaterThan(0)

    const deserialised = deserializeCategoryHashState(serialised)
    expect(deserialised).toEqual(state)
  })

  it('returns null when payload is empty or invalid', () => {
    expect(deserializeCategoryHashState('')).toBeNull()
    expect(deserializeCategoryHashState(undefined)).toBeNull()

    const invalidPayload = 'invalid-base64'
    expect(deserializeCategoryHashState(invalidPayload)).toBeNull()
  })

  it('accepts URI encoded payloads', () => {
    const state: CategoryHashState = { search: 'écologie' }
    const serialised = serializeCategoryHashState(state)
    const encoded = encodeURIComponent(serialised)

    expect(deserializeCategoryHashState(encoded)).toEqual(state)
  })

  it('buildCategoryHash prefixes with hash character', () => {
    const payload: CategoryHashState = { view: 'list' }
    const hash = buildCategoryHash(payload)

    expect(hash.startsWith('#')).toBe(true)

    const restored = deserializeCategoryHashState(hash.slice(1))
    expect(restored).toEqual(payload)
  })
})
