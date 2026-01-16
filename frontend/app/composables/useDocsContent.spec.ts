import { describe, expect, it } from 'vitest'

import {
  normalizeSlugOrPath,
  resolveDocPath,
} from '~/composables/useDocsContent'

describe('useDocsContent helpers', () => {
  it('normalizes slug input safely', () => {
    expect(normalizeSlugOrPath('getting-started/intro')).toBe(
      'getting-started/intro'
    )
    expect(normalizeSlugOrPath('/docs/en/getting-started/intro')).toBe(
      'docs/en/getting-started/intro'
    )
    expect(normalizeSlugOrPath('../secret')).toBe('')
    expect(normalizeSlugOrPath('docs/en/guide?ref=nav')).toBe('docs/en/guide')
  })

  it('resolves doc paths with locale and base path', () => {
    expect(
      resolveDocPath({
        locale: 'en',
        basePath: '/docs',
        slugOrPath: 'impact-score/overview',
      })
    ).toBe('/docs/en/impact-score/overview')

    expect(
      resolveDocPath({
        locale: 'fr',
        basePath: '/docs',
        slugOrPath: '/docs/fr/impact-score/methodology',
      })
    ).toBe('/docs/fr/impact-score/methodology')
  })
})
