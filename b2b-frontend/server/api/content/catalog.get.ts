import type { ContentScope } from '~/composables/useContentCatalog'
import { readdir, readFile } from 'node:fs/promises'
import { join, relative, sep } from 'node:path'

interface PagesCollectionItem {
  id: string
  path: string
  title: string
  description: string
  tags: string[]
  scope: ContentScope
  body: unknown
}

export default eventHandler(async (): Promise<PagesCollectionItem[]> => {
  const contentRoot = join(process.cwd(), 'content')
  const markdownFiles = await listMarkdownFiles(contentRoot)

  return Promise.all(markdownFiles.map(async (filePath) => {
    const rawContent = await readFile(filePath, 'utf8')
    const parsed = parseFrontMatter(rawContent)
    const relativePath = relative(contentRoot, filePath).split(sep).join('/')
    const path = `/${relativePath.replace(/\.md$/, '')}`

    return {
      id: path,
      path,
      title: parsed.title || relativePath.split('/').at(-1)?.replace(/\.md$/, '') || 'Untitled',
      description: parsed.description || '',
      tags: parsed.tags,
      scope: parsed.scope,
      body: parsed.body
    }
  }))
})

async function listMarkdownFiles(dir: string): Promise<string[]> {
  const entries = await readdir(dir, { withFileTypes: true })
  const files = await Promise.all(entries.map(async (entry) => {
    const entryPath = join(dir, entry.name)
    if (entry.isDirectory()) {
      return listMarkdownFiles(entryPath)
    }

    return entry.isFile() && entry.name.endsWith('.md') ? [entryPath] : []
  }))

  return files.flat()
}

function parseFrontMatter(content: string): {
  title: string
  description: string
  tags: string[]
  scope: ContentScope
  body: string
} {
  if (!content.startsWith('---\n')) {
    return { title: '', description: '', tags: [], scope: 'public', body: content }
  }

  const end = content.indexOf('\n---', 4)
  if (end === -1) {
    return { title: '', description: '', tags: [], scope: 'public', body: content }
  }

  const frontMatter = content.slice(4, end).split('\n')
  const body = content.slice(end + 4).trim()
  const values: Record<string, string | string[]> = {}
  let currentArrayKey = ''

  for (const line of frontMatter) {
    const arrayItem = line.match(/^\s*-\s+(.+)$/)
    if (arrayItem && currentArrayKey) {
      const existing = values[currentArrayKey]
      values[currentArrayKey] = [...(Array.isArray(existing) ? existing : []), unquote(arrayItem[1] || '')]
      continue
    }

    const keyValue = line.match(/^([A-Za-z0-9_-]+):\s*(.*)$/)
    if (!keyValue) {
      continue
    }

    const key = keyValue[1] || ''
    const value = keyValue[2] || ''
    if (!value) {
      values[key] = []
      currentArrayKey = key
      continue
    }

    values[key] = unquote(value)
    currentArrayKey = ''
  }

  return {
    title: stringValue(values.title),
    description: stringValue(values.description),
    tags: Array.isArray(values.tags) ? values.tags : [],
    scope: values.scope === 'admin' ? 'admin' : 'public',
    body
  }
}

function stringValue(value: string | string[] | undefined): string {
  return typeof value === 'string' ? value : ''
}

function unquote(value: string): string {
  return value.trim().replace(/^["']|["']$/g, '')
}
