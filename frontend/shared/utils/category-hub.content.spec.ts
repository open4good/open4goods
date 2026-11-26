import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'
import { parse } from 'yaml'

import { validateCategoryHubDoc, type CategoryHubDoc } from './category-hub'

// Every hub file Growth writes is checked here (format: content/category-hubs/README.md).
const HUBS_DIR = resolve(__dirname, '../../content/category-hubs')
const VERTICALS_DIR = resolve(
  __dirname,
  '../../../verticals/src/main/resources/verticals'
)

const hubFiles = readdirSync(HUBS_DIR).filter(file => file.endsWith('.yml'))

describe('content/category-hubs', () => {
  it('holds the TV demonstration file, not published', () => {
    const tv = parse(readFileSync(resolve(HUBS_DIR, 'tv.yml'), 'utf8'))
    expect(tv.verticalId).toBe('tv')
    expect(tv.published).toBe(false)
  })

  it.each(hubFiles)('%s is a valid hub file', file => {
    const doc = parse(
      readFileSync(resolve(HUBS_DIR, file), 'utf8')
    ) as CategoryHubDoc

    expect(file.replace(/(\.en)?\.yml$/, '')).toBe(doc.verticalId)
    expect(existsSync(resolve(VERTICALS_DIR, `${doc.verticalId}.yml`))).toBe(
      true
    )
    expect(['fr', 'en']).toContain(doc.language ?? 'fr')
    expect(typeof (doc.published ?? false)).toBe('boolean')
    expect(validateCategoryHubDoc(doc)).toEqual([])
  })
})
