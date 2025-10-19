import type { Filter } from '~~/shared/api-client'

export interface CategorySubsetClause {
  id: string
  subsetId: string
  filter: Filter
  index: number
  label: string
}
