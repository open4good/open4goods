import type { FieldMetadataDto, ProductDto } from '~~/shared/api-client'
import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'
import {
  resolveAttributeRawValueByKey,
  formatAttributeValue,
  type ResolvedProductAttribute,
} from '~/utils/_product-attributes'
import { formatOffersCount } from '~/utils/_product-pricing'

import {
  resolveFilterFieldTitle,
  resolveSortFieldTitle,
} from '~/utils/_field-localization'

type TranslateFn = (key: string, params?: Record<string, unknown>) => string
type NumberFormatFn = (
  value: number,
  options?: Intl.NumberFormatOptions
) => string
type PluralizeFn = (
  key: string,
  count: number,
  params?: Record<string, unknown>
) => string

const STATIC_SORT_FIELDS = new Set([
  ECOSCORE_RELATIVE_FIELD,
  'price.minPrice.price',
  'attributes.referentielAttributes.BRAND',
  'attributes.referentielAttributes.MODEL',
  'offersCount',
])

export const extractAttributeKeyFromMapping = (
  mapping: string
): string | null => {
  const patterns = [
    /attributes\.(?:indexed(?:Attributes)?|referential(?:Attributes)?|referentielAttributes|indexedAttributes)\.([^.]+)/i,
    /attributes\.([^.]+)/i,
  ]

  for (const pattern of patterns) {
    const match = mapping.match(pattern)
    if (match?.[1]) {
      return match[1]
    }
  }

  return null
}

/**
 * Determines if a sort field is a "custom" field that requires dynamic display.
 * A custom field is one that is NOT:
 * - A static sort field (price, impactScore, brand, model, offersCount)
 * - One of the popular attributes already shown in the UI
 */
export const isCustomSortField = (
  sortField: string | null | undefined,
  popularAttributeKeys: string[]
): boolean => {
  if (!sortField) {
    return false
  }

  if (STATIC_SORT_FIELDS.has(sortField)) {
    return false
  }

  const attributeKey = extractAttributeKeyFromMapping(sortField)

  if (attributeKey && popularAttributeKeys.includes(attributeKey)) {
    return false
  }

  return true
}

export interface SortedAttributeDisplay {
  key: string
  label: string
  value: string
}

export interface SortedFieldDisplay {
  key: string
  label: string
  value: string
  type: 'attribute' | 'static'
  attributeKey?: string | null
}

const resolveSortFieldLabel = (
  sortField: string,
  fieldMetadata: Record<string, FieldMetadataDto> | null | undefined,
  t: TranslateFn
): string => {
  const metadata = fieldMetadata?.[sortField]
  return resolveSortFieldTitle(metadata ?? { mapping: sortField, title: '' }, t)
}

const resolveStaticSortDisplay = (
  product: ProductDto,
  sortField: string,
  fieldMetadata: Record<string, FieldMetadataDto> | null | undefined,
  t: TranslateFn,
  n: NumberFormatFn,
  translatePlural?: PluralizeFn
): SortedFieldDisplay | null => {
  const label = resolveSortFieldLabel(sortField, fieldMetadata, t)

  // Impact score and price already have dedicated UI in the product card
  // (ImpactScore badge and ProductPriceRows), so skip sorted-field display.
  if (sortField === ECOSCORE_RELATIVE_FIELD) {
    return null
  }

  if (sortField === 'price.minPrice.price') {
    return null
  }

  if (sortField === 'offersCount') {
    if (!translatePlural) {
      return null
    }

    return {
      key: sortField,
      label,
      value: formatOffersCount(product, translatePlural),
      type: 'static',
    }
  }

  if (sortField === 'attributes.referentielAttributes.BRAND') {
    const brand =
      product.identity?.brand ?? resolveAttributeRawValueByKey(product, 'BRAND')

    if (!brand) {
      return null
    }

    return {
      key: sortField,
      label,
      value: String(brand),
      type: 'static',
    }
  }

  if (sortField === 'attributes.referentielAttributes.MODEL') {
    const model =
      product.identity?.model ?? resolveAttributeRawValueByKey(product, 'MODEL')

    if (!model) {
      return null
    }

    return {
      key: sortField,
      label,
      value: String(model),
      type: 'static',
    }
  }

  return null
}

/**
 * Resolves the sorted attribute value from a product for display.
 * Returns null if the value cannot be resolved or formatted.
 */
export const resolveSortedAttributeValue = (
  product: ProductDto,
  sortField: string | null | undefined,
  fieldMetadata: Record<string, FieldMetadataDto> | null | undefined,
  t: TranslateFn,
  n: NumberFormatFn
): SortedAttributeDisplay | null => {
  if (!sortField) {
    return null
  }

  const attributeKey = extractAttributeKeyFromMapping(sortField)

  if (!attributeKey) {
    return null
  }

  const rawValue = resolveAttributeRawValueByKey(product, attributeKey)

  if (rawValue == null) {
    return null
  }

  const metadata = fieldMetadata?.[sortField]
  const label =
    metadata?.title ??
    resolveFilterFieldTitle(metadata, t, sortField) ??
    attributeKey

  const attribute: ResolvedProductAttribute = {
    key: attributeKey,
    label,
    rawValue,
    unit: null,
    icon: null,
    suffix: null,
  }

  const formattedValue = formatAttributeValue(attribute, t, n)

  if (!formattedValue) {
    return null
  }

  return {
    key: attributeKey,
    label,
    value: formattedValue,
  }
}

export const resolveSortedFieldDisplay = (
  product: ProductDto,
  sortField: string | null | undefined,
  fieldMetadata: Record<string, FieldMetadataDto> | null | undefined,
  t: TranslateFn,
  n: NumberFormatFn,
  translatePlural?: PluralizeFn
): SortedFieldDisplay | null => {
  if (!sortField) {
    return null
  }

  if (STATIC_SORT_FIELDS.has(sortField)) {
    return resolveStaticSortDisplay(
      product,
      sortField,
      fieldMetadata,
      t,
      n,
      translatePlural
    )
  }

  const attributeDisplay = resolveSortedAttributeValue(
    product,
    sortField,
    fieldMetadata,
    t,
    n
  )

  if (!attributeDisplay) {
    return null
  }

  return {
    key: sortField,
    label: attributeDisplay.label,
    value: attributeDisplay.value,
    type: 'attribute',
    attributeKey: attributeDisplay.key,
  }
}
