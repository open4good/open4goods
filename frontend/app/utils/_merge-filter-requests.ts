import type { FilterRequestDto } from '~~/shared/api-client'

export const mergeFilterRequests = (
  primary?: FilterRequestDto,
  secondary?: FilterRequestDto,
): FilterRequestDto | undefined => {
  const filters = [...(primary?.filters ?? []), ...(secondary?.filters ?? [])]
  const mergedMust = [...(primary?.filterGroups ?? []), ...(secondary?.filterGroups ?? [])]
    .flatMap((group) => group.must ?? group.filters ?? [])
  const mergedShould = [...(primary?.filterGroups ?? []), ...(secondary?.filterGroups ?? [])]
    .flatMap((group) => group.should ?? [])

  const filterGroups = mergedMust.length || mergedShould.length
    ? [{ ...(mergedMust.length ? { must: mergedMust } : {}), ...(mergedShould.length ? { should: mergedShould } : {}) }]
    : []

  if (!filters.length && !filterGroups.length) {
    return undefined
  }

  return {
    ...(filters.length ? { filters } : {}),
    ...(filterGroups.length ? { filterGroups } : {}),
  }
}
