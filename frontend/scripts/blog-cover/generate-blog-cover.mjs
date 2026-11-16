#!/usr/bin/env node
/**
 * Generates a blog post's cover from the `cover:` block of its frontmatter.
 *
 *   pnpm blog:cover <slug> [--lang fr] [--out <file.png>]
 *
 * Reads content/blog/<lang>/<slug>.md and writes public/images/blog/<slug>/cover.png (or --out).
 * The post's `image` must then be /images/blog/<slug>/cover.png; the script warns when it is not.
 * Exits with 1 when the spec is invalid or a text does not fit the template.
 * See docs/markdown-mapping.md, section "Blog cover".
 */
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { dirname, relative } from 'node:path'
import { fileURLToPath } from 'node:url'
import { parseArgs } from 'node:util'

import { parse as parseYaml } from 'yaml'

import {
  BlogCoverError,
  normalizeCoverSpec,
  renderBlogCover,
} from './blog-cover.mjs'

const FRONTEND_DIR = fileURLToPath(new URL('../../', import.meta.url))
const SLUG_PATTERN = /^[a-z0-9-]+$/

const { values, positionals } = parseArgs({
  allowPositionals: true,
  options: {
    lang: { type: 'string', default: 'fr' },
    out: { type: 'string' },
  },
})

const slug = positionals[0]
if (!slug || !SLUG_PATTERN.test(slug) || !/^[a-z]{2}$/.test(values.lang)) {
  console.error('Usage: pnpm blog:cover <slug> [--lang fr] [--out <file.png>]')
  process.exit(1)
}

const postFile = `${FRONTEND_DIR}content/blog/${values.lang}/${slug}.md`
const imagePath = `/images/blog/${slug}/cover.png`
const outFile = values.out ?? `${FRONTEND_DIR}public${imagePath}`

try {
  const source = await readFile(postFile, 'utf8')
  const frontmatter = /^---\n([\s\S]*?)\n---\n/.exec(source)
  if (!frontmatter) {
    throw new BlogCoverError(
      `${relative(FRONTEND_DIR, postFile)} has no frontmatter`
    )
  }
  const meta = parseYaml(frontmatter[1]) ?? {}
  const spec = normalizeCoverSpec(meta.cover, {
    fallbackKicker: meta.tags?.[0],
  })
  const png = await renderBlogCover(spec)
  await mkdir(dirname(outFile), { recursive: true })
  await writeFile(outFile, png)
  console.log(`Cover written: ${relative(process.cwd(), outFile)}`)
  if (!values.out && meta.image !== imagePath) {
    console.warn(
      `Warning: set "image: ${imagePath}" in the frontmatter of ${slug}.md`
    )
  }
} catch (error) {
  if (error instanceof BlogCoverError || error?.code === 'ENOENT') {
    console.error(`blog:cover ${slug}: ${error.message}`)
    process.exit(1)
  }
  throw error
}
