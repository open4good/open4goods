import { describe, expect, it } from 'vitest'
import { isBlogArticlePath } from './blog-route'

describe('isBlogArticlePath', () => {
  it('accepts blog article paths', () => {
    expect(isBlogArticlePath('/blog/quel-volume-de-four-choisir')).toBe(true)
  })

  it('rejects product pages and the blog index', () => {
    expect(isBlogArticlePath('/televiseurs/some-product-123')).toBe(false)
    expect(isBlogArticlePath('/blog')).toBe(false)
    expect(isBlogArticlePath('/blog/')).toBe(false)
  })
})
