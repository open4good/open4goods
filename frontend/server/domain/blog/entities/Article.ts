/**
 * Blog Article domain entity
 * Represents a blog post with business logic
 */
export interface Article {
  id: string
  slug: string
  title: string
  excerpt: string
  content: string
  author: string
  publishedAt: Date
  updatedAt: Date
  tags: string[]
  imageUrl?: string
  readTime?: number
}

/**
 * Factory function to create an Article from raw data
 */
export const createArticle = (data: {
  id: string
  slug: string
  title: string
  excerpt: string
  content: string
  author: string
  publishedAt: string | Date
  updatedAt: string | Date
  tags: string[]
  imageUrl?: string
  readTime?: number
}): Article => {
  return {
    id: data.id,
    slug: data.slug.toLowerCase().trim(),
    title: data.title.trim(),
    excerpt: data.excerpt.trim(),
    content: data.content,
    author: data.author.trim(),
    publishedAt:
      typeof data.publishedAt === 'string'
        ? new Date(data.publishedAt)
        : data.publishedAt,
    updatedAt:
      typeof data.updatedAt === 'string'
        ? new Date(data.updatedAt)
        : data.updatedAt,
    tags: data.tags.map(tag => tag.trim()),
    imageUrl: data.imageUrl?.trim(),
    readTime: data.readTime,
  }
}

/**
 * Business logic: Check if article is recently published (within last 7 days)
 */
export const isRecentArticle = (article: Article): boolean => {
  const sevenDaysAgo = new Date()
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7)
  return article.publishedAt >= sevenDaysAgo
}

/**
 * Business logic: Check if article has a specific tag
 */
export const hasTag = (article: Article, tag: string): boolean => {
  return article.tags.some(t => t.toLowerCase() === tag.toLowerCase())
}
