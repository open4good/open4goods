import { describe, expect, it } from 'vitest'

import { isNoindexPath, resolveNoindexRobots } from './sitemap-config'

describe('resolveNoindexRobots', () => {
  it.each([
    '/metriks',
    '/prompt',
    '/docs-browser',
    '/comparer',
    '/docs/api/overview',
  ])('marks %s noindex, follow', path => {
    expect(resolveNoindexRobots(path)).toBe('noindex, follow')
  })

  it('marks /rechercher noindex only when it carries a query', () => {
    expect(resolveNoindexRobots('/rechercher', { q: 'lave' })).toBe(
      'noindex, follow'
    )
    expect(resolveNoindexRobots('/rechercher')).toBeNull()
  })

  it('leaves commercial pages indexable', () => {
    expect(resolveNoindexRobots('/')).toBeNull()
    expect(resolveNoindexRobots('/contact')).toBeNull()
    expect(isNoindexPath('/documentation-produit')).toBe(false)
  })
})
