export interface BlogArticle {
  id: number
  title: string
  body: string
  userId: number
}

export interface BlogArticleResponse {
  articles: BlogArticle[]
}

export interface PageInfo {
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export interface BlogArticleData {
  url: string
  title: string
  author: string
  summary: string
  body: string
  category: string[]
  image: string
  editLink: string
  createdMs: number
  modifiedMs: number
}

export interface PaginatedBlogResponse {
  page: PageInfo
  data: BlogArticleData[]
}
