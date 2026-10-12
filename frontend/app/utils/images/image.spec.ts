import { describe, expect, it } from 'vitest'

import { DEFAULT_IMAGE, getImageUrl, isValidImageUrl } from './image'

describe('isValidImageUrl', () => {
  it('accepts absolute http(s) URLs', () => {
    expect(isValidImageUrl('https://example.com/image.png')).toBe(true)
    expect(isValidImageUrl('http://example.com/image.png')).toBe(true)
  })

  it('accepts site-root-relative paths', () => {
    expect(
      isValidImageUrl(
        '/images/blog/impact-ecologique-des-televiseurs/1716413022905-815.png'
      )
    ).toBe(true)
  })

  it('rejects protocol-relative URLs', () => {
    expect(isValidImageUrl('//example.com/image.png')).toBe(false)
  })

  it('rejects empty, non-string or malformed values', () => {
    expect(isValidImageUrl('')).toBe(false)
    expect(isValidImageUrl('not a url')).toBe(false)
  })
})

describe('getImageUrl', () => {
  it('returns a root-relative path untouched', () => {
    expect(getImageUrl('/images/blog/some-slug/cover.png')).toBe(
      '/images/blog/some-slug/cover.png'
    )
  })

  it('falls back to the default image for an invalid url', () => {
    expect(getImageUrl('not-a-url')).toBe(DEFAULT_IMAGE)
  })

  it('falls back to a custom fallback when provided', () => {
    expect(getImageUrl('not-a-url', '/custom-fallback.png')).toBe(
      '/custom-fallback.png'
    )
  })
})
