/**
 * Query object for fetching paginated articles
 * Follows CQRS pattern - represents a read operation
 */
export interface GetArticlesQuery {
  pageNumber?: number
  pageSize?: number
  tag?: string
}
