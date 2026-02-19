export type TechnicalFieldGuideEntry = {
  mapping: string
  labelKey: string
  tooltipKey: string
  sourceKey: string
  essential: boolean
}

const FIELD_GUIDE_ENTRIES: TechnicalFieldGuideEntry[] = [
  {
    mapping: 'gtin',
    labelKey: 'technicalFieldGuide.fields.gtin.label',
    tooltipKey: 'technicalFieldGuide.fields.gtin.tooltip',
    sourceKey: 'technicalFieldGuide.fields.gtin.source',
    essential: true,
  },
  {
    mapping: 'gtinInfos.country',
    labelKey: 'technicalFieldGuide.fields.gtinCountry.label',
    tooltipKey: 'technicalFieldGuide.fields.gtinCountry.tooltip',
    sourceKey: 'technicalFieldGuide.fields.gtinCountry.source',
    essential: false,
  },
  {
    mapping: 'scores.ECOSCORE.relative.value',
    labelKey: 'technicalFieldGuide.fields.ecoscoreRelative.label',
    tooltipKey: 'technicalFieldGuide.fields.ecoscoreRelative.tooltip',
    sourceKey: 'technicalFieldGuide.fields.ecoscoreRelative.source',
    essential: true,
  },
  {
    mapping: 'scores.ECOSCORE.value',
    labelKey: 'technicalFieldGuide.fields.ecoscoreAbsolute.label',
    tooltipKey: 'technicalFieldGuide.fields.ecoscoreAbsolute.tooltip',
    sourceKey: 'technicalFieldGuide.fields.ecoscoreAbsolute.source',
    essential: false,
  },
  {
    mapping: 'scores.ECOSCORE.ranking',
    labelKey: 'technicalFieldGuide.fields.ecoscoreRanking.label',
    tooltipKey: 'technicalFieldGuide.fields.ecoscoreRanking.tooltip',
    sourceKey: 'technicalFieldGuide.fields.ecoscoreRanking.source',
    essential: false,
  },
  {
    mapping: 'price.minPrice.price',
    labelKey: 'technicalFieldGuide.fields.bestPrice.label',
    tooltipKey: 'technicalFieldGuide.fields.bestPrice.tooltip',
    sourceKey: 'technicalFieldGuide.fields.bestPrice.source',
    essential: true,
  },
  {
    mapping: 'price.conditions',
    labelKey: 'technicalFieldGuide.fields.offerCondition.label',
    tooltipKey: 'technicalFieldGuide.fields.offerCondition.tooltip',
    sourceKey: 'technicalFieldGuide.fields.offerCondition.source',
    essential: true,
  },
  {
    mapping: 'offersCount',
    labelKey: 'technicalFieldGuide.fields.offersCount.label',
    tooltipKey: 'technicalFieldGuide.fields.offersCount.tooltip',
    sourceKey: 'technicalFieldGuide.fields.offersCount.source',
    essential: true,
  },
  {
    mapping: 'datasourceCodes',
    labelKey: 'technicalFieldGuide.fields.datasourceCodes.label',
    tooltipKey: 'technicalFieldGuide.fields.datasourceCodes.tooltip',
    sourceKey: 'technicalFieldGuide.fields.datasourceCodes.source',
    essential: false,
  },
]

const FIELD_GUIDE_BY_MAPPING = new Map(
  FIELD_GUIDE_ENTRIES.map(entry => [entry.mapping.toLowerCase(), entry])
)

const FIELD_GUIDE_ALIASES: Record<string, string> = {
  'scores.ecoscore.relativ.value': 'scores.ECOSCORE.relative.value',
}

const normalizeMapping = (mapping: string) => mapping.trim().toLowerCase()

export const resolveTechnicalFieldGuideEntry = (
  mapping: string | null | undefined
): TechnicalFieldGuideEntry | null => {
  if (!mapping?.trim()) {
    return null
  }

  const normalized = normalizeMapping(mapping)
  const canonical = FIELD_GUIDE_ALIASES[normalized] ?? mapping

  return FIELD_GUIDE_BY_MAPPING.get(normalizeMapping(canonical)) ?? null
}

export const buildTechnicalFieldGuideEntries = (
  mappings: Array<string | null | undefined>
): TechnicalFieldGuideEntry[] => {
  const unique = new Set<string>()
  const result: TechnicalFieldGuideEntry[] = []

  mappings.forEach(mapping => {
    if (!mapping?.trim()) {
      return
    }

    const entry = resolveTechnicalFieldGuideEntry(mapping)

    if (!entry) {
      return
    }

    if (unique.has(entry.mapping)) {
      return
    }

    unique.add(entry.mapping)
    result.push(entry)
  })

  return result
}
