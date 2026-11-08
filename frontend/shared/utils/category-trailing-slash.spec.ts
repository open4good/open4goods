import { describe, expect, it } from 'vitest'

import { resolveCategoryTrailingSlashTarget } from './category-trailing-slash'

describe('resolveCategoryTrailingSlashTarget', () => {
  it('strips the trailing slash of a category path', () => {
    expect(resolveCategoryTrailingSlashTarget('/televiseurs/')).toBe(
      '/televiseurs'
    )
    expect(resolveCategoryTrailingSlashTarget('/lave-vaisselle/')).toBe(
      '/lave-vaisselle'
    )
  })

  it.each([
    '/',
    '/televiseurs',
    '/blog/x/',
    '/contrib/abc/',
    '/contrib/',
    '/123456789-fiche/',
    '/televiseurs/123456789-fiche/',
    '/Televiseurs/',
  ])('leaves %s untouched', path => {
    expect(resolveCategoryTrailingSlashTarget(path)).toBeNull()
  })
})
