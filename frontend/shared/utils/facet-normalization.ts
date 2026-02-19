const LABEL_SYNONYMS: Record<string, string> = {
  occasion: 'used',
  refurbished: 'used',
  'seconde main': 'used',
  neuf: 'new',
  nouveau: 'new',
  litre: 'l',
  litres: 'l',
  kilogramme: 'kg',
  kilogrammes: 'kg',
}

const stripDiacritics = (value: string): string =>
  value.normalize('NFD').replace(/\p{Diacritic}+/gu, '')

const normalizePlural = (value: string): string => {
  if (value.length <= 3) {
    return value
  }

  if (value.endsWith('es')) {
    return value.slice(0, -2)
  }

  if (value.endsWith('s')) {
    return value.slice(0, -1)
  }

  return value
}

/**
 * Canonical normalization used to compare facet labels across sources.
 */
export const normalizeFacetLabel = (value: string | null | undefined): string => {
  const trimmed = String(value ?? '').trim()
  if (!trimmed) {
    return ''
  }

  const collapsed = trimmed.replace(/\s+/g, ' ').toLocaleLowerCase()
  const noAccents = stripDiacritics(collapsed)
  const singular = normalizePlural(noAccents)

  return LABEL_SYNONYMS[singular] ?? singular
}

/**
 * Returns true when a label can safely be rendered in UI.
 */
export const hasRenderableFacetLabel = (
  value: string | null | undefined
): boolean => normalizeFacetLabel(value).length > 0

const UNIT_BY_FIELD_PATTERN: Array<{ pattern: RegExp; unit: string }> = [
  { pattern: /(^|\.)volume(\.|$)|litre|liter/i, unit: 'L' },
  { pattern: /(^|\.)weight(\.|$)|mass/i, unit: 'kg' },
  { pattern: /energy.*annual|kwh/i, unit: 'kWh/an' },
  { pattern: /co2|carbon/i, unit: 'kg CO₂e' },
]

/**
 * Resolves a display unit for known numeric facet fields.
 */
export const resolveFacetUnit = (mapping: string | null | undefined): string => {
  const key = String(mapping ?? '')

  const matched = UNIT_BY_FIELD_PATTERN.find(entry => entry.pattern.test(key))
  return matched?.unit ?? ''
}
