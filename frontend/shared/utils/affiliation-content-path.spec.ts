import { describe, expect, it } from 'vitest'

import {
  isBlogOrGuideArticlePath,
  sanitizeAffiliationContentPath,
} from './affiliation-content-path'

describe('isBlogOrGuideArticlePath', () => {
  it.each(['/blog/slug', '/guides/four', '/guides/a/b'])('accepts %s', p =>
    expect(isBlogOrGuideArticlePath(p)).toBe(true)
  )
  it.each(['/blog', '/blog/', '/guides', '/', '/fours/x'])('rejects %s', p =>
    expect(isBlogOrGuideArticlePath(p)).toBe(false)
  )
})

describe('sanitizeAffiliationContentPath', () => {
  it('keeps a valid path', () => {
    expect(sanitizeAffiliationContentPath('/blog/slug')).toBe('/blog/slug')
  })
  it.each([
    null,
    undefined,
    3,
    '',
    '/fours/x',
    'https://a/blog/x',
    '/blog/x?a',
  ])('ignores %s', v => expect(sanitizeAffiliationContentPath(v)).toBeNull())
})
