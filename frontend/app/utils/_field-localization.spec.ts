import { describe, expect, it } from 'vitest'
import type { FieldMetadataDto } from '~~/shared/api-client'
import { resolveFilterFieldTitle } from './_field-localization'

describe('resolveFilterFieldTitle', () => {
  const t = (key: string) =>
    ({
      'category.filters.fields.condition': 'Offer condition',
    })[key] ?? key

  it('uses i18n mappings for condition filters', () => {
    const conditionField: FieldMetadataDto = {
      mapping: 'price.conditions',
      title: '',
      valueType: 'keyword',
    }

    expect(resolveFilterFieldTitle(conditionField, t)).toBe('Offer condition')
  })
})
