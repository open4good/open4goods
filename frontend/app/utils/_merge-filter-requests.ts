import type { FilterRequestDto } from '~~/shared/api-client'

export const mergeFilterRequests = (
  primary?: FilterRequestDto,
  secondary?: FilterRequestDto,
): FilterRequestDto | undefined => {
  const filters = [...(primary?.filters ?? []), ...(secondary?.filters ?? [])]
  const filterGroups = [...(primary?.filterGroups ?? []), ...(secondary?.filterGroups ?? [])]

  if (!filters.length && !filterGroups.length) {
    return undefined
  }

  return {
    ...(filters.length ? { filters } : {}),
    ...(filterGroups.length ? { filterGroups } : {}),
  }
}
