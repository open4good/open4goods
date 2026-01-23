import { describe, expect, it } from 'vitest'
import type { AiReviewDto } from '~~/shared/api-client'
import { applyAiReviewReferenceLinks } from './ai-review-references'

describe('applyAiReviewReferenceLinks', () => {
  it('links only references that exist in the sources list', () => {
    const review: AiReviewDto = {
      summary: 'Highlights [1, 2, 3].',
      technicalOneline: 'Specs [2].',
      sources: [
        { number: 1, url: 'https://example.com/1' },
        { number: 3, url: 'https://example.com/3' },
      ],
    }

    applyAiReviewReferenceLinks(review)

    expect(review.summary).toBe(
      'Highlights <a class="review-ref" href="#review-ref-1">[1]</a>, <a class="review-ref" href="#review-ref-3">[3]</a>.'
    )
    expect(review.technicalOneline).toBe('Specs .')
  })

  it('drops reference markers when no source matches', () => {
    const review: AiReviewDto = {
      ecologicalReview: 'Eco notes [4].',
      sources: [{ number: 2, url: 'https://example.com/2' }],
    }

    applyAiReviewReferenceLinks(review)

    expect(review.ecologicalReview).toBe('Eco notes .')
  })
})
