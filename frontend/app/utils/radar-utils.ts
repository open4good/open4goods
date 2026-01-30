import type { AttributeConfigDto } from '~~/shared/api-client'

export interface RadarInversionContext {
  axisId: string
  productValue: number | null
  bestValue: number | null
  worstValue: number | null
  scaleMin?: number
  scaleMax?: number
}

/**
 * Transforms a radar chart value, applying inversion if the attribute is "lower is better".
 *
 * @param value The value to transform
 * @param context Context containing axis ID and all observed values for this axis (for scaling)
 * @param attributeConfigMap Map of attribute configurations to look up impactBetterIs
 * @returns The transformed value (possibly inverted) or the original value
 */
export function transformRadarValue(
  value: number | null,
  context: RadarInversionContext,
  attributeConfigMap: Map<string, AttributeConfigDto> | undefined
): number | null {
  if (value === null || !Number.isFinite(value)) {
    return null
  }

  const attributeConfig = attributeConfigMap?.get(context.axisId.toUpperCase())
  const isLowerBetter = attributeConfig?.impactBetterIs === 'LOWER'

  // Determine scale bounds
  let min = context.scaleMin
  let max = context.scaleMax

  // If bounds are missing, infer from observed values
  if (min === undefined || max === undefined) {
    const observed = [
      context.productValue,
      context.bestValue,
      context.worstValue,
    ].filter((v): v is number => typeof v === 'number' && Number.isFinite(v))

    if (observed.length) {
      min = Math.min(...observed)
      max = Math.max(...observed)
    } else {
      // Last resort fallback
      min = 0
      max = 100
    }
  }

  // Avoid division by zero for flat scales
  if (Math.abs(max - min) < 0.000001) {
    // If range is 0, everybody gets full score if it's "good" (min for lower-better, max for higher-better)
    return 100
  }

  let normalized = 0
  if (isLowerBetter) {
    // Lower is Better: Min is Best (100), Max is Worst (0)
    // Formula: (Max - Value) / (Max - Min)
    normalized = (max - value) / (max - min)
  } else {
    // Higher is Better: Max is Best (100), Min is Worst (0)
    // Formula: (Value - Min) / (Max - Min)
    normalized = (value - min) / (max - min)
  }

  // Clamp between 0 and 1 to handle outliers outside expected min/max
  normalized = Math.max(0, Math.min(1, normalized))

  return normalized * 100
}
