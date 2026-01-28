import type { AttributeConfigDto } from '~~/shared/api-client'

export interface RadarInversionContext {
  axisId: string
  productValue: number | null
  bestValue: number | null
  worstValue: number | null
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

  // If impactBetterIs == LOWER, then lower values are better.
  // We want "better" to be further from the center in the radar chart.
  // So we invert the scale: new_val = padded_max - old_val
  if (attributeConfig?.impactBetterIs === 'LOWER') {
    const axisValues = [
      context.productValue,
      context.bestValue,
      context.worstValue,
    ].filter((v): v is number => typeof v === 'number' && Number.isFinite(v))

    const maxObserved = axisValues.length ? Math.max(...axisValues) : 5
    const paddedMax = maxObserved > 0 ? maxObserved * 1.1 : 5

    return Math.max(0, paddedMax - value)
  }

  return value
}
