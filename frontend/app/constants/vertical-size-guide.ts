/**
 * Per-vertical "size guide" editorial link (GOU-2718).
 *
 * A vertical listed here shows a link to its size-choice blog post on the
 * category page and a short "which size?" block on product pages. Texts live
 * in i18n under `i18nPrefix`; nothing vertical-specific is hardcoded in the
 * generic components.
 */
export interface VerticalSizeGuideConfig {
  /** Blog route of the article, without locale prefix. */
  readonly postPath: string
  /** i18n namespace holding `categoryLink`, `productTitle`, `productText`, `productTextWithDiagonal`, `productLink`. */
  readonly i18nPrefix: string
  /** Indexed attribute holding the screen diagonal, in inches. */
  readonly diagonalInchesAttribute: string
}

const INCH_IN_CM = 2.54

export const VERTICAL_SIZE_GUIDES: Readonly<
  Record<string, VerticalSizeGuideConfig>
> = {
  tv: {
    postPath: '/blog/quelle-taille-de-tv-choisir-pour-son-salon',
    i18nPrefix: 'verticalSizeGuide.tv',
    diagonalInchesAttribute: 'DIAGONALE_POUCES',
  },
}

export const resolveVerticalSizeGuide = (
  verticalId?: string | null
): VerticalSizeGuideConfig | null => {
  const id = verticalId?.trim()
  return id ? (VERTICAL_SIZE_GUIDES[id] ?? null) : null
}

/** Converts an inch diagonal to whole centimetres, or null when absent/invalid. */
export const inchesToCm = (inches?: number | null): number | null =>
  typeof inches === 'number' && Number.isFinite(inches) && inches > 0
    ? Math.round(inches * INCH_IN_CM)
    : null
