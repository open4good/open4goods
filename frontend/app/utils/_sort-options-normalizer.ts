export type SortFieldItem = {
  title: string
  value: string
}

const normalizeSortTitle = (value: string): string => {
  return value.trim().replace(/\s+/g, ' ').toLowerCase()
}

/**
 * Deduplicates sort options by normalized visible title while preserving the
 * highest-priority option for each title.
 */
export const deduplicateSortItemsByTitle = (
  items: SortFieldItem[],
  computePriority: (item: SortFieldItem) => number
): SortFieldItem[] => {
  const byTitle = new Map<string, { item: SortFieldItem; priority: number }>()

  items.forEach(item => {
    const normalizedTitle = normalizeSortTitle(item.title)
    const current = byTitle.get(normalizedTitle)
    const priority = computePriority(item)

    if (!current || priority > current.priority) {
      byTitle.set(normalizedTitle, { item, priority })
    }
  })

  return Array.from(byTitle.values()).map(entry => entry.item)
}
