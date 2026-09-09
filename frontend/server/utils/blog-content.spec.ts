import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  findBlogDocBySlug,
  listBlogDocs,
  toBlogPostDto,
  toBlogTagDtos,
  type BlogContentDoc,
} from './blog-content'

const readFileMock = vi.hoisted(() => vi.fn())
const queryCollectionMock = vi.hoisted(() => vi.fn())

vi.mock('node:fs/promises', () => ({
  __esModule: true,
  readFile: readFileMock,
  default: { readFile: readFileMock },
}))
vi.mock('@nuxt/content/server', () => ({
  queryCollection: queryCollectionMock,
}))

const doc = (overrides: Partial<BlogContentDoc>): BlogContentDoc => ({
  path: '/blog/fr/some-post',
  title: 'Some post',
  description: 'A description',
  author: 'o4g',
  language: 'fr',
  tags: ['ESG'],
  date: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-02T00:00:00Z',
  draft: false,
  published: true,
  ...overrides,
})

const fakeEvent = {} as Parameters<typeof listBlogDocs>[0]

describe('blog-content', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listBlogDocs', () => {
    it('drops drafts and unpublished posts, sorted newest first', async () => {
      const all = vi.fn().mockResolvedValue([
        doc({ path: '/blog/fr/oldest', date: '2025-01-01T00:00:00Z' }),
        doc({ path: '/blog/fr/draft', draft: true }),
        doc({ path: '/blog/fr/unpublished', published: false }),
        doc({ path: '/blog/fr/newest', date: '2026-06-01T00:00:00Z' }),
      ])
      queryCollectionMock.mockReturnValue({ all })

      const result = await listBlogDocs(fakeEvent)

      expect(result.map(d => d.path)).toEqual(['/blog/fr/newest', '/blog/fr/oldest'])
      expect(queryCollectionMock).toHaveBeenCalledWith(fakeEvent, 'blog')
    })
  })

  describe('findBlogDocBySlug', () => {
    it('matches on the last path segment regardless of language prefix depth', async () => {
      const all = vi.fn().mockResolvedValue([
        doc({ path: '/blog/fr/a-quoi-sert-l-esg' }),
      ])
      queryCollectionMock.mockReturnValue({ all })

      const found = await findBlogDocBySlug(fakeEvent, 'a-quoi-sert-l-esg')
      expect(found?.path).toBe('/blog/fr/a-quoi-sert-l-esg')

      const notFound = await findBlogDocBySlug(fakeEvent, 'does-not-exist')
      expect(notFound).toBeNull()
    })
  })

  describe('toBlogPostDto', () => {
    it('maps dates to epoch ms and slug to the url field', async () => {
      readFileMock.mockResolvedValue('---\ntitle: x\n---\nHello **world**')

      const dto = await toBlogPostDto(doc({ path: '/blog/fr/hello-world' }))

      expect(dto.url).toBe('hello-world')
      expect(dto.createdMs).toBe(Date.parse('2026-01-01T00:00:00Z'))
      expect(dto.modifiedMs).toBe(Date.parse('2026-01-02T00:00:00Z'))
      expect(dto.body).toContain('<strong>world</strong>')
      expect(dto.editLink).toBeUndefined()
    })

    it('unwraps a bare [assistant] marker out of its markdown-it paragraph wrapper', async () => {
      readFileMock.mockResolvedValue('---\ntitle: x\n---\nBefore\n\n[assistant]\n\nAfter')

      const dto = await toBlogPostDto(doc({}))

      expect(dto.body).toContain('[assistant]')
      expect(dto.body).not.toContain('<p>[assistant]</p>')
    })
  })

  describe('toBlogTagDtos', () => {
    it('counts tag occurrences across docs', () => {
      const result = toBlogTagDtos([
        doc({ tags: ['ESG', 'News'] }),
        doc({ tags: ['ESG'] }),
      ])

      expect(result).toEqual(
        expect.arrayContaining([
          { name: 'ESG', count: 2 },
          { name: 'News', count: 1 },
        ])
      )
    })
  })
})
