import type { Filter } from '~~/shared/api-client'

export interface BlogOfferBlock {
  /** Stable id, used in analytics and i18n-free keys. */
  id: string
  /** i18n key of the block title. */
  titleKey: string
  filters: Filter[]
}

export interface BlogOfferBlocksConfig {
  vertical: string
  /** Category page the "see all" link points to. */
  categoryPath: string
  blocks: BlogOfferBlock[]
}

const DIAGONAL = 'attributes.indexed.DIAGONALE_POUCES.numericValue'
const OVEN_CAPACITY = 'attributes.indexed.OVEN_CAPACITY.numericValue'
const OVEN_CLEANING = 'attributes.indexed.OVEN_CLEANING_MODE.value'

const range = (field: string, min?: number, max?: number): Filter => ({
  field,
  operator: 'range',
  ...(min === undefined ? {} : { min }),
  ...(max === undefined ? {} : { max }),
})

const ovenBlocks = (blocks: BlogOfferBlock[]): BlogOfferBlocksConfig => ({
  vertical: 'oven',
  categoryPath: '/fours',
  blocks,
})

// The API `range` filter is inclusive on both bounds and capacities can be
// decimal: adjacent blocks use a 0.01 L gap so no product falls in two blocks.
const OVEN_VOLUME_BLOCKS: BlogOfferBlock[] = [
  {
    id: 'compact',
    titleKey: 'blog.offerBlocks.oven.compact',
    filters: [range(OVEN_CAPACITY, undefined, 59.99)],
  },
  {
    id: 'standard',
    titleKey: 'blog.offerBlocks.oven.standard',
    filters: [range(OVEN_CAPACITY, 60, 72)],
  },
  {
    id: 'large',
    titleKey: 'blog.offerBlocks.oven.large',
    filters: [range(OVEN_CAPACITY, 72.01)],
  },
]

/** Top-rated-with-offers blocks, indexed by blog article slug. */
export const blogOfferBlocksBySlug: Record<string, BlogOfferBlocksConfig> = {
  'quelle-taille-de-tv-choisir-pour-son-salon': {
    vertical: 'tv',
    categoryPath: '/televiseurs',
    blocks: [
      {
        id: 'tv-43-50',
        titleKey: 'blog.offerBlocks.tv.small',
        filters: [range(DIAGONAL, 43, 50)],
      },
      {
        id: 'tv-55',
        titleKey: 'blog.offerBlocks.tv.medium',
        filters: [range(DIAGONAL, 55, 55)],
      },
      {
        id: 'tv-65-plus',
        titleKey: 'blog.offerBlocks.tv.large',
        filters: [range(DIAGONAL, 65)],
      },
    ],
  },
  'quel-volume-de-four-choisir': ovenBlocks(OVEN_VOLUME_BLOCKS),
  'quel-systeme-choisir-entre-four-pyrolyse-et-catalyse': ovenBlocks([
    {
      id: 'pyrolysis',
      titleKey: 'blog.offerBlocks.oven.pyrolysis',
      filters: [
        { field: OVEN_CLEANING, operator: 'term', terms: ['PYROLYSE'] },
      ],
    },
    {
      id: 'catalysis',
      titleKey: 'blog.offerBlocks.oven.catalysis',
      filters: [
        { field: OVEN_CLEANING, operator: 'term', terms: ['CATALYSE'] },
      ],
    },
  ]),
}

export const getBlogOfferBlocks = (
  slug?: string | null
): BlogOfferBlocksConfig | null =>
  slug ? (blogOfferBlocksBySlug[slug] ?? null) : null
