import { beforeEach, describe, expect, it, vi } from 'vitest'

// Provide minimal implementations for Nitro helpers used in the handlers
;(global as any).defineEventHandler = (fn: any) => fn
;(global as any).setResponseHeader = (event: any, name: string, value: string) => {
  event.node.res.setHeader(name, value)
}
;(global as any).createError = ({ statusCode, statusMessage }: any) => {
  const err = new Error(statusMessage) as any
  err.statusCode = statusCode
  return err
}
;(global as any).getRouterParam = (event: any, name: string) => event.context?.params?.[name]

vi.mock('~/services/blog.services', () => ({
  blogService: {
    getArticles: vi.fn(),
    getArticleById: vi.fn(),
  },
}))

import articlesHandler from '../articles'
import articleHandler from '../articles/[id]'
import { blogService } from '~/services/blog.services'

const createEvent = (params: Record<string, string> = {}) => ({
  context: { params },
  node: { res: { setHeader: vi.fn() } },
}) as any

describe('blog proxy endpoints', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('returns articles and sets cache header', async () => {
    const event = createEvent()
    ;(blogService.getArticles as any).mockResolvedValue({ data: [] })

    const result = await articlesHandler(event)
    expect(blogService.getArticles).toHaveBeenCalled()
    expect(event.node.res.setHeader).toHaveBeenCalledWith(
      'Cache-Control',
      'public, max-age=3600, s-maxage=3600'
    )
    expect(result).toEqual({ data: [] })
  })

  it('propagates service error as 500', async () => {
    const event = createEvent()
    ;(blogService.getArticles as any).mockRejectedValue(new Error('fail'))

    await expect(articlesHandler(event)).rejects.toHaveProperty('statusCode', 500)
  })

  it('returns 400 when id missing', async () => {
    const event = createEvent()
    await expect(articleHandler(event)).rejects.toHaveProperty('statusCode', 400)
  })

  it('returns article from service', async () => {
    const event = createEvent({ id: '1' })
    ;(blogService.getArticleById as any).mockResolvedValue({ id: '1' })

    const result = await articleHandler(event)
    expect(blogService.getArticleById).toHaveBeenCalledWith('1')
    expect(result).toEqual({ id: '1' })
  })

  it('propagates article service error as 500', async () => {
    const event = createEvent({ id: '2' })
    ;(blogService.getArticleById as any).mockRejectedValue(new Error('fail'))

    await expect(articleHandler(event)).rejects.toHaveProperty('statusCode', 500)
  })
})
