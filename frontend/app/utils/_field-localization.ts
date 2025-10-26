import type { FieldMetadataDto } from '~~/shared/api-client'

type TranslateFn = (key: string, ...args: unknown[]) => string

const FILTER_FIELD_TRANSLATION_KEYS: Record<string, string> = {
  'price.minPrice.price': 'category.filters.fields.price',
  offersCount: 'category.filters.fields.offersCount',
  'price.conditions': 'category.filters.fields.condition',
  googleTaxonomyId: 'category.filters.fields.googleTaxonomyId',
  'attributes.referentielAttributes.BRAND': 'category.filters.fields.brand',
  'gtinInfos.country': 'category.filters.fields.country',
  datasourceCodes: 'category.filters.fields.datasource',
}

const SORT_FIELD_TRANSLATION_KEYS: Record<string, string> = {
  'price.minPrice.price': 'category.products.sort.fields.price',
  offersCount: 'category.products.sort.fields.offersCount',
  'identity.brand.keyword': 'category.products.sort.fields.brand',
  'identity.model.keyword': 'category.products.sort.fields.model',
  'scores.ECOSCORE.value': 'category.products.sort.fields.impactScore',
}

const resolveTranslation = (mapping: string, t: TranslateFn, keys: Record<string, string>): string | null => {
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
  fallbackMapping?: string | null,
): string => {
  const providedTitle = normalizeTitle(field?.title)
  if (providedTitle) {
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

export const resolveSortFieldTitle = (field: FieldMetadataDto, t: TranslateFn): string => {
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
