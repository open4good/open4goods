/**
 * Viewing distance → TV size (GOU-2713). Pure functions, shared by the SSR render and the client
 * recalculation of `TvSizeCalculator.vue`.
 */
export type TvResolution = '4k' | 'full-hd'

export const TV_DISTANCE_MIN_M = 1
export const TV_DISTANCE_MAX_M = 6
export const TV_DISTANCE_DEFAULT_M = 3
export const TV_DEFAULT_RESOLUTION: TvResolution = '4k'

/** Screen sizes (inches) sold on the market, the recommendation is one of them. */
export const TV_STANDARD_SIZES = [32, 40, 43, 50, 55, 65, 75, 85] as const

/** Divisors of the viewing distance (cm) giving the largest then the smallest comfortable diagonal. */
const DISTANCE_DIVISORS: Record<TvResolution, { min: number; max: number }> = {
  '4k': { min: 1.6, max: 1.2 },
  'full-hd': { min: 2.0, max: 1.6 },
}

const CM_PER_INCH = 2.54
/** 16:9 screen: width = d × 16/√337, height = d × 9/√337 (16² + 9² = 337). */
const DIAGONAL_RATIO = Math.sqrt(337)

/**
 * Sub-category (`id` of `verticals/.../tv.yml`) listing the TVs of a standard size. Single source
 * of the size → link mapping: GOU-2715 will point 32", 50" and 75" to their own sub-categories.
 * The URL itself is built from the category detail (`verticalHomeUrl` + sub-category `slug`).
 */
export const TV_SIZE_SUBCATEGORY: Record<number, string> = {
  32: 'medium_screens',
  40: 'medium_screens',
  43: 'medium_screens',
  50: 'medium_screens',
  55: 'large_screens',
  65: 'large_screens',
  75: 'large_screens',
  85: 'large_screens',
}

export interface TvSizeResult {
  distanceM: number
  resolution: TvResolution
  minCm: number
  maxCm: number
  minInches: number
  maxInches: number
  /** Standard size (inches) closest to the middle of the range. */
  recommendedInches: number
  /** `below` / `above` when the whole range is outside the standard sizes. */
  beyond: 'below' | 'above' | null
  /** Dimensions (cm) of a 16:9 screen of the recommended size. */
  widthCm: number
  heightCm: number
}

/** Clamps to 1–6 m; a non-numeric value falls back to the default distance. */
export function clampDistance(distanceM: number): number {
  if (!Number.isFinite(distanceM)) {
    return TV_DISTANCE_DEFAULT_M
  }
  return Math.min(TV_DISTANCE_MAX_M, Math.max(TV_DISTANCE_MIN_M, distanceM))
}

const round1 = (value: number) => Math.round(value * 10) / 10

export function computeTvSize(
  distanceM: number,
  resolution: TvResolution = TV_DEFAULT_RESOLUTION
): TvSizeResult {
  const distance = clampDistance(distanceM)
  const divisors = DISTANCE_DIVISORS[resolution] ?? DISTANCE_DIVISORS['4k']
  const distanceCm = distance * 100
  const minCm = distanceCm / divisors.min
  const maxCm = distanceCm / divisors.max
  const centerInches = (minCm + maxCm) / 2 / CM_PER_INCH

  const recommendedInches = TV_STANDARD_SIZES.reduce((best, size) =>
    Math.abs(size - centerInches) < Math.abs(best - centerInches) ? size : best
  )
  const smallest = TV_STANDARD_SIZES[0]
  const largest = TV_STANDARD_SIZES[TV_STANDARD_SIZES.length - 1]!
  const maxInches = maxCm / CM_PER_INCH
  const minInches = minCm / CM_PER_INCH

  const recommendedCm = recommendedInches * CM_PER_INCH
  return {
    distanceM: distance,
    resolution,
    minCm: round1(minCm),
    maxCm: round1(maxCm),
    minInches: Math.round(minInches),
    maxInches: Math.round(maxInches),
    recommendedInches,
    beyond:
      maxInches < smallest ? 'below' : minInches > largest ? 'above' : null,
    widthCm: round1((recommendedCm * 16) / DIAGONAL_RATIO),
    heightCm: round1((recommendedCm * 9) / DIAGONAL_RATIO),
  }
}
