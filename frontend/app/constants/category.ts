import type { CategoryViewMode } from '../utils/_category-filter-state'

export const CATEGORY_VIEW_MODES: CategoryViewMode[] = [
  'cards',
  'list',
  'table',
]

export const CATEGORY_PAGE_SIZES: Record<CategoryViewMode, number> = {
  cards: 30,
  list: 15,
  table: 60,
}

export const CATEGORY_DEFAULT_VIEW_MODE: CategoryViewMode = 'cards'
