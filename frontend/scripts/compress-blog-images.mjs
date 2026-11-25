#!/usr/bin/env node
/**
 * Recompress the blog images above 500 KB in place (same path and format, so
 * article front matter stays valid): width capped at 1600 px, PNG quantized to
 * a palette, JPEG re-encoded with mozjpeg. A file is only replaced when the
 * result is smaller. Usage: node scripts/compress-blog-images.mjs [--dry-run]
 */
import { readdir, readFile, stat, writeFile } from 'node:fs/promises'
import { extname, join } from 'node:path'
import sharp from 'sharp'

const ROOT = new URL('../public/images/blog/', import.meta.url).pathname
const THRESHOLD = 500 * 1024
const MAX_WIDTH = 1600
const dryRun = process.argv.includes('--dry-run')

const walk = async dir => {
  const entries = await readdir(dir, { withFileTypes: true })
  const nested = await Promise.all(
    entries.map(entry =>
      entry.isDirectory()
        ? walk(join(dir, entry.name))
        : [join(dir, entry.name)]
    )
  )
  return nested.flat()
}

let before = 0
let after = 0
for (const file of await walk(ROOT)) {
  const extension = extname(file).toLowerCase()
  const { size } = await stat(file)
  if (size <= THRESHOLD || !['.png', '.jpg', '.jpeg'].includes(extension)) {
    continue
  }
  const pipeline = sharp(await readFile(file)).resize({
    width: MAX_WIDTH,
    withoutEnlargement: true,
  })
  const output =
    extension === '.png'
      ? await pipeline
          .png({ palette: true, quality: 85, compressionLevel: 9, effort: 8 })
          .toBuffer()
      : await pipeline.jpeg({ quality: 82, mozjpeg: true }).toBuffer()
  before += size
  if (output.length < size) {
    after += output.length
    if (!dryRun) await writeFile(file, output)
  } else {
    after += size
  }
  console.log(
    `${file.replace(ROOT, '')}: ${size} -> ${Math.min(size, output.length)}`
  )
}
console.log(`Total: ${before} -> ${after} bytes`)
