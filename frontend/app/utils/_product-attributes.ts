import type { AttributeConfigDto, ProductDto } from '~~/shared/api-client'

type TranslateFn = (key: string, params?: Record<string, unknown>) => string
type NumberFormatFn = (
  value: number,
  options?: Intl.NumberFormatOptions
) => string

export interface ResolvedProductAttribute {
  key: string
  label: string
  rawValue: unknown
  unit?: string | null
  icon?: string | null
  suffix?: string | null
  mappings?: Record<string, string> | null
}

const hasMeaningfulValue = (value: unknown): boolean => {
  if (value == null) {
    return false
  }

  if (Array.isArray(value)) {
    return value.some(entry => hasMeaningfulValue(entry))
  }

  if (typeof value === 'string') {
    return value.trim().length > 0
  }

  if (typeof value === 'number') {
    return Number.isFinite(value)
  }

  return true
}

const readValueFromIndexedAttribute = (attribute: unknown): unknown => {
  if (!attribute || typeof attribute !== 'object') {
    return null
  }

  const record = attribute as Record<string, unknown>

  if (hasMeaningfulValue(record.value)) {
    return record.value
  }

  if (hasMeaningfulValue(record.numericValue)) {
    return record.numericValue
  }

  if (record.booleanValue != null) {
    return record.booleanValue
  }

  return null
}

const getValueByPath = (
  product: ProductDto,
  path: string | undefined
): unknown => {
  if (!path) {
    return undefined
  }

  return path.split('.').reduce<unknown>((accumulator, segment) => {
    if (
      accumulator == null ||
      typeof accumulator !== 'object' ||
      Array.isArray(accumulator)
    ) {
      return undefined
    }

    const record = accumulator as Record<string, unknown>

    if (!(segment in record)) {
      return undefined
    }

    return record[segment]
  }, product as unknown)
}

const resolveAttributeRawValue = (
  product: ProductDto,
  key: string
): unknown => {
  if (!key) {
    return null
  }

  const directValue = getValueByPath(product, key)
  if (hasMeaningfulValue(directValue)) {
    return directValue
  }

  const referential = product.attributes?.referentialAttributes?.[key]
  if (hasMeaningfulValue(referential)) {
    return referential
  }

  const indexed = product.attributes?.indexedAttributes?.[key]
  const indexedValue = readValueFromIndexedAttribute(indexed)
  if (hasMeaningfulValue(indexedValue)) {
    return indexedValue
  }

  const nestedIndexed = getValueByPath(
    product,
    `attributes.indexedAttributes.${key}`
  )
  const nestedValue = readValueFromIndexedAttribute(nestedIndexed)
  if (hasMeaningfulValue(nestedValue)) {
    return nestedValue
  }

  const nestedReferential = getValueByPath(
    product,
    `attributes.referentialAttributes.${key}`
  )
  if (hasMeaningfulValue(nestedReferential)) {
    return nestedReferential
  }

  return null
}

export const resolveAttributeRawValueByKey = (
  product: ProductDto,
  key: string
): unknown => {
  return resolveAttributeRawValue(product, key)
}

export const resolvePopularAttributes = (
  product: ProductDto,
  configs: AttributeConfigDto[] | null | undefined
): ResolvedProductAttribute[] => {
  if (!configs?.length) {
    return []
  }

  return configs.reduce<ResolvedProductAttribute[]>((accumulator, config) => {
    const key = config.key ?? ''
    if (!key) {
      return accumulator
    }

    const rawValue = resolveAttributeRawValue(product, key)
    if (!hasMeaningfulValue(rawValue)) {
      return accumulator
    }

    accumulator.push({
      key,
      label: config.name ?? key,
      rawValue,
      unit: config.unit,
      icon: config.icon,
      suffix: config.suffix ?? null,
      mappings: config.mappings ?? null,
    })

    return accumulator
  }, [])
}

export const resolveRemainingAttributes = (
  product: ProductDto,
  popularKeys: string[] = [],
  limit: number | null = 6
): ResolvedProductAttribute[] => {
  const usedKeys = new Set(popularKeys.filter(Boolean))
  const results: ResolvedProductAttribute[] = []

  const indexed = product.attributes?.indexedAttributes ?? {}
  Object.entries(indexed).forEach(([key, attribute]) => {
    if (usedKeys.has(key)) {
      return
    }

    const rawValue = readValueFromIndexedAttribute(attribute)
    if (!hasMeaningfulValue(rawValue)) {
      return
    }

    const label =
      typeof attribute === 'object' &&
      attribute &&
      'name' in attribute &&
      typeof attribute.name === 'string'
        ? attribute.name
        : key

    results.push({ key, label, rawValue })
  })

  const referential = product.attributes?.referentialAttributes ?? {}
  Object.entries(referential).forEach(([key, rawValue]) => {
    if (usedKeys.has(key)) {
      return
    }

    if (!hasMeaningfulValue(rawValue)) {
      return
    }

    results.push({ key, label: key, rawValue })
  })

  return limit != null ? results.slice(0, limit) : results
}

const normalizeMappings = (
  mappings?: Record<string, string> | null
): Map<string, string> => {
  const normalized = new Map<string, string>()

  if (!mappings) {
    return normalized
  }

  Object.entries(mappings).forEach(([key, value]) => {
    const normalizedKey = key.trim().toLocaleLowerCase()
    if (!normalizedKey.length) {
      return
    }

    normalized.set(normalizedKey, value)
  })

  return normalized
}

const resolveMappedValue = (
  value: string,
  mappings: Map<string, string>
): string => {
  if (!mappings.size) {
    return value
  }

  const normalized = value.trim().toLocaleLowerCase()
  if (!normalized.length) {
    return value
  }

  const mapped = mappings.get(normalized)
  if (typeof mapped === 'string' && mapped.trim().length) {
    return mapped
  }

  return value
}

const formatRawValue = (
  value: unknown,
  t: TranslateFn,
  n: NumberFormatFn,
  mappings: Map<string, string>
): string | null => {
  if (value == null) {
    return null
  }

  if (Array.isArray(value)) {
    const formatted = value
      .map(entry => formatRawValue(entry, t, n, mappings))
      .filter((entry): entry is string => Boolean(entry))
      .join(', ')

    return formatted || null
  }

  if (typeof value === 'number') {
    if (!Number.isFinite(value)) {
      return null
    }

    return n(value)
  }

  if (typeof value === 'boolean') {
    return t(`common.boolean.${value ? 'true' : 'false'}`)
  }

  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (!trimmed.length) {
      return null
    }

    return resolveMappedValue(trimmed, mappings)
  }

  return String(value)
}

export const formatAttributeValue = (
  attribute: ResolvedProductAttribute,
  t: TranslateFn,
  n: NumberFormatFn
): string | null => {
  const mappings = normalizeMappings(attribute.mappings)
  const base = formatRawValue(attribute.rawValue, t, n, mappings)

  if (!base) {
    return null
  }

  let formatted = base

  if (attribute.unit) {
    formatted = `${formatted}\u00a0${attribute.unit}`
  }

  if (attribute.suffix) {
    formatted = `${formatted} ${attribute.suffix}`
  }

  return formatted
}
