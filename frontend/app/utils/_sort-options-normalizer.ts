export type SortFieldItem = {
  title: string
  value: string
}

const normalizeSortTitle = (value: string): string => {
  return value
    .trim()
    .replace(/\s+/g, ' ')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
}

/**
 * Deduplicates sort options by normalized visible title while preserving the
 * first option order from the provided list.
 */
export const deduplicateSortItemsByTitle = (
  items: SortFieldItem[]
): SortFieldItem[] => {
  const byTitle = new Map<string, SortFieldItem>()

  items.forEach(item => {
    const normalizedTitle = normalizeSortTitle(item.title)

    if (!byTitle.has(normalizedTitle)) {
      byTitle.set(normalizedTitle, item)
    }
  })

  return Array.from(byTitle.values())
}
