/**
 * Page numbers to render (1-based); `null` stands for an ellipsis.
 * Always keeps the first and last pages plus a window around the current one.
 */
export const buildPaginationItems = (
  length: number,
  current: number,
  siblings = 2
): Array<number | null> => {
  if (length <= 1) {
    return []
  }

  const pages = new Set<number>([1, length])
  for (let page = current - siblings; page <= current + siblings; page += 1) {
    if (page >= 1 && page <= length) {
      pages.add(page)
    }
  }

  const sorted = [...pages].sort((a, b) => a - b)
  const items: Array<number | null> = []
  sorted.forEach((page, index) => {
    const previous = sorted[index - 1]
    if (previous !== undefined && page - previous > 1) {
      items.push(page - previous === 2 ? page - 1 : null)
    }
    items.push(page)
  })
  return items
}
