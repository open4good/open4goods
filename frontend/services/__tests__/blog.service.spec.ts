import { beforeEach, describe, expect, it, vi } from 'vitest'
import { BlogService } from '../blog.services'
import type { BlogApi } from '~/src/api/apis/BlogApi'

describe('BlogService', () => {
  const mockApi = {
    posts: vi.fn(),
    post: vi.fn(),
  } as unknown as BlogApi
  let service: BlogService

  beforeEach(() => {
    vi.clearAllMocks()
    service = new BlogService(mockApi)
  })

  it('fetches articles from BlogApi', async () => {
    const data = { page: { number: 0 }, data: [] } as any
    ;(mockApi.posts as any).mockResolvedValue(data)
    const result = await service.getArticles()
    expect(mockApi.posts).toHaveBeenCalledTimes(1)
    expect(result).toEqual(data)
  })

  it('fetches article by id', async () => {
    const article = { title: 'Test' } as any
    ;(mockApi.post as any).mockResolvedValue(article)
    const result = await service.getArticleById('slug')
    expect(mockApi.post).toHaveBeenCalledWith({ slug: 'slug' })
    expect(result).toEqual(article)
  })

  it('throws when posts request fails', async () => {
    ;(mockApi.posts as any).mockRejectedValue(new Error('fail'))
    await expect(service.getArticles()).rejects.toThrow('Failed to fetch blog articles')
  })

  it('throws when post request fails', async () => {
    ;(mockApi.post as any).mockRejectedValue(new Error('fail'))
    await expect(service.getArticleById('slug')).rejects.toThrow('Failed to fetch blog article slug')
  })
})
