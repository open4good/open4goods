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
    return value
  }

  const attributeConfig = attributeConfigMap?.get(context.axisId.toUpperCase())

  // Inversion for "Lower is Better" attributes (e.g. Energy Consumption)
  if (attributeConfig?.impactBetterIs === 'LOWER') {
    // If we have explicit Min/Max scales (which should be the case for scores)
    if (
      typeof context.scaleMin === 'number' &&
      Number.isFinite(context.scaleMin) &&
      typeof context.scaleMax === 'number' &&
      Number.isFinite(context.scaleMax)
    ) {
      // Logic: Max Axis Value = Best Score (scaleMin)
      // We want to map [Best(scaleMin), Worst(scaleMax)] -> [AxisMax(scaleMin), 0]

      const best = context.scaleMin
      const worst = context.scaleMax

      // Safety check to avoid division by zero
      if (Math.abs(worst - best) < 0.0001) {
        return best // Flat scale? return max.
      }

      // Linear interpolation:
      // Normalized (0-1 where 1 is best) = (worst - value) / (worst - best)
      // Plotted (0-AxisMax) = Normalized * AxisMax
      const normalized = Math.max(0, (worst - value) / (worst - best))
      return normalized * best
    }

    // Fallback if no explicit scales (legacy behavior or missing data)
    // We try to infer a boundary from observed values
    let maxBoundary = 5
    if (context.scaleMax) {
      maxBoundary = context.scaleMax
    } else {
      const axisValues = [
        context.productValue,
        context.bestValue,
        context.worstValue,
      ].filter((v): v is number => typeof v === 'number' && Number.isFinite(v))

      const maxObserved = axisValues.length ? Math.max(...axisValues) : 5
      maxBoundary = maxObserved > 0 ? maxObserved * 1.1 : 5
    }

    return Math.max(0, maxBoundary - value)
  }

  // Identity for "Higher is Better" (e.g. Durability)
  return value
}
