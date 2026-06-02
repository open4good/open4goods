import { describe, expect, it } from 'vitest'

import {
  getLanguageTags,
  isDocVisibleForLocale,
  normalizeSlugOrPath,
  resolveDocPath,
  resolveGuideDocPath,
  resolveLegacyGuideRedirectPath,
  resolvePublicGuidePath,
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

  it('resolves category guide content and public paths', () => {
    expect(
      resolveGuideDocPath({
        verticalId: 'tv',
        guideSlug: 'meilleur-televiseur-caravane-camping-car',
      })
    ).toBe('/guides/tv/meilleur-televiseur-caravane-camping-car')

    expect(
      resolvePublicGuidePath({
        categorySlug: 'televiseurs',
        guideSlug: 'meilleur-televiseur-caravane-camping-car',
      })
    ).toBe('/televiseurs/meilleur-televiseur-caravane-camping-car')
  })

  it('maps legacy guide docs URLs to moved category guide URLs', () => {
    expect(
      resolveLegacyGuideRedirectPath({
        legacyPath: '/docs/fr/guides/meilleur-televiseur-caravane-camping-car',
        movedDocPaths: [
          '/docs/fr/guides/other-guide',
          '/docs/fr/televiseurs/meilleur-televiseur-caravane-camping-car',
        ],
      })
    ).toBe('/televiseurs/meilleur-televiseur-caravane-camping-car')

    expect(
      resolveLegacyGuideRedirectPath({
        legacyPath: '/docs/fr/guides/missing',
        movedDocPaths: [
          '/docs/fr/televiseurs/meilleur-televiseur-caravane-camping-car',
        ],
      })
    ).toBeNull()
  })

  it('extracts and enforces language tags', () => {
    expect(getLanguageTags(['overview', 'language:en', 'LANGUAGE:FR'])).toEqual(
      ['en', 'fr']
    )

    expect(
      isDocVisibleForLocale(
        {
          path: '/docs/en/sample',
          tags: ['language:en', 'guide'],
        },
        'en'
      )
    ).toBe(true)

    expect(
      isDocVisibleForLocale(
        {
          path: '/docs/en/sample',
          tags: ['language:en', 'guide'],
        },
        'fr'
      )
    ).toBe(false)
  })
})
