export type OpenDataExploreCategoryId =
  'tv' | 'washing-machine' | 'dishwasher' | 'oven' | 'air-conditioner'

export interface OpenDataExploreCategory {
  /** Vertical id used by the products search API */
  id: OpenDataExploreCategoryId
  /** `verticalHomeUrl` per domain language (see verticals/*.yml) */
  slugs: { fr: string; en: string }
}

export interface OpenDataExploreProduct {
  gtin: string
  name: string
  /** Fully qualified product path (vertical home URL + product slug) */
  href: string
}

export interface OpenDataExploreCategoryProducts {
  id: OpenDataExploreCategoryId
  products: OpenDataExploreProduct[]
}

export const OPENDATA_EXPLORE_CATEGORIES: OpenDataExploreCategory[] = [
  { id: 'tv', slugs: { fr: 'televiseurs', en: 'tvs' } },
  {
    id: 'washing-machine',
    slugs: { fr: 'lave-linges', en: 'washing-machines' },
  },
  { id: 'dishwasher', slugs: { fr: 'lave-vaisselles', en: 'dishwashers' } },
  { id: 'oven', slugs: { fr: 'fours', en: 'ovens' } },
  {
    id: 'air-conditioner',
    slugs: { fr: 'climatiseurs', en: 'air-conditioners' },
  },
]

export const OPENDATA_EXPLORE_PRODUCTS_PER_CATEGORY = 5
