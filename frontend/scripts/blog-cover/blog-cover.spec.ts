// @vitest-environment node
import { readdirSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

import sharp from 'sharp'
import { describe, expect, it } from 'vitest'
import { parse as parseYaml } from 'yaml'

import {
  BlogCoverError,
  COVER_HEIGHT,
  COVER_WIDTH,
  normalizeCoverSpec,
  renderBlogCover,
  waffleHighlightCount,
} from './blog-cover.mjs'

const BLOG_DIR = fileURLToPath(
  new URL('../../content/blog/fr/', import.meta.url)
)
const PUBLIC_DIR = fileURLToPath(new URL('../../public', import.meta.url))

describe('normalizeCoverSpec', () => {
  it('fills the kicker from the first tag and the icon with the leaf', () => {
    expect(
      normalizeCoverSpec(
        { title: '  Un titre  ' },
        { fallbackKicker: "Pouvoir d'achat" }
      )
    ).toEqual({
      kicker: "Pouvoir d'achat",
      title: 'Un titre',
      figure: undefined,
      caption: undefined,
      source: undefined,
      icon: 'leaf',
      ratio: undefined,
      ratioLabel: undefined,
    })
  })

  it.each([
    [undefined, /no `cover:` block/],
    [{ figure: '12 %' }, /cover.title is required/],
    [{ title: 'T', icon: 'toaster' }, /cover.icon "toaster" is unknown/],
    [{ title: 'T', ratio: 1.2 }, /between 0 and 1/],
    [{ title: 'T', ratio: '0.3' }, /must be a number/],
    [{ title: 'T', figure: '12 %' }, /caption is required with cover.figure/],
  ])('rejects %j', (cover, message) => {
    expect(() => normalizeCoverSpec(cover)).toThrow(message)
  })
})

describe('waffleHighlightCount', () => {
  it.each([
    [0, 0],
    [0.04, 0],
    [0.31, 3],
    [0.96, 10],
    [1, 10],
  ])('highlights %d of 10 icons for ratio %d', (ratio, expected) => {
    expect(waffleHighlightCount(ratio)).toBe(expected)
  })
})

describe('renderBlogCover', () => {
  it('renders a 1600x900 PNG for a data cover', async () => {
    const png = await renderBlogCover(
      normalizeCoverSpec({
        kicker: 'Black Friday 2026',
        title: "Avant d'acheter : réparer, reconditionné ou comparer",
        figure: '31 %',
        caption: 'des téléviseurs ont une note de réparabilité sous 7/10',
        source: 'Données Nudger, 270 modèles, relevé du 15/11/2026',
        icon: 'tv',
        ratio: 0.31,
        ratioLabel: 'près de 1 téléviseur sur 3',
      })
    )
    const meta = await sharp(png).metadata()
    expect([meta.format, meta.width, meta.height]).toEqual([
      'png',
      COVER_WIDTH,
      COVER_HEIGHT,
    ])
  })

  it('refuses a title that overflows its zone instead of truncating it', async () => {
    const spec = normalizeCoverSpec({
      title:
        'Un titre beaucoup trop long pour la couverture chiffrée, qui ne tient pas sur deux lignes du gabarit',
      figure: '12 %',
      caption: 'une légende',
    })
    await expect(renderBlogCover(spec)).rejects.toThrow(BlogCoverError)
    await expect(renderBlogCover(spec)).rejects.toThrow(/allows 2/)
  })
})

describe('blog posts with a cover block', () => {
  const posts = readdirSync(BLOG_DIR)
    .filter(file => file.endsWith('.md'))
    .map(file => {
      const source = readFileSync(`${BLOG_DIR}${file}`, 'utf8')
      const meta =
        parseYaml(/^---\n([\s\S]*?)\n---\n/.exec(source)?.[1] ?? '') ?? {}
      return { slug: file.replace(/\.md$/, ''), meta }
    })
    .filter(post => post.meta.cover)

  it('exist', () => {
    expect(posts.length).toBeGreaterThan(0)
  })

  it.each(posts.map(post => [post.slug, post.meta]))(
    '%s has a valid spec and its generated cover as image',
    (slug, meta) => {
      expect(() => normalizeCoverSpec(meta.cover)).not.toThrow()
      expect(meta.image).toBe(`/images/blog/${slug}/cover.png`)
      expect(() => readFileSync(`${PUBLIC_DIR}${meta.image}`)).not.toThrow()
    }
  )
})
