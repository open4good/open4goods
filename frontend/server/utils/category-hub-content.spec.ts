import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { CategoryHubDoc } from '~~/shared/utils/category-hub'

import { findCategoryHubDoc, toCategoryHubDto } from './category-hub-content'

const queryCollectionMock = vi.hoisted(() => vi.fn())

vi.mock('@nuxt/content/server', () => ({
  queryCollection: queryCollectionMock,
}))

const intro = Array.from({ length: 130 }, () => 'mot').join(' ')
const faq = [1, 2, 3, 4].map(index => ({
  question: ` Question ${index} ? `,
  answer: `Réponse ${index}.`,
}))

const doc = (overrides: Partial<CategoryHubDoc> = {}): CategoryHubDoc => ({
  verticalId: 'tv',
  language: 'fr',
  published: true,
  intro,
  faq,
  subCategories: ['televiseur-55-pouces'],
  guides: [{ slug: 'guide-tv', label: 'Guide TV' }],
  ...overrides,
})

const fakeEvent = {} as Parameters<typeof findCategoryHubDoc>[0]

const mockDocs = (docs: CategoryHubDoc[]) => {
  const where = vi.fn()
  const query = { where, all: vi.fn().mockResolvedValue(docs) }
  where.mockReturnValue(query)
  queryCollectionMock.mockReturnValue(query)
  return where
}

describe('category-hub-content', () => {
  beforeEach(() => {
    queryCollectionMock.mockReset()
  })

  it('queries the hub of the vertical in the requested language', async () => {
    const where = mockDocs([doc()])

    await expect(findCategoryHubDoc(fakeEvent, 'tv', 'fr')).resolves.toEqual(
      doc()
    )
    expect(queryCollectionMock).toHaveBeenCalledWith(fakeEvent, 'categoryHubs')
    expect(where).toHaveBeenCalledWith('verticalId', '=', 'tv')
    expect(where).toHaveBeenCalledWith('language', '=', 'fr')
  })

  it('returns null for a vertical without file', async () => {
    mockDocs([])

    await expect(
      findCategoryHubDoc(fakeEvent, 'oven', 'fr')
    ).resolves.toBeNull()
  })

  it('hides a published: false file unless preview is on', async () => {
    mockDocs([doc({ published: false })])

    await expect(findCategoryHubDoc(fakeEvent, 'tv', 'fr')).resolves.toBeNull()
    await expect(
      findCategoryHubDoc(fakeEvent, 'tv', 'fr', { includeUnpublished: true })
    ).resolves.toMatchObject({ published: false })
  })

  it('never serves an invalid file', async () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
    mockDocs([doc({ faq: faq.slice(0, 3) })])

    await expect(findCategoryHubDoc(fakeEvent, 'tv', 'fr')).resolves.toBeNull()
    expect(consoleError).toHaveBeenCalledWith(
      expect.stringContaining('faq has 3 items')
    )
    consoleError.mockRestore()
  })

  it('renders the intro Markdown without raw HTML and trims the FAQ', () => {
    const dto = toCategoryHubDto(
      doc({ intro: '**Bien** choisir <script>alert(1)</script>' })
    )

    expect(dto.introHtml).toContain('<strong>Bien</strong>')
    expect(dto.introHtml).not.toContain('<script>')
    expect(dto.faq[0]).toEqual({
      question: 'Question 1 ?',
      answer: 'Réponse 1.',
    })
    expect(dto.title).toBeNull()
  })
})
