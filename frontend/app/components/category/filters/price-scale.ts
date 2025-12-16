export type PriceScaleSegment = {
  limit: number
  step: number
}

const PRICE_SCALE_SEGMENTS: PriceScaleSegment[] = [
  { limit: 1000, step: 5 },
  { limit: 5000, step: 50 },
  { limit: 20000, step: 250 },
]

const DEFAULT_STEP = 1000

const clampNumericValue = (
  value: number | null | undefined,
  fallback = 0
): number => {
  if (value == null || Number.isNaN(value)) {
    return fallback
  }

  return Math.max(0, Number(value))
}

const resolveSegmentSteps = (
  lowerBound: number,
  segment: PriceScaleSegment
): number => {
  return Math.round((segment.limit - lowerBound) / segment.step)
}

export const priceToSliderValue = (
  price: number | null | undefined
): number => {
  const safePrice = clampNumericValue(price)
  let position = 0
  let lowerBound = 0

  for (const segment of PRICE_SCALE_SEGMENTS) {
    if (safePrice <= lowerBound) {
      return position
    }

    const segmentSteps = resolveSegmentSteps(lowerBound, segment)
    if (safePrice >= segment.limit) {
      position += segmentSteps
      lowerBound = segment.limit
      continue
    }

    const relativeValue = Math.round((safePrice - lowerBound) / segment.step)
    return position + relativeValue
  }

  const lastLimit = PRICE_SCALE_SEGMENTS.at(-1)?.limit ?? 0
  const lastStep = PRICE_SCALE_SEGMENTS.at(-1)?.step ?? DEFAULT_STEP
  const extraSteps = Math.round((safePrice - lastLimit) / lastStep)

  return position + extraSteps
}

export const sliderValueToPrice = (
  sliderValue: number | null | undefined
): number => {
  const safeValue = clampNumericValue(sliderValue)
  let remaining = Math.round(safeValue)
  let lowerBound = 0

  for (const segment of PRICE_SCALE_SEGMENTS) {
    const segmentSteps = resolveSegmentSteps(lowerBound, segment)
    if (remaining <= segmentSteps) {
      return lowerBound + remaining * segment.step
    }

    remaining -= segmentSteps
    lowerBound = segment.limit
  }

  const lastStep = PRICE_SCALE_SEGMENTS.at(-1)?.step ?? DEFAULT_STEP
  return lowerBound + remaining * lastStep
}

export const isPriceField = (mapping: string | null | undefined): boolean => {
  if (!mapping) {
    return false
  }

  return mapping.toLowerCase().includes('price')
}

export const clampSliderRange = ([min, max]: [number, number]): [
  number,
  number,
] => {
  if (Number.isNaN(min) || Number.isNaN(max)) {
    return [0, 0]
  }

  return min <= max ? [min, max] : [max, min]
}
