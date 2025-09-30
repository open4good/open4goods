/**
 * Blog Tag domain entity
 */
export interface Tag {
  name: string
  slug: string
  count: number
}

/**
 * Factory function to create a Tag from raw data
 */
export const createTag = (data: {
  name: string
  slug: string
  count: number
}): Tag => {
  return {
    name: data.name.trim(),
    slug: data.slug.toLowerCase().trim(),
    count: Math.max(0, data.count), // Ensure non-negative
  }
}

/**
 * Business logic: Check if tag is popular (more than N articles)
 */
export const isPopularTag = (tag: Tag, threshold: number = 5): boolean => {
  return tag.count >= threshold
}
