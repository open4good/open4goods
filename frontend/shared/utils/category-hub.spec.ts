import { describe, expect, it } from 'vitest'

import {
  buildCategoryHubFaqJsonLd,
  countCategoryHubIntroWords,
  validateCategoryHubDoc,
  type CategoryHubDoc,
} from './category-hub'

const words = (count: number) =>
  Array.from({ length: count }, () => 'mot').join(' ')

const faq = [1, 2, 3, 4].map(index => ({
  question: `Question ${index} ?`,
  answer: `Réponse ${index}.`,
}))

const doc = (overrides: Partial<CategoryHubDoc> = {}): CategoryHubDoc => ({
  verticalId: 'tv',
  language: 'fr',
  published: true,
  intro: words(150),
  faq,
  subCategories: [],
  guides: [],
  ...overrides,
})

describe('countCategoryHubIntroWords', () => {
  it('counts the words a reader sees, not the Markdown syntax', () => {
    expect(
      countCategoryHubIntroWords(
        "**Bien** choisir : voir le [guide d'achat](/televiseurs/guide) - 55 pouces."
      )
    ).toBe(8)
  })
})

describe('validateCategoryHubDoc', () => {
  it('accepts 120 to 180 words and 4 questions', () => {
    expect(validateCategoryHubDoc(doc({ intro: words(120) }))).toEqual([])
    expect(validateCategoryHubDoc(doc({ intro: words(180) }))).toEqual([])
  })

  it('rejects an intro outside the word range', () => {
    expect(validateCategoryHubDoc(doc({ intro: words(119) }))[0]).toContain(
      '119 words'
    )
    expect(validateCategoryHubDoc(doc({ intro: words(181) }))[0]).toContain(
      '181 words'
    )
  })

  it('rejects a FAQ without exactly 4 complete questions', () => {
    expect(validateCategoryHubDoc(doc({ faq: faq.slice(0, 3) }))).toEqual([
      'faq has 3 items, expected 4',
    ])
    expect(
      validateCategoryHubDoc(
        doc({ faq: [...faq.slice(0, 3), { question: 'Q ?', answer: ' ' }] })
      )
    ).toEqual(['faq[3] needs a question and an answer'])
  })
})

describe('buildCategoryHubFaqJsonLd', () => {
  it('builds a FAQPage with one Question and acceptedAnswer per visible item', () => {
    expect(buildCategoryHubFaqJsonLd(faq)).toEqual({
      '@context': 'https://schema.org',
      '@type': 'FAQPage',
      mainEntity: faq.map(item => ({
        '@type': 'Question',
        name: item.question,
        acceptedAnswer: { '@type': 'Answer', text: item.answer },
      })),
    })
  })

  it('returns null without questions', () => {
    expect(buildCategoryHubFaqJsonLd([])).toBeNull()
  })
})
