import type { SortRequestDto } from '~~/shared/api-client'
import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'

/**
 * Sort sent with the server-rendered category listing: the ImpactScore, best
 * first, i.e. the default sort the toolbar shows as "(par défaut)". Without it
 * the first paint came back in index order while the toolbar claimed an
 * ImpactScore order (GOU-1940).
 */
export const CATEGORY_INITIAL_SORT_FIELD = ECOSCORE_RELATIVE_FIELD
export const CATEGORY_INITIAL_SORT_ORDER = 'desc' as const

export const CATEGORY_INITIAL_SORT: SortRequestDto = {
  sorts: [
    { field: CATEGORY_INITIAL_SORT_FIELD, order: CATEGORY_INITIAL_SORT_ORDER },
  ],
}

/**
 * Whether the server-rendered listing (sorted with {@link CATEGORY_INITIAL_SORT})
 * still matches the sort applied after hydration, so it can be reused instead
 * of being fetched again.
 */
export const matchesCategoryInitialSort = (
  sortField: string | null,
  sortOrder: 'asc' | 'desc'
): boolean =>
  sortField === CATEGORY_INITIAL_SORT_FIELD &&
  sortOrder === CATEGORY_INITIAL_SORT_ORDER
