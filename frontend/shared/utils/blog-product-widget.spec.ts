import { describe, expect, it } from 'vitest'

import {
  parseProductWidgetAttributes,
  parseTvSizeCalculatorAttributes,
  renderTvSizeCalculatorMarker,
  renderProductWidgetMarker,
  splitBlogBody,
} from './blog-product-widget'

describe('blog-product-widget', () => {
  describe('parseProductWidgetAttributes', () => {
    it('reads vertical, subcategory and top', () => {
      expect(
        parseProductWidgetAttributes(
          'vertical="tv" subcategory="tv_55_inches" top="4"'
        )
      ).toEqual({ vertical: 'tv', subcategory: 'tv_55_inches', top: 4 })
    })

    it('defaults top to 3 and ignores unknown attributes', () => {
      expect(
        parseProductWidgetAttributes('vertical="oven" class="x" sort="price"')
      ).toEqual({ vertical: 'oven', top: 3 })
    })

    it('falls back to 3 for an out-of-range or non-numeric top', () => {
      expect(parseProductWidgetAttributes('vertical="tv" top="50"')?.top).toBe(
        3
      )
      expect(parseProductWidgetAttributes('vertical="tv" top="x"')?.top).toBe(3)
    })

    it('drops the widget without a valid vertical or with an invalid subcategory', () => {
      expect(parseProductWidgetAttributes('top="3"')).toBeNull()
      expect(
        parseProductWidgetAttributes('vertical="<script>alert(1)</script>"')
      ).toBeNull()
      expect(
        parseProductWidgetAttributes('vertical="tv" subcategory="a b"')
      ).toBeNull()
    })
  })

  describe('splitBlogBody', () => {
    const marker = renderProductWidgetMarker({
      vertical: 'tv',
      subcategory: 'tv_43_inches',
      top: 3,
    })

    it('returns a body without marker unchanged', () => {
      expect(splitBlogBody('<p>Hello</p>')).toEqual([
        { type: 'html', content: '<p>Hello</p>' },
      ])
    })

    it('splits on widget and assistant markers, in order', () => {
      expect(
        splitBlogBody(`<p>A</p>${marker}<p>B</p>[assistant]<p>C</p>`)
      ).toEqual([
        { type: 'html', content: '<p>A</p>' },
        {
          type: 'product-widget',
          widget: { vertical: 'tv', subcategory: 'tv_43_inches', top: 3 },
        },
        { type: 'html', content: '<p>B</p>' },
        { type: 'assistant' },
        { type: 'html', content: '<p>C</p>' },
      ])
    })

    it('reads a widget without subcategory', () => {
      expect(
        splitBlogBody(renderProductWidgetMarker({ vertical: 'oven', top: 3 }))
      ).toEqual([
        { type: 'product-widget', widget: { vertical: 'oven', top: 3 } },
      ])
    })
  })

  describe('tv-size-calculator', () => {
    it('reads the default distance and resolution', () => {
      expect(
        parseTvSizeCalculatorAttributes(
          'default-distance="2,5" default-resolution="full-hd"'
        )
      ).toEqual({ distance: 2.5, resolution: 'full-hd' })
    })

    it('falls back to 3 m / 4K on missing or invalid values', () => {
      const defaults = { distance: 3, resolution: '4k' }
      expect(parseTvSizeCalculatorAttributes('')).toEqual(defaults)
      expect(
        parseTvSizeCalculatorAttributes(
          'default-distance="99" default-resolution="<x>"'
        )
      ).toEqual(defaults)
    })

    it('round-trips through the marker in splitBlogBody', () => {
      const marker = renderTvSizeCalculatorMarker({
        distance: 2,
        resolution: 'full-hd',
      })
      expect(splitBlogBody(`<p>A</p>${marker}<p>B</p>`)).toEqual([
        { type: 'html', content: '<p>A</p>' },
        {
          type: 'tv-size-calculator',
          calculator: { distance: 2, resolution: 'full-hd' },
        },
        { type: 'html', content: '<p>B</p>' },
      ])
    })
  })
})
