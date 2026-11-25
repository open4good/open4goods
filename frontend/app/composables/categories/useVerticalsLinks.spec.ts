import { describe, expect, it } from 'vitest'
import { buildVerticalLinks } from './useVerticalsLinks'

describe('buildVerticalLinks', () => {
  it('keeps enabled verticals, ordered, with sub-category and guide links', () => {
    const links = buildVerticalLinks([
      { id: 'b', verticalHomeUrl: '/b', verticalHomeTitle: 'B', order: 2 },
      {
        id: 'a',
        verticalHomeUrl: '/televiseurs/',
        verticalHomeTitle: 'Téléviseurs',
        order: 1,
        subCategories: [{ id: 's', slug: 'oled', h1Title: 'OLED' }],
        guides: ['guide-tv'],
      },
      { id: 'off', verticalHomeUrl: '/off', enabled: false },
      { id: 'nourl' },
    ])

    expect(links.map(link => link.href)).toEqual(['/televiseurs/', '/b'])
    expect(links[0]?.subCategories).toEqual([
      { id: 's', title: 'OLED', href: '/televiseurs/oled' },
    ])
    expect(links[0]?.guides).toEqual([
      { slug: 'guide-tv', href: '/televiseurs/guide-tv' },
    ])
  })
})
