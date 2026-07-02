import type {
  ProductDto,
  ProductPriceHistoryEntryDto,
} from '~~/shared/api-client'

export type VerdictLevel =
  'excellent' | 'good' | 'fair' | 'poor' | 'insufficient'

export interface VerdictDimension {
  level: VerdictLevel
  score: number | null
  scoreOutOf?: number
  labelKey: string
  color: string
  icon: string
  text?: string
}

export interface PriceVerdict {
  level: 'good' | 'fair' | 'poor' | 'insufficient'
  deviationPercent: number | null
  currentPrice: number | null
  medianPrice: number | null
  labelKey: string
  color: string
  icon: string
}

export const getImpactLevel = (score: number): VerdictLevel => {
  if (score >= 17) return 'excellent'
  if (score >= 14) return 'good'
  if (score >= 11) return 'fair'
  if (score >= 8) return 'poor'
  return 'insufficient'
}

export const getImpactColor = (level: VerdictLevel): string => {
  switch (level) {
    case 'excellent':
    case 'good':
      return 'success'
    case 'fair':
    case 'poor':
      return 'warning'
    case 'insufficient':
    default:
      return 'error'
  }
}

export const calculateMedianPrice = (
  entries?: ProductPriceHistoryEntryDto[]
): number | null => {
  if (!entries || entries.length === 0) return null
  const prices = entries
    .map(e => e.price)
    .filter((p): p is number => typeof p === 'number' && Number.isFinite(p))
    .sort((a, b) => a - b)

  if (prices.length === 0) return null
  const mid = Math.floor(prices.length / 2)
  if (prices.length % 2 !== 0) {
    return prices[mid]
  }
  return (prices[mid - 1] + prices[mid]) / 2
}

export const getPriceVerdict = (product: ProductDto): PriceVerdict => {
  const currentPrice =
    product.offers?.bestNewOffer?.price ??
    product.offers?.bestPrice?.price ??
    null
  const historyEntries = product.offers?.newHistory?.entries
  const medianPrice =
    calculateMedianPrice(historyEntries) ??
    product.offers?.newHistory?.average ??
    null

  const validHistoryCount = (historyEntries ?? [])
    .map(e => e.price)
    .filter(
      (p): p is number => typeof p === 'number' && Number.isFinite(p)
    ).length

  if (
    currentPrice === null ||
    medianPrice === null ||
    medianPrice === 0 ||
    validHistoryCount < 3
  ) {
    return {
      level: 'insufficient',
      deviationPercent: null,
      currentPrice,
      medianPrice,
      labelKey: 'product.verdict.priceLevels.insufficient',
      color: 'grey',
      icon: 'mdi-tag-outline',
    }
  }

  const deviationPercent = ((currentPrice - medianPrice) / medianPrice) * 100

  let level: 'good' | 'fair' | 'poor'
  let color: string
  if (deviationPercent <= -5) {
    level = 'good'
    color = 'success'
  } else if (deviationPercent >= 5) {
    level = 'poor'
    color = 'error'
  } else {
    level = 'fair'
    color = 'warning'
  }

  return {
    level,
    deviationPercent: Math.round(deviationPercent),
    currentPrice,
    medianPrice,
    labelKey: `product.verdict.priceLevels.${level}`,
    color,
    icon: 'mdi-tag-outline',
  }
}

export const getReliabilityLevel = (score: number): VerdictLevel => {
  if (score >= 17) return 'excellent'
  if (score >= 14) return 'good'
  if (score >= 11) return 'fair'
  if (score >= 8) return 'poor'
  return 'insufficient'
}

export const getReliabilityVerdict = (
  product: ProductDto
): VerdictDimension => {
  const dqScore = product.scores?.scores?.['DATA_QUALITY']?.value ?? null

  if (dqScore === null) {
    return {
      level: 'insufficient',
      score: null,
      scoreOutOf: 20,
      labelKey: 'product.verdict.reliabilityLevels.insufficient',
      color: 'grey',
      icon: 'mdi-shield-check-outline',
    }
  }

  const level = getReliabilityLevel(dqScore)
  let color: string
  if (level === 'excellent' || level === 'good') {
    color = 'success'
  } else if (level === 'fair' || level === 'poor') {
    color = 'warning'
  } else {
    color = 'error'
  }

  return {
    level,
    score: dqScore,
    scoreOutOf: 20,
    labelKey: `product.verdict.reliabilityLevels.${level}`,
    color,
    icon: 'mdi-shield-check-outline',
  }
}
