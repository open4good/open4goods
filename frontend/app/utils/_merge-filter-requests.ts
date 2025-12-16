import type { FilterGroup, FilterRequestDto } from '~~/shared/api-client'

export const mergeFilterRequests = (
  primary?: FilterRequestDto,
  secondary?: FilterRequestDto
): FilterRequestDto | undefined => {
  const filters = [...(primary?.filters ?? []), ...(secondary?.filters ?? [])]
  const filterGroups = [
    ...(primary?.filterGroups ?? []),
    ...(secondary?.filterGroups ?? []),
  ]
    .map(group => {
      const mustClauses = [...(group.must ?? group.filters ?? [])].filter(
        Boolean
      )
      const shouldClauses = [...(group.should ?? [])].filter(Boolean)

      const normalized: FilterGroup | null =
        mustClauses.length || shouldClauses.length
          ? {
              ...(mustClauses.length ? { must: mustClauses } : {}),
              ...(shouldClauses.length ? { should: shouldClauses } : {}),
            }
          : null

      return normalized
    })
    .filter((group): group is FilterGroup => Boolean(group))
    .filter((group, index, self) => {
      const signature = JSON.stringify(group)
      return (
        self.findIndex(candidate => JSON.stringify(candidate) === signature) ===
        index
      )
    })

  if (!filters.length && !filterGroups.length) {
    return undefined
  }

  return {
    ...(filters.length ? { filters } : {}),
    ...(filterGroups.length ? { filterGroups } : {}),
  }
}
