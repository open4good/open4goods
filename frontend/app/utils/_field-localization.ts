import type { FieldMetadataDto } from '~~/shared/api-client'
import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'

type TranslateFn = (key: string, ...args: unknown[]) => string

const FILTER_FIELD_TRANSLATION_KEYS: Record<string, string> = {
  'price.minPrice.price': 'category.filters.fields.price',
  offersCount: 'category.filters.fields.offersCount',
  'price.conditions': 'category.filters.fields.condition',
  googleTaxonomyId: 'category.filters.fields.googleTaxonomyId',
  'attributes.referentielAttributes.BRAND': 'category.filters.fields.brand',
  'gtinInfos.country': 'category.filters.fields.country',
  datasourceCodes: 'category.filters.fields.datasource',
  gtin: 'technicalFieldGuide.fields.gtin.label',
  'scores.ECOSCORE.relative.value':
    'technicalFieldGuide.fields.ecoscoreRelative.label',
  'scores.ECOSCORE.relativ.value':
    'technicalFieldGuide.fields.ecoscoreRelative.label',
  'scores.ECOSCORE.value': 'technicalFieldGuide.fields.ecoscoreAbsolute.label',
  'scores.ECOSCORE.ranking':
    'technicalFieldGuide.fields.ecoscoreRanking.label',
}

const SORT_FIELD_TRANSLATION_KEYS: Record<string, string> = {
  'price.minPrice.price': 'category.products.sort.fields.price',
  offersCount: 'category.products.sort.fields.offersCount',
  'attributes.referentielAttributes.BRAND':
    'category.products.sort.fields.brand',
  'attributes.referentielAttributes.MODEL':
    'category.products.sort.fields.model',
  [ECOSCORE_RELATIVE_FIELD]: 'category.products.sort.fields.impactScore',
}

export const SORT_FIELD_PRIORITIES: Record<string, number> = {
  [ECOSCORE_RELATIVE_FIELD]: 100,
  'price.minPrice.price': 90,
  offersCount: 80,
}

const resolveTranslation = (
  mapping: string,
  t: TranslateFn,
  keys: Record<string, string>
): string | null => {
  const translationKey = keys[mapping]
  if (!translationKey) {
    return null
  }

  const translated = t(translationKey)
  return translated && translated !== translationKey ? translated : null
}

const normalizeTitle = (title?: string | null) => title?.trim() ?? ''

export const resolveFilterFieldTitle = (
  field: FieldMetadataDto | undefined,
  t: TranslateFn,
  fallbackMapping?: string | null
): string => {
  const providedTitle = normalizeTitle(field?.title)
  if (providedTitle && providedTitle.toLowerCase() !== 'explication') {
    return providedTitle
  }

  const mapping = field?.mapping ?? fallbackMapping ?? ''
  if (!mapping) {
    return ''
  }

  const translated =
    resolveTranslation(mapping, t, FILTER_FIELD_TRANSLATION_KEYS) ??
    resolveTranslation(mapping, t, SORT_FIELD_TRANSLATION_KEYS)

  return translated ?? mapping
}

export const resolveSortFieldTitle = (
  field: FieldMetadataDto,
  t: TranslateFn
): string => {
  const providedTitle = normalizeTitle(field.title)
  if (providedTitle) {
    return providedTitle
  }

  const mapping = field.mapping ?? ''
  if (!mapping) {
    return ''
  }

  const translated =
    resolveTranslation(mapping, t, SORT_FIELD_TRANSLATION_KEYS) ??
    resolveTranslation(mapping, t, FILTER_FIELD_TRANSLATION_KEYS)

  return translated ?? mapping
}
