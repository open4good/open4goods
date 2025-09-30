/**
 * Generic paginated result entity
 */
export interface Page<T> {
  data: T[]
  page: PageMetadata
}

export interface PageMetadata {
  number: number // 0-based page number
  size: number
  totalElements: number
  totalPages: number
}

/**
 * Factory function to create a Page
 */
export const createPage = <T>(data: T[], metadata: PageMetadata): Page<T> => {
  return {
    data,
    page: {
      number: Math.max(0, metadata.number),
      size: Math.max(1, metadata.size),
      totalElements: Math.max(0, metadata.totalElements),
      totalPages: Math.max(1, metadata.totalPages),
    },
  }
}

/**
 * Business logic: Check if page is the first page
 */
export const isFirstPage = <T>(page: Page<T>): boolean => {
  return page.page.number === 0
}

/**
 * Business logic: Check if page is the last page
 */
export const isLastPage = <T>(page: Page<T>): boolean => {
  return page.page.number >= page.page.totalPages - 1
}

/**
 * Business logic: Check if page has results
 */
export const hasResults = <T>(page: Page<T>): boolean => {
  return page.data.length > 0
}
