import { describe, expect, it } from 'vitest'
import type { FieldMetadataDto } from '~~/shared/api-client'
import { resolveFilterFieldTitle } from './_field-localization'

describe('resolveFilterFieldTitle', () => {
  const t = (key: string) =>
    ({
      'category.filters.fields.creationDate': 'Creation date',
      'category.filters.fields.lastChange': 'Last updated',
    })[key] ?? key

  it('uses i18n mappings for date filters', () => {
    const creationField: FieldMetadataDto = {
      mapping: 'creationDate',
      title: '',
      valueType: 'numeric',
    }

    const lastChangeField: FieldMetadataDto = {
      mapping: 'lastChange',
      title: '',
      valueType: 'numeric',
    }

    expect(resolveFilterFieldTitle(creationField, t)).toBe('Creation date')
    expect(resolveFilterFieldTitle(lastChangeField, t)).toBe('Last updated')
  })
})
